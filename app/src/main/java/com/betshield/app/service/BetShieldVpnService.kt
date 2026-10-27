package com.betshield.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
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
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class BetShieldVpnService : VpnService(), Runnable {

    companion object {
        const val ACTION_START = "com.betshield.app.service.START"
        const val ACTION_STOP = "com.betshield.app.service.STOP"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "BetShieldVpn"

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isStopping = AtomicBoolean(false)
    private var workerThread: Thread? = null
    private val dnsParser = DnsPacketParser()

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
            Log.e(TAG, "Failed to startForeground with type specialUse, retrying fallback", e)
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
                // Use a valid /24 subnet for the TUN virtual interface
                .addAddress("10.0.0.2", 24)
                .addDnsServer("10.0.0.2")
                // Route local TUN DNS traffic
                .addRoute("10.0.0.2", 32)
                // Intercept standard public DNS servers explicitly (/32)
                // Note: Upstream resolver 1.1.1.3 is intentionally omitted to avoid routing loops
                .addRoute("8.8.8.8", 32)
                .addRoute("8.8.4.4", 32)
                .addRoute("1.1.1.1", 32)
                .addRoute("1.0.0.1", 32)
                .addRoute("9.9.9.9", 32)
                .addRoute("9.9.9.11", 32)
                .addRoute("208.67.222.222", 32)
                .addRoute("208.67.220.220", 32)

            // Prevent our own app's upstream sockets from routing into TUN
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Could not add self to disallowed apps", e)
            }

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning = true
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
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val packetBuffer = ByteArray(32767)

        val app = BetShieldApplication.instance
        val blocklist = app.blocklistDatabase
        val prefs = app.preferencesManager

        var upstreamSocket: DatagramSocket? = null
        try {
            upstreamSocket = DatagramSocket().apply {
                protect(this)
                soTimeout = 2500
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing upstream socket", e)
        }

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

                    if (isBlocked) {
                        Log.w(TAG, "BLOCKED domain: $domain")
                        prefs.incrementInterceptionCount()

                        // Craft synthetic sinkhole DNS response returning 0.0.0.0
                        try {
                            val sinkholeResponse = dnsParser.createSinkholeResponsePacket(query, packetBuffer)
                            if (sinkholeResponse.isNotEmpty()) {
                                outputStream.write(sinkholeResponse)
                                outputStream.flush()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error writing sinkhole response", e)
                        }
                    } else {
                        // Forward clean DNS query to safe upstream DNS resolver
                        try {
                            if (upstreamSocket == null || upstreamSocket.isClosed) {
                                upstreamSocket = DatagramSocket().apply {
                                    protect(this)
                                    soTimeout = 2500
                                }
                            }

                            val upstreamDnsIp = InetAddress.getByName(prefs.upstreamDns)
                            val dnsPayload = ByteArray(query.dnsPayloadLength)
                            System.arraycopy(
                                packetBuffer,
                                query.dnsPayloadOffset,
                                dnsPayload,
                                0,
                                query.dnsPayloadLength
                            )

                            val sendPacket = DatagramPacket(
                                dnsPayload,
                                dnsPayload.size,
                                upstreamDnsIp,
                                53
                            )
                            upstreamSocket?.send(sendPacket)

                            val receiveBuffer = ByteArray(1500)
                            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                            upstreamSocket?.receive(receivePacket)

                            // Translate upstream DNS response back to TUN interface
                            val clientResponse = dnsParser.createForwardedResponsePacket(
                                query,
                                receivePacket.data,
                                receivePacket.length
                            )
                            if (clientResponse.isNotEmpty()) {
                                outputStream.write(clientResponse)
                                outputStream.flush()
                            }
                        } catch (e: Exception) {
                            // DNS forward timeout or unreachability - normal for transient network changes
                        }
                    }
                }
            } catch (e: Throwable) {
                if (isStopping.get()) break
                Log.e(TAG, "Error in VPN worker packet loop", e)
            }
        }

        try {
            upstreamSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun stopVpn() {
        isStopping.set(true)
        isRunning = false
        try {
            workerThread?.interrupt()
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
        // Ensure channel is registered
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
