package com.betshield.app.service

import java.nio.ByteBuffer
import java.nio.ByteOrder

class DnsPacketParser {

    data class ParsedDnsQuery(
        val domain: String,
        val transactionId: Short,
        val queryType: Short,
        val queryClass: Short,
        val ipHeaderOffset: Int,
        val ipHeaderLength: Int,
        val udpHeaderOffset: Int,
        val dnsPayloadOffset: Int,
        val dnsPayloadLength: Int,
        val dnsQuestionLength: Int,
        val sourceIp: ByteArray,
        val destIp: ByteArray,
        val sourcePort: Int,
        val destPort: Int
    )

    /**
     * Attempts to parse an IPv4 UDP packet destined for DNS (port 53).
     * Returns null if not a valid IPv4 UDP DNS query.
     */
    fun parseDnsQuery(packet: ByteArray, length: Int): ParsedDnsQuery? {
        if (length < 28) return null // 20 IP + 8 UDP min

        val buffer = ByteBuffer.wrap(packet, 0, length)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // 1. Validate IPv4
        val versionAndIhl = buffer.get(0).toInt() and 0xFF
        val version = versionAndIhl ushr 4
        if (version != 4) return null

        val ihl = (versionAndIhl and 0x0F) * 4
        if (length < ihl + 8 + 12) return null // Need IP + UDP + DNS header (12 bytes)

        // 2. Validate Protocol == UDP (17)
        val protocol = buffer.get(9).toInt() and 0xFF
        if (protocol != 17) return null

        val sourceIp = ByteArray(4)
        val destIp = ByteArray(4)
        buffer.position(12)
        buffer.get(sourceIp)
        buffer.get(destIp)

        // 3. UDP Header
        val udpOffset = ihl
        buffer.position(udpOffset)
        val sourcePort = buffer.short.toInt() and 0xFFFF
        val destPort = buffer.short.toInt() and 0xFFFF
        val udpLength = buffer.short.toInt() and 0xFFFF

        if (destPort != 53 && sourcePort != 53) return null

        // 4. DNS Header
        val dnsOffset = udpOffset + 8
        if (udpLength < 8 + 12) return null
        val actualDnsLength = Math.min(udpLength - 8, length - dnsOffset)
        if (actualDnsLength < 12) return null

        buffer.position(dnsOffset)
        val transactionId = buffer.short
        val flags = buffer.short.toInt() and 0xFFFF

        // Only parse standard queries (QR = 0)
        val isQuery = (flags and 0x8000) == 0
        if (!isQuery) return null

        val qdCount = buffer.short.toInt() and 0xFFFF
        if (qdCount < 1) return null

        // Skip ANCOUNT, NSCOUNT, ARCOUNT
        buffer.position(dnsOffset + 12)

        // 5. Parse QNAME
        val domainBuilder = StringBuilder()
        var pos = buffer.position()
        val dnsPayloadEnd = dnsOffset + actualDnsLength
        while (pos < dnsPayloadEnd) {
            val labelLen = buffer.get(pos).toInt() and 0xFF
            pos++
            if (labelLen == 0) break // End of QNAME

            if (labelLen > 63 || pos + labelLen > dnsPayloadEnd) return null // Corrupted

            if (domainBuilder.isNotEmpty()) {
                domainBuilder.append('.')
            }
            for (i in 0 until labelLen) {
                domainBuilder.append(buffer.get(pos + i).toInt().toChar())
            }
            pos += labelLen
        }

        if (pos + 4 > dnsPayloadEnd) return null
        buffer.position(pos)
        val qType = buffer.short
        val qClass = buffer.short

        // Exact length of the Question section (QNAME + 2 bytes QTYPE + 2 bytes QCLASS)
        val questionLength = (pos + 4) - (dnsOffset + 12)

        return ParsedDnsQuery(
            domain = domainBuilder.toString(),
            transactionId = transactionId,
            queryType = qType,
            queryClass = qClass,
            ipHeaderOffset = 0,
            ipHeaderLength = ihl,
            udpHeaderOffset = udpOffset,
            dnsPayloadOffset = dnsOffset,
            dnsPayloadLength = actualDnsLength,
            dnsQuestionLength = questionLength,
            sourceIp = sourceIp,
            destIp = destIp,
            sourcePort = sourcePort,
            destPort = destPort
        )
    }

