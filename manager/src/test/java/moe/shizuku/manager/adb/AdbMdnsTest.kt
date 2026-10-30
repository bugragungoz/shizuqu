package moe.shizuku.manager.adb

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.Observer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The discovery state machine of [AdbMdns]: NsdManager reports start and stop
 * asynchronously, and a stop or restart in between must neither leak a running
 * discovery nor reuse a listener NsdManager still holds; failures must not leave it
 * stuck; found services are resolved one at a time.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = moe.shizuku.manager.SmokeTestApplication::class)
class AdbMdnsTest {

    private class FakeNsd : AdbMdns.Nsd {
        var discovers = 0
        var stops = 0
        // NsdManager rejects a listener that is still in use
        var listenerInUse = false
        var throwOnStop = false
        val resolves = mutableListOf<Pair<NsdServiceInfo, NsdManager.ResolveListener>>()

        override fun discoverServices(serviceType: String, listener: NsdManager.DiscoveryListener) {
            check(!listenerInUse) { "listener already in use" }
            listenerInUse = true
            discovers++
        }

        override fun stopServiceDiscovery(listener: NsdManager.DiscoveryListener) {
            stops++
            if (throwOnStop) {
                // what NsdManager does when it no longer holds the listener
                listenerInUse = false
                throw IllegalArgumentException("listener not registered")
            }
        }

        override fun resolveService(info: NsdServiceInfo, listener: NsdManager.ResolveListener) {
            resolves += info to listener
        }
    }

    private fun service(name: String) = NsdServiceInfo().apply { serviceName = name }

    private val nsd = FakeNsd()
    private val mdns = AdbMdns(AdbMdns.TLS_CONNECT, Observer { }, nsd)
    private val listener get() = mdns.listener

    private fun systemStarted() = listener.onDiscoveryStarted(AdbMdns.TLS_CONNECT)

    private fun systemStopped() {
        nsd.listenerInUse = false
        listener.onDiscoveryStopped(AdbMdns.TLS_CONNECT)
    }

    @Test
    fun startThenStop() {
        mdns.start()
        systemStarted()
        mdns.stop()
        systemStopped()
        assertEquals(1, nsd.discovers)
        assertEquals(1, nsd.stops)
    }

    @Test
    fun stopBeforeTheDiscoveryStartedStillStopsIt() {
        mdns.start()
        mdns.stop()
        assertEquals("no stop before the system confirms the start", 0, nsd.stops)
        systemStarted()
        assertEquals("stopped as soon as it started", 1, nsd.stops)
        systemStopped()
        assertEquals(1, nsd.discovers)
    }

    @Test
    fun restartWhileStoppingWaitsForTheOldDiscovery() {
        mdns.start()
        systemStarted()
        mdns.stop()
        mdns.start()
        assertEquals("the listener is still in use, no second discover yet", 1, nsd.discovers)
        systemStopped()
        assertEquals("discovers again once the old one stopped", 2, nsd.discovers)
        systemStarted()
        mdns.stop()
        assertEquals(2, nsd.stops)
    }

    @Test
    fun repeatedStartAndStopAreIgnored() {
        mdns.start()
        mdns.start()
        systemStarted()
        mdns.stop()
        mdns.stop()
        assertEquals(1, nsd.discovers)
        assertEquals(1, nsd.stops)
    }

    @Test
    fun aFailedStartCanBeRetried() {
        mdns.start()
        nsd.listenerInUse = false
        listener.onStartDiscoveryFailed(AdbMdns.TLS_CONNECT, NsdManager.FAILURE_INTERNAL_ERROR)
        // the failed start ended the session: start() tries again, no stop() needed
        mdns.start()
        assertEquals(2, nsd.discovers)
        mdns.stop()
        assertEquals("nothing started yet, nothing to stop", 0, nsd.stops)
    }

    @Test
    fun aThrowingStopDoesNotBlockTheNextStart() {
        nsd.throwOnStop = true
        mdns.start()
        systemStarted()
        mdns.stop()
        // no onDiscoveryStopped will come
        mdns.start()
        assertEquals(2, nsd.discovers)
    }

    @Test
    fun servicesAreResolvedOneAtATime() {
        mdns.start()
        systemStarted()
        listener.onServiceFound(service("other phone"))
        listener.onServiceFound(service("this phone"))
        assertEquals("before Android 14 a second resolve would fail", 1, nsd.resolves.size)
        val (info, resolveListener) = nsd.resolves[0]
        resolveListener.onResolveFailed(info, NsdManager.FAILURE_INTERNAL_ERROR)
        assertEquals("the next one after the first finished", 2, nsd.resolves.size)
        assertEquals("this phone", nsd.resolves[1].first.serviceName)
    }

    @Test
    fun nothingIsResolvedAfterStop() {
        mdns.start()
        systemStarted()
        listener.onServiceFound(service("a"))
        listener.onServiceFound(service("b"))
        mdns.stop()
        // found before onDiscoveryStopped arrived
        listener.onServiceFound(service("c"))
        val (info, resolveListener) = nsd.resolves[0]
        resolveListener.onResolveFailed(info, NsdManager.FAILURE_INTERNAL_ERROR)
        assertEquals("only the resolve that was already running", 1, nsd.resolves.size)
    }

    @Test
    fun aFailedStopFreesTheListener() {
        mdns.start()
        systemStarted()
        mdns.stop()
        nsd.listenerInUse = false
        listener.onStopDiscoveryFailed(AdbMdns.TLS_CONNECT, NsdManager.FAILURE_INTERNAL_ERROR)
        mdns.start()
        assertEquals(2, nsd.discovers)
    }
}
