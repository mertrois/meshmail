package app.meshmail.service

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.mail.Message
import javax.mail.internet.MimeMultipart

// todo: improve html->plain text conversion
// currently includes things like style sheet contents, scripts, etc
// do some testing on compression algorithms, see overhead on small messages vs large
fun extractReadableBody(message: Message): String {
    val body = StringBuilder()

    if (message.isMimeType("text/plain")) {
        body.append(message.content as String)
    } else if (message.isMimeType("multipart/*")) {
        val multipart = message.content as MimeMultipart
        for (i in 0 until multipart.count) {
            val part = multipart.getBodyPart(i)
            if (part.isMimeType("text/plain")) {
                // This part is plain text, so we can get the body
                body.append(part.content as String)
            } else if (part.isMimeType("text/html")) {
                // This part is HTML, so we need to strip the HTML tags
                var html = part.content as String

                //var regex = Regex("<style[^>]*>.*</[^>]*>")

                // strip out all html tags
                val regex = Regex("<[^>]*>")
                html = regex.replace(html,"")

                body.append(regex.replace(html, ""))
            }
        }
    }
    return body.toString()
}

fun compressString(input: String) : ByteArray {
    val out = ByteArrayOutputStream()
    val gzipOut = GZIPOutputStream(out)
    gzipOut.write(input.toByteArray())
    gzipOut.close()
    return out.toByteArray()
}

fun decompressData(compressedData: ByteArray) : String {
    val out = ByteArrayOutputStream()
    val zipIn = GZIPInputStream(ByteArrayInputStream(compressedData))
    val buffer = ByteArray(1024)
    var len = zipIn.read(buffer)
    while(len != -1) {
        out.write(buffer, 0, len)
        len = zipIn.read(buffer)
    }
    return out.toString()
}

fun dateToMillis(date: Date?): Long {
    return if(date == null)
        0
    else
        date.getTime()
}

fun millisToDate(millis: Long): Date {
    return java.util.Date(millis)
}