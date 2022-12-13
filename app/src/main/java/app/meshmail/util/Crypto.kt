package app.meshmail.util

import kotlin.text.Charsets.UTF_8
import java.security.MessageDigest

fun sha256(str: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(str.toByteArray(UTF_8))
fun md5(str: String): ByteArray = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8))
fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

