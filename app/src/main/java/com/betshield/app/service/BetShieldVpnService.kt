package com.betshield.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.ui.MainActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class BetShieldVpnService : VpnService(), Runnable {

    companion object {
        const val ACTION_START = "com.betshield.app.service.START"
        const val ACTION_STOP = "com.betshield.app.service.STOP"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "BetShieldVpn"

        private const val VPN_INTERFACE_IP = "10.0.0.2"
        private const val VPN_DNS_SERVER_IP = "10.0.0.1"

        private val FALLBACK_UPSTREAM_RESOLVERS = listOf(
            "8.8.8.8", // Google DNS (Globally accessible, resilient fallback)
            "1.1.1.1", // Cloudflare standard
            "8.8.4.4", // Google Secondary
            "9.9.9.9"  // Quad9
        )

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isStopping = AtomicBoolean(false)
    private var workerThread: Thread? = null
    private val dnsParser = DnsPacketParser()

    private val outputStreamLock = Any()
    private var outputStream: FileOutputStream? = null
    private var dnsExecutor: ExecutorService? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            startForegroundSafely()
            startVpn()
        }

        return START_STICKY
    }

    private fun startForegroundSafely() {
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed startForeground with type specialUse, using fallback", e)
            try {
                startForeground(NOTIFICATION_ID, buildNotification())
            } catch (t: Throwable) {
                Log.e(TAG, "Fallback startForeground failed", t)
            }
        }
    }

    private fun startVpn() {
        try {
            isStopping.set(false)

            val builder = Builder()
                .setSession("UrgeVault")
                .setMtu(1500)
                // Virtual TUN address
                .addAddress(VPN_INTERFACE_IP, 24)
                // Set the virtual DNS server to 10.0.0.1 and route ONLY DNS traffic to TUN
                .addDnsServer(VPN_DNS_SERVER_IP)
                .addRoute(VPN_DNS_SERVER_IP, 32)

            // Allow applications to bypass the VPN for all non-DNS web destinations (HTTP/HTTPS/streaming)
            builder.allowBypass()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            // Exclude our own app package so that upstream queries never loop
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not addDisallowedApplication: $packageName", e)
            }

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning = true
                dnsExecutor = Executors.newFixedThreadPool(12)
                workerThread = Thread(this, "UrgeVault-VpnWorker").apply { start() }
                Log.i(TAG, "UrgeVault VPN service successfully started.")
            } else {
                Log.e(TAG, "Failed to establish VPN interface: builder.establish() returned null.")
                isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting VPN", e)
            isRunning = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun run() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        synchronized(outputStreamLock) {
            outputStream = FileOutputStream(pfd.fileDescriptor)
        }

        val app = BetShieldApplication.instance
        val blocklist = app.blocklistDatabase
        val prefs = app.preferencesManager

        val packetBuffer = ByteArray(32767)

        while (!isStopping.get()) {
            try {
                val length = try {
                    inputStream.read(packetBuffer)
                } catch (e: Exception) {
                    if (isStopping.get()) break
                    try { Thread.sleep(50) } catch (_: InterruptedException) { break }
                    continue
                }

                if (length <= 0) continue

                val query = try {
                    dnsParser.parseDnsQuery(packetBuffer, length)
                } catch (e: Exception) {
                    null
                }

                if (query != null) {
                    val domain = query.domain
                    val isBlocked = blocklist.isDomainBlocked(domain)

                    // Clone the raw packet bytes for asynchronous processing
                    val rawPacketCopy = ByteArray(length)
                    System.arraycopy(packetBuffer, 0, rawPacketCopy, 0, length)

                    if (isBlocked) {
                        Log.w(TAG, "BLOCKED domain: $domain")
                        prefs.incrementInterceptionCount()

                        // Synthesize 0.0.0.0 sinkhole response immediately
                        try {
                            val sinkholeResponse = dnsParser.createSinkholeResponsePacket(query, rawPacketCopy)
                            if (sinkholeResponse.isNotEmpty()) {
                                synchronized(outputStreamLock) {
                                    outputStream?.write(sinkholeResponse)
                                    outputStream?.flush()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error writing sinkhole response", e)
                        }
                    } else {
                        // Forward clean DNS query to safe upstream DNS resolver in background pool
                        val configuredUpstream = prefs.upstreamDns
                        dnsExecutor?.execute {
                            forwardDnsQuery(query, rawPacketCopy, configuredUpstream)
                        }
                    }
                }
            } catch (e: Throwable) {
                if (isStopping.get()) break
                Log.e(TAG, "Error in VPN worker packet loop", e)
            }
        }
    }

    private fun getActiveNetworkDnsResolvers(): List<String> {
        val resolvers = mutableListOf<String>()
        try {
            val cm = getSystemService(ConnectivityManager::class.java)
            val activeNetwork = cm?.activeNetwork
            if (activeNetwork != null) {
                val lp = cm.getLinkProperties(activeNetwork)
                if (lp != null) {
                    for (dns in lp.dnsServers) {
                        if (dns is Inet4Address) {
                            val ip = dns.hostAddress
                            if (!ip.isNullOrBlank() && ip != VPN_INTERFACE_IP && ip != VPN_DNS_SERVER_IP) {
                                resolvers.add(ip)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching active network DNS servers", e)
        }
        for (fallback in FALLBACK_UPSTREAM_RESOLVERS) {
            if (!resolvers.contains(fallback)) {
                resolvers.add(fallback)
            }
        }
        return resolvers
    }

    private fun forwardDnsQuery(
        query: DnsPacketParser.ParsedDnsQuery,
        rawPacket: ByteArray,
        preferredUpstream: String
    ) {
        val dnsPayload = ByteArray(query.dnsPayloadLength)
        System.arraycopy(
            rawPacket,
            query.dnsPayloadOffset,
            dnsPayload,
            0,
            query.dnsPayloadLength
        )

        val domain = query.domain.lowercase()
        val isAdDomain = domain.endsWith("doubleclick.net") ||
                         domain.endsWith("googlesyndication.com") ||
                         domain.endsWith("googleadservices.com") ||
                         domain.endsWith("app-measurement.com") ||
                         domain.contains("adservice.google") ||
                         domain.contains("pagead")

        val resolversToTry = mutableListOf<String>()
        if (isAdDomain) {
            // Circumvent Wi-Fi/private DNS ad blockers by routing ad requests directly through global resolvers
            resolversToTry.add("8.8.8.8")
            resolversToTry.add("1.1.1.1")
            resolversToTry.add("8.8.4.4")
        } else {
            if (preferredUpstream.isNotBlank() && preferredUpstream != "1.1.1.3") {
                resolversToTry.add(preferredUpstream)
            }
            for (res in getActiveNetworkDnsResolvers()) {
                if (!resolversToTry.contains(res)) {
                    resolversToTry.add(res)
                }
            }
        }

        var responsePayload: ByteArray? = null
        var responseLength = 0

        for (resolverIp in resolversToTry) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                protect(socket)
                socket.soTimeout = 700

                val targetAddress = InetAddress.getByName(resolverIp)
                val sendPacket = DatagramPacket(dnsPayload, dnsPayload.size, targetAddress, 53)
                socket.send(sendPacket)

                val receiveBuffer = ByteArray(1500)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)

                if (receivePacket.length >= 12) {
                    val respId = ((receiveBuffer[0].toInt() and 0xFF) shl 8) or (receiveBuffer[1].toInt() and 0xFF)
                    val expectedId = query.transactionId.toInt() and 0xFFFF
                    if (respId == expectedId) {
                        responsePayload = receiveBuffer
                        responseLength = receivePacket.length
                        break
                    }
                }
            } catch (_: Exception) {
                // Timeout or network unreachable on this resolver, try next
                continue
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }

        if (responsePayload != null && responseLength > 0 && !isStopping.get()) {
            try {
                val clientResponse = dnsParser.createForwardedResponsePacket(
                    query,
                    responsePayload,
                    responseLength
                )
                if (clientResponse.isNotEmpty()) {
                    synchronized(outputStreamLock) {
                        outputStream?.write(clientResponse)
                        outputStream?.flush()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing forwarded DNS response", e)
            }
        }
    }

    private fun stopVpn() {
        isStopping.set(true)
        isRunning = false
        try {
            dnsExecutor?.shutdownNow()
            dnsExecutor = null
        } catch (_: Exception) {}

        try {
            workerThread?.interrupt()
            synchronized(outputStreamLock) {
                outputStream?.close()
                outputStream = null
            }
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground service", e)
        }
        stopSelf()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm != null && nm.getNotificationChannel(BetShieldApplication.VPN_NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    BetShieldApplication.VPN_NOTIFICATION_CHANNEL_ID,
                    "UrgeVault Shield Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "UrgeVault active blocking service notification"
                }
                nm.createNotificationChannel(channel)
            }
        }

        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, BetShieldApplication.VPN_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("UrgeVault Shield Active")
            .setContentText("Gambling platforms and custom blocked apps are locked.")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
