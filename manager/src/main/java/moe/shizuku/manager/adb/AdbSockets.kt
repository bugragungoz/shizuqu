package moe.shizuku.manager.adb

import android.util.Log
import java.io.IOException
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Socket helpers for talking to the device's own adbd.
 *
 * adbd's TLS (connect) server and pairing server listen on all interfaces
 * (`network_inaddr_any_server` / `socket_inaddr_any_server` in
 * packages/modules/adb, unchanged in android17-release), so 127.0.0.1 works
 * on stock Android 11-17. If a build only listens on the Wi-Fi address,
 * the device's own non-loopback addresses are tried as well.
 */
internal object AdbSockets {

    private const val TAG = "AdbSockets"
    private const val CONNECT_TIMEOUT_MS = 5000

    fun connect(host: String, port: Int): Socket {
        val candidates = LinkedHashSet<String>()
        candidates.add(host)
        if (isLoopback(host)) {
            candidates.addAll(localAddresses())
        }

        var lastError: IOException? = null
        for (candidate in candidates) {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(candidate, port), CONNECT_TIMEOUT_MS)
                if (candidate != host) {
                    Log.i(TAG, "adbd is not reachable on $host:$port, connected via $candidate")
                }
                return socket
            } catch (e: IOException) {
                lastError = e
                try {
                    socket.close()
                } catch (_: IOException) {
                }
            }
        }
        throw lastError ?: ConnectException("Unable to connect to $host:$port")
    }

    /**
     * Whether something (adbd) listens on [port], either on loopback or on [host]
     * (the address mDNS resolved the service to).
     */
    fun isPortInUse(port: Int, host: InetAddress?): Boolean {
        val addresses = LinkedHashSet<InetAddress>()
        addresses.add(InetAddress.getByName("127.0.0.1"))
        if (host != null && !host.isLinkLocalAddress) addresses.add(host)
        for (address in addresses) {
            try {
                ServerSocket().use {
                    it.bind(InetSocketAddress(address, port), 1)
                }
            } catch (e: IOException) {
                // bind failed: the port is taken on this address
                return true
            }
        }
        return false
    }

    private fun isLoopback(host: String): Boolean {
        return host == "127.0.0.1" || host == "localhost" || host == "::1"
    }

    private fun localAddresses(): List<String> {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filter { it is Inet4Address && !it.isLinkLocalAddress }
                .mapNotNull { it.hostAddress }
        } catch (e: Throwable) {
            Log.w(TAG, "localAddresses", e)
            emptyList()
        }
    }
}