    /**
     * Builds a synthetic DNS sinkhole response returning 0.0.0.0 (or NXDOMAIN).
     */
    fun createSinkholeResponsePacket(parsed: ParsedDnsQuery, rawPacket: ByteArray): ByteArray {
        val dnsQuestionLength = parsed.dnsQuestionLength
        if (dnsQuestionLength <= 0 || parsed.dnsPayloadOffset + 12 + dnsQuestionLength > rawPacket.size) {
            return ByteArray(0)
        }
        val answerRecordLength = 16 // 2 (ptr) + 2 (type) + 2 (class) + 4 (ttl) + 2 (rdlen) + 4 (ip)

        val totalDnsLength = 12 + dnsQuestionLength + answerRecordLength
        val totalUdpLength = 8 + totalDnsLength
        val totalIpLength = 20 + totalUdpLength

        val response = ByteArray(totalIpLength)
        val buffer = ByteBuffer.wrap(response)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // IPv4 Header (20 bytes)
        buffer.put(0x45.toByte()) // Version 4, IHL 5
        buffer.put(0x00.toByte()) // DSCP/ECN
        buffer.putShort(totalIpLength.toShort()) // Total length
        buffer.putShort(0x1234.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Flags (Don't Fragment)
        buffer.put(64.toByte()) // TTL
        buffer.put(17.toByte()) // Protocol: UDP (17)
        buffer.putShort(0.toShort()) // Checksum placeholder
        buffer.put(parsed.destIp) // Source IP = original Dest IP (e.g. 10.0.0.1)
        buffer.put(parsed.sourceIp) // Dest IP = original Source IP (e.g. 10.0.0.2)

        // Calculate IP Checksum
        val ipChecksum = computeIpChecksum(response, 0, 20)
        buffer.putShort(10, ipChecksum.toShort())

        // UDP Header (8 bytes)
        buffer.position(20)
        buffer.putShort(parsed.destPort.toShort()) // Source Port (53)
        buffer.putShort(parsed.sourcePort.toShort()) // Dest Port
        buffer.putShort(totalUdpLength.toShort())
        buffer.putShort(0.toShort()) // UDP checksum optional in IPv4 (0 = no checksum)

        // DNS Header (12 bytes)
        val dnsStart = 28
        buffer.position(dnsStart)
        buffer.putShort(parsed.transactionId)
        buffer.putShort(0x8180.toShort()) // Standard query response, No error
        buffer.putShort(1.toShort()) // QDCOUNT = 1
        buffer.putShort(1.toShort()) // ANCOUNT = 1
        buffer.putShort(0.toShort()) // NSCOUNT = 0
        buffer.putShort(0.toShort()) // ARCOUNT = 0

        // Copy Original Question Section
        System.arraycopy(
            rawPacket,
            parsed.dnsPayloadOffset + 12,
            response,
            buffer.position(),
            dnsQuestionLength
        )
        buffer.position(buffer.position() + dnsQuestionLength)

        // Add A Record pointing to 0.0.0.0
        buffer.putShort(0xC00C.toShort()) // Pointer to QNAME at offset 12 in DNS header
        buffer.putShort(0x0001.toShort()) // Type A
        buffer.putShort(0x0001.toShort()) // Class IN
        buffer.putInt(60) // TTL = 60 seconds
        buffer.putShort(4.toShort()) // Data length = 4 bytes
        buffer.put(0.toByte()) // 0.0.0.0
        buffer.put(0.toByte())
        buffer.put(0.toByte())
        buffer.put(0.toByte())

        return response
    }

    /**
     * Builds a valid IPv4 UDP packet containing the real upstream DNS response
     * addressed back to the client device.
     */
    fun createForwardedResponsePacket(
        parsed: ParsedDnsQuery,
        dnsResponsePayload: ByteArray,
        payloadLength: Int
    ): ByteArray {
        if (payloadLength < 12 || payloadLength > dnsResponsePayload.size) {
            return ByteArray(0)
        }
        val totalUdpLength = 8 + payloadLength
        val totalIpLength = 20 + totalUdpLength

        val packet = ByteArray(totalIpLength)
        val buffer = ByteBuffer.wrap(packet)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // IPv4 Header (20 bytes)
        buffer.put(0x45.toByte()) // Version 4, IHL 5
        buffer.put(0x00.toByte()) // DSCP/ECN
        buffer.putShort(totalIpLength.toShort()) // Total length
        buffer.putShort(0x4321.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Flags (Don't Fragment)
        buffer.put(64.toByte()) // TTL
        buffer.put(17.toByte()) // Protocol: UDP (17)
        buffer.putShort(0.toShort()) // Checksum placeholder
        buffer.put(parsed.destIp) // Source IP = original Destination IP (DNS server e.g. 10.0.0.1)
        buffer.put(parsed.sourceIp) // Destination IP = original Source IP (Client device e.g. 10.0.0.2)

        // Calculate IP Checksum
        val ipChecksum = computeIpChecksum(packet, 0, 20)
        buffer.putShort(10, ipChecksum.toShort())

        // UDP Header (8 bytes)
        buffer.position(20)
        buffer.putShort(parsed.destPort.toShort()) // Source Port (53)
        buffer.putShort(parsed.sourcePort.toShort()) // Destination Port (Client port)
        buffer.putShort(totalUdpLength.toShort())
        buffer.putShort(0.toShort()) // UDP checksum 0 (optional in IPv4)

        // DNS Response Payload (starts at offset 28)
        System.arraycopy(dnsResponsePayload, 0, packet, 28, payloadLength)

        return packet
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < end) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return (sum.inv()) and 0xFFFF
    }
}
