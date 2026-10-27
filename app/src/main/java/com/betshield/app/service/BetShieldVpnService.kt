package com.betshield.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
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
            startForeground(NOTIFICATION_ID, buildNotification())
            startVpn()
        }

        return START_STICKY
    }

    private fun startVpn() {
        try {
            isStopping.set(false)

            val builder = Builder()
                .setSession("QuitBet")
                .addAddress("10.1.10.1", 32)
                .addDnsServer("10.1.10.1")
                // Route DNS traffic through our local sinkhole tunnel
                .addRoute("10.1.10.1", 32)
                .addRoute("1.1.1.0", 24)
                .addRoute("1.0.0.0", 24)
                .addRoute("8.8.8.0", 24)
                .addRoute("8.8.4.0", 24)
                .addRoute("9.9.9.0", 24)
                .addRoute("208.67.222.0", 24)
                .addRoute("208.67.220.0", 24)
                .setMtu(1500)

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning = true
                workerThread = Thread(this, "BetShield-VpnWorker").apply { start() }
                Log.i(TAG, "BetShield VPN service successfully started.")
            } else {
                Log.e(TAG, "Failed to establish VPN interface.")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting VPN", e)
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
            upstreamSocket = DatagramSocket()
            protect(upstreamSocket) // Bypass VPN loop for upstream socket
            upstreamSocket.soTimeout = 2500

            val upstreamDnsIp = InetAddress.getByName(prefs.upstreamDns)

            while (!isStopping.get()) {
                val length = inputStream.read(packetBuffer)
                if (length <= 0) continue

                val query = dnsParser.parseDnsQuery(packetBuffer, length)
                if (query != null) {
                    val domain = query.domain
                    val isBlocked = blocklist.isDomainBlocked(domain)

                    if (isBlocked) {
                        Log.w(TAG, "BLOCKED gambling domain: $domain")
                        prefs.incrementInterceptionCount()

                        // Craft synthetic sinkhole DNS response returning 0.0.0.0
                        val sinkholeResponse = dnsParser.createSinkholeResponsePacket(query, packetBuffer)
                        outputStream.write(sinkholeResponse)
                        outputStream.flush()
                    } else {
                        // Forward clean DNS query to safe upstream DNS resolver
                        try {
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
                            upstreamSocket.send(sendPacket)

                            val receiveBuffer = ByteArray(1500)
                            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                            upstreamSocket.receive(receivePacket)

                            // Translate upstream DNS response back to TUN interface
                            val clientResponse = dnsParser.createForwardedResponsePacket(
                                query,
                                receivePacket.data,
                                receivePacket.length
                            )
                            outputStream.write(clientResponse)
                            outputStream.flush()
                        } catch (e: Exception) {
                            // DNS forward timeout or unreachability
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (!isStopping.get()) {
                Log.e(TAG, "Error in VPN worker loop", e)
            }
        } finally {
            upstreamSocket?.close()
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
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, BetShieldApplication.VPN_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle("QuitBet Protection Active")
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
