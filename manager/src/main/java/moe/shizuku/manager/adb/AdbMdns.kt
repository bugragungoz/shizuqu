package moe.shizuku.manager.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import java.net.NetworkInterface

@RequiresApi(Build.VERSION_CODES.R)
class AdbMdns(
    context: Context, private val serviceType: String,
    private val observer: Observer<Int>
) {

    // NsdManager reports start/stop asynchronously, so track three states:
    // running   - the caller wants results (start() .. stop())
    // requested - discoverServices() was called and the discovery has not stopped yet
    // registered - onDiscoveryStarted() arrived, stopServiceDiscovery() is allowed
    // Upstream only had "registered": a stop() before onDiscoveryStarted() left the
    // discovery running forever, and start() right after stop() could reuse a listener
    // that was still in use (IllegalArgumentException).
    private val lock = Any()
    @Volatile
    private var running = false
    private var requested = false
    private var registered = false
    private var serviceName: String? = null
    private val listener = DiscoveryListener(this)
    private val nsdManager: NsdManager = context.getSystemService(NsdManager::class.java)

    fun start() {
        synchronized(lock) {
            if (running) return
            running = true
            if (!requested) discoverLocked()
        }
    }

    fun stop() {
        synchronized(lock) {
            if (!running) return
            running = false
            // if the discovery has not started yet, onDiscoveryStart() stops it
            if (registered) stopDiscoveryLocked()
        }
    }

    private fun discoverLocked() {
        requested = true
        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Throwable) {
            requested = false
            Log.w(TAG, "discoverServices failed", e)
        }
    }

    private fun stopDiscoveryLocked() {
        try {
            nsdManager.stopServiceDiscovery(listener)
        } catch (e: Throwable) {
            Log.w(TAG, "stopServiceDiscovery failed", e)
        }
    }

    private fun onDiscoveryStart() {
        synchronized(lock) {
            registered = true
            if (!running) stopDiscoveryLocked()
        }
    }

    private fun onDiscoveryStop() {
        synchronized(lock) {
            registered = false
            requested = false
            // start() was called again while the old discovery was stopping
            if (running) discoverLocked()
        }
    }

    private fun onDiscoveryFailed() {
        synchronized(lock) {
            registered = false
            requested = false
        }
    }

    private fun onServiceFound(info: NsdServiceInfo) {
        nsdManager.resolveService(info, ResolveListener(this))
    }

    private fun onServiceLost(info: NsdServiceInfo) {
        if (info.serviceName == serviceName) observer.onChanged(-1)
    }

    private fun onServiceResolved(resolvedService: NsdServiceInfo) {
        @Suppress("DEPRECATION")
        val host = resolvedService.host ?: return
        // only accept the adbd of this device, not other devices on the network
        if (running && NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .any { networkInterface ->
                    networkInterface.inetAddresses
                        .asSequence()
                        .any { host.hostAddress == it.hostAddress }
                }
            && AdbSockets.isPortInUse(resolvedService.port, host)
        ) {
            serviceName = resolvedService.serviceName
            observer.onChanged(resolvedService.port)
        }
    }

    internal class DiscoveryListener(private val adbMdns: AdbMdns) : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            Log.v(TAG, "onDiscoveryStarted: $serviceType")

            adbMdns.onDiscoveryStart()
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.v(TAG, "onStartDiscoveryFailed: $serviceType, $errorCode")

            adbMdns.onDiscoveryFailed()
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.v(TAG, "onDiscoveryStopped: $serviceType")

            adbMdns.onDiscoveryStop()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.v(TAG, "onStopDiscoveryFailed: $serviceType, $errorCode")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "onServiceFound: ${serviceInfo.serviceName}")

            adbMdns.onServiceFound(serviceInfo)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "onServiceLost: ${serviceInfo.serviceName}")

            adbMdns.onServiceLost(serviceInfo)
        }
    }

    internal class ResolveListener(private val adbMdns: AdbMdns) : NsdManager.ResolveListener {
        override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, i: Int) {}

        override fun onServiceResolved(nsdServiceInfo: NsdServiceInfo) {
            adbMdns.onServiceResolved(nsdServiceInfo)
        }

    }

    companion object {
        const val TLS_CONNECT = "_adb-tls-connect._tcp"
        const val TLS_PAIRING = "_adb-tls-pairing._tcp"
        const val TAG = "AdbMdns"
    }
}
