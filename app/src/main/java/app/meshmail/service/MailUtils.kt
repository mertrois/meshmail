package app.meshmail.service

import java.util.*
import javax.mail.Message
import javax.mail.internet.MimeMultipart

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
                val html = part.content as String
                val regex = Regex("<[^>]*>")
                body.append(regex.replace(html, ""))
            }
        }
    }
    return body.toString()
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