package com.example.data.remote

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Pure Kotlin/JVM MariaDB & MySQL Wire Protocol (v10) Client.
 *
 * Connects directly to MariaDB/MySQL over TCP Socket using standard Android runtime APIs
 * (java.net.Socket & java.security.MessageDigest), completely avoiding desktop-only
 * java.sql.DriverAction classes that crash on Android ART.
 */
class MariaDBWireClient(
  private val host: String,
  private val port: Int,
  private val database: String,
  private val user: String,
  private val password: String,
  private val connectTimeoutMs: Int = 10_000,
  private val socketTimeoutMs: Int = 15_000
) {

  fun <T> withConnection(block: (Session) -> T): T {
    val socket = Socket()
    try {
      socket.soTimeout = socketTimeoutMs
      socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
      val session = Session(socket.getInputStream(), socket.getOutputStream())
      session.authenticate(user, password, database)
      session.executeUpdate("SET NAMES utf8mb4")
      return block(session)
    } finally {
      try {
        socket.close()
      } catch (_: Exception) {
      }
    }
  }

  class Session(
    private val input: InputStream,
    private val output: OutputStream
  ) {
    fun authenticate(user: String, password: String, database: String) {
      val (_, greeting) = readPacket()
      if (greeting.isEmpty()) {
        throw RuntimeException("Sunucudan boş el sıkışma (handshake) paketi alındı.")
      }
      if ((greeting[0].toInt() and 0xFF) == 0xFF) {
        throw RuntimeException("Sunucu bağlantı hatası: ${parseErrorPacket(greeting)}")
      }

      // Skip protocol version (1 byte) and null-terminated server version string
      var pos = 1
      while (pos < greeting.size && greeting[pos].toInt() != 0) {
        pos++
      }
      pos++ // skip null terminator

      // Skip thread id (4 bytes)
      pos += 4

      // Read auth_plugin_data_part_1 (8 bytes)
      val authPart1 = greeting.copyOfRange(pos, pos + 8)
      pos += 8

      // Skip filler (1 byte), capability_flags_1 (2 bytes), charset (1 byte),
      // status_flags (2 bytes), capability_flags_2 (2 bytes), auth_plugin_data_len (1 byte),
      // reserved (10 bytes) -> total 19 bytes
      pos += 19

      // Read auth_plugin_data_part_2 (12 bytes)
      val authPart2 = greeting.copyOfRange(pos, pos + 12)
      val scramble = ByteArray(20)
      System.arraycopy(authPart1, 0, scramble, 0, 8)
      System.arraycopy(authPart2, 0, scramble, 8, 12)

      // Client capabilities:
      // CLIENT_LONG_PASSWORD (0x0001) | CLIENT_CONNECT_WITH_DB (0x0008) |
      // CLIENT_PROTOCOL_41 (0x0200) | CLIENT_SECURE_CONNECTION (0x8000) |
      // CLIENT_PLUGIN_AUTH (0x00080000)
      val clientFlags = 0x00000001 or 0x00000008 or 0x00000200 or 0x00008000 or 0x00080000
      val maxPacketSize = 16_777_215
      val charsetUtf8mb4: Byte = 45 // utf8mb4_general_ci

      val out = ByteArrayOutputStream()
      val headerBuf = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
      headerBuf.putInt(clientFlags)
      headerBuf.putInt(maxPacketSize)
      headerBuf.put(charsetUtf8mb4)
      out.write(headerBuf.array())

      // 23 reserved zero bytes
      out.write(ByteArray(23))

      // Username (null-terminated)
      out.write(user.toByteArray(Charsets.UTF_8))
      out.write(0)

      // Native password hash (length-prefixed)
      val authData = computeNativePassword(password, scramble)
      out.write(authData.size)
      out.write(authData)

      // Database name (null-terminated)
      out.write(database.toByteArray(Charsets.UTF_8))
      out.write(0)

      // Auth plugin name (null-terminated)
      out.write("mysql_native_password".toByteArray(Charsets.UTF_8))
      out.write(0)

      sendPacket(seq = 1, payload = out.toByteArray())

      val (_, authReply) = readPacket()
      val status = authReply[0].toInt() and 0xFF
      if (status == 0xFF) {
        throw RuntimeException("MariaDB Kimlik Doğrulama Hatası: ${parseErrorPacket(authReply)}")
      }
      if (status != 0x00) {
        throw RuntimeException("Beklenmeyen kimlik doğrulama yanıt kodu: $status")
      }
    }

    fun executeUpdate(sql: String): Int {
      sendQueryCommand(sql)
      val (_, pkt) = readPacket()
      val firstByte = pkt[0].toInt() and 0xFF
      if (firstByte == 0xFF) {
        throw RuntimeException("SQL Hatası: ${parseErrorPacket(pkt)}")
      }
      if (firstByte == 0x00) {
        val reader = PacketReader(pkt, 1)
        val affectedRows = reader.readLengthEncodedInt().toInt()
        return affectedRows
      }
      // If query returned a result set, drain it
      drainResultSet(firstByte, pkt)
      return 0
    }

    fun executeQuery(sql: String): List<Map<String, String?>> {
      sendQueryCommand(sql)
      val (_, firstPkt) = readPacket()
      val firstByte = firstPkt[0].toInt() and 0xFF
      if (firstByte == 0xFF) {
        throw RuntimeException("SQL Sorgu Hatası: ${parseErrorPacket(firstPkt)}")
      }
      if (firstByte == 0x00) {
        return emptyList()
      }

      val colCount = PacketReader(firstPkt, 0).readLengthEncodedInt().toInt()
      val columnNames = mutableListOf<String>()

      // Read column definition packets
      for (i in 0 until colCount) {
        val (_, colPkt) = readPacket()
        val reader = PacketReader(colPkt, 0)
        reader.skipLengthEncodedString() // catalog ("def")
        reader.skipLengthEncodedString() // schema
        reader.skipLengthEncodedString() // table alias
        reader.skipLengthEncodedString() // org_table
        val colName = reader.readLengthEncodedString() // column name
        columnNames.add(colName)
      }

      // Read EOF packet after column definitions
      val (_, eofAfterCols) = readPacket()
      if ((eofAfterCols[0].toInt() and 0xFF) == 0xFF) {
        throw RuntimeException("SQL Sütun Okuma Hatası: ${parseErrorPacket(eofAfterCols)}")
      }

      // Read rows until EOF packet
      val rows = mutableListOf<Map<String, String?>>()
      while (true) {
        val (_, rowPkt) = readPacket()
        val marker = rowPkt[0].toInt() and 0xFF
        if (marker == 0xFE && rowPkt.size < 9) {
          break // EOF packet
        }
        if (marker == 0xFF) {
          throw RuntimeException("SQL Satır Okuma Hatası: ${parseErrorPacket(rowPkt)}")
        }

        val reader = PacketReader(rowPkt, 0)
        val rowMap = LinkedHashMap<String, String?>(colCount)
        for (i in 0 until colCount) {
          val colName = columnNames[i]
          val value = reader.readNullableLengthEncodedString()
          rowMap[colName] = value
        }
        rows.add(rowMap)
      }

      return rows
    }

    private fun drainResultSet(firstByte: Int, firstPkt: ByteArray) {
      val colCount = PacketReader(firstPkt, 0).readLengthEncodedInt().toInt()
      for (i in 0 until colCount) {
        readPacket()
      }
      readPacket() // EOF after cols
      while (true) {
        val (_, pkt) = readPacket()
        val marker = pkt[0].toInt() and 0xFF
        if ((marker == 0xFE && pkt.size < 9) || marker == 0xFF) break
      }
    }

    private fun sendQueryCommand(sql: String) {
      val sqlBytes = sql.toByteArray(Charsets.UTF_8)
      val payload = ByteArray(1 + sqlBytes.size)
      payload[0] = 0x03 // COM_QUERY
      System.arraycopy(sqlBytes, 0, payload, 1, sqlBytes.size)
      sendPacket(seq = 0, payload = payload)
    }

    private fun sendPacket(seq: Int, payload: ByteArray) {
      val len = payload.size
      val header = byteArrayOf(
        (len and 0xFF).toByte(),
        ((len shr 8) and 0xFF).toByte(),
        ((len shr 16) and 0xFF).toByte(),
        (seq and 0xFF).toByte()
      )
      output.write(header)
      output.write(payload)
      output.flush()
    }

    private fun readPacket(): Pair<Int, ByteArray> {
      val header = readFully(4)
      val len = (header[0].toInt() and 0xFF) or
        ((header[1].toInt() and 0xFF) shl 8) or
        ((header[2].toInt() and 0xFF) shl 16)
      val seq = header[3].toInt() and 0xFF
      val body = readFully(len)
      return seq to body
    }

    private fun readFully(length: Int): ByteArray {
      val buf = ByteArray(length)
      var offset = 0
      while (offset < length) {
        val read = input.read(buf, offset, length - offset)
        if (read == -1) {
          throw EOFException("MariaDB bağlantısı beklenmedik şekilde kapandı.")
        }
        offset += read
      }
      return buf
    }

    private fun parseErrorPacket(pkt: ByteArray): String {
      if (pkt.size < 3) return "Bilinmeyen veritabanı hatası"
      val errCode = (pkt[1].toInt() and 0xFF) or ((pkt[2].toInt() and 0xFF) shl 8)
      val msgOffset = if (pkt.size > 9 && pkt[3] == '#'.code.toByte()) 9 else 3
      val msg = String(pkt, msgOffset, pkt.size - msgOffset, Charsets.UTF_8)
      return "[$errCode] $msg"
    }

    private fun computeNativePassword(password: String, scramble: ByteArray): ByteArray {
      if (password.isEmpty()) return ByteArray(0)
      val sha1 = MessageDigest.getInstance("SHA-1")
      val stage1 = sha1.digest(password.toByteArray(Charsets.UTF_8))
      sha1.reset()
      val stage2 = sha1.digest(stage1)
      sha1.reset()
      sha1.update(scramble)
      sha1.update(stage2)
      val stage3 = sha1.digest()

      val result = ByteArray(stage1.size)
      for (i in stage1.indices) {
        result[i] = (stage1[i].toInt() xor stage3[i].toInt()).toByte()
      }
      return result
    }
  }

  private class PacketReader(private val data: ByteArray, var pos: Int = 0) {
    fun readLengthEncodedInt(): Long {
      val first = data[pos++].toInt() and 0xFF
      return when {
        first < 251 -> first.toLong()
        first == 0xFC -> {
          val v = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
          pos += 2
          v.toLong()
        }
        first == 0xFD -> {
          val v = (data[pos].toInt() and 0xFF) or
            ((data[pos + 1].toInt() and 0xFF) shl 8) or
            ((data[pos + 2].toInt() and 0xFF) shl 16)
          pos += 3
          v.toLong()
        }
        first == 0xFE -> {
          var v = 0L
          for (i in 0 until 8) {
            v = v or ((data[pos + i].toLong() and 0xFFL) shl (8 * i))
          }
          pos += 8
          v
        }
        else -> 0L
      }
    }

    fun skipLengthEncodedString() {
      val len = readLengthEncodedInt().toInt()
      pos += len
    }

    fun readLengthEncodedString(): String {
      val len = readLengthEncodedInt().toInt()
      val s = String(data, pos, len, Charsets.UTF_8)
      pos += len
      return s
    }

    fun readNullableLengthEncodedString(): String? {
      if (pos >= data.size) return null
      val first = data[pos].toInt() and 0xFF
      if (first == 0xFB) {
        pos++
        return null
      }
      return readLengthEncodedString()
    }
  }

  companion object {
    fun sqlEscape(value: String?): String {
      if (value == null) return "NULL"
      val sb = StringBuilder("'")
      for (ch in value) {
        when (ch) {
          '\\' -> sb.append("\\\\")
          '\'' -> sb.append("\\'")
          '"' -> sb.append("\\\"")
          '\n' -> sb.append("\\n")
          '\r' -> sb.append("\\r")
          '\u0000' -> sb.append("\\0")
          else -> sb.append(ch)
        }
      }
      sb.append("'")
      return sb.toString()
    }

    fun parseJdbcUrl(jdbcUrl: String, defaultDb: String): Triple<String, Int, String> {
      // e.g. jdbc:mariadb://31.186.11.44:3306/mar220aksesucom_mobildb
      val cleaned = jdbcUrl
        .removePrefix("jdbc:mariadb://")
        .removePrefix("jdbc:mysql://")
        .removePrefix("mariadb://")
        .trim()
      val slashIdx = cleaned.indexOf('/')
      val hostPort = if (slashIdx != -1) cleaned.substring(0, slashIdx) else cleaned
      val dbPart = if (slashIdx != -1) cleaned.substring(slashIdx + 1).substringBefore('?') else defaultDb
      val colonIdx = hostPort.indexOf(':')
      val host = if (colonIdx != -1) hostPort.substring(0, colonIdx) else hostPort
      val port = if (colonIdx != -1) hostPort.substring(colonIdx + 1).toIntOrNull() ?: 3306 else 3306
      return Triple(
        host.ifBlank { "31.186.11.44" },
        port,
        dbPart.ifBlank { defaultDb }
      )
    }
  }
}
