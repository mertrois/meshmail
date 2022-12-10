package app.meshmail

import android.os.AsyncTask
import android.util.Log
import java.util.*
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class CheckMessagesTask : AsyncTask<Void, Void, Int>() {

    override fun doInBackground(vararg params: Void?): Int {
        // Replace with your own values

        val username = "test@meshmail.app"
        val password = "4xxr7hdT"

        val properties = Properties().apply {
            put("mail.imap.ssl.enable", "true")
            put("mail.smtp.ssl.enable", "true")
            put("mail.imap.starttls.enable", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.imaps.host", "imap.dreamhost.com")
            put("mail.smtps.host", "smtp.dreamhost.com")
            put("mail.imaps.port", 993)
            put("mail.smtps.port", 465)
        }

        val session = Session.getInstance(properties)
        val store = session.getStore("imaps")
        session.debug = true

        try {
            store.connect(username, password)

            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)

            val messages = inbox.getMessages()


            for(message in messages) {
                val replyMessage = MimeMessage(session)
                replyMessage.setRecipient(Message.RecipientType.TO, message.from[0] as InternetAddress)
                replyMessage.setSubject("RE: ${message.subject}")
                replyMessage.setText("YOU ARE INSANE!")
                Transport.send(replyMessage, username, password)
             //call version of Transport.send that also takes a username and password...
            }

            // database test... could do this to add to local db if it doesn't exist.
//            for(msg in messages) {
//                sendMessage(msg.subject)
//                val testEnt = TestEntity()
//                testEnt.subject = msg.subject
//                testEnt.from = msg.from[0].toString()
//                testEnt.messageId = msg.getHeader("Message-ID")[0]
//                this@MainActivity.database.myDao().insert(testEnt)
////            }
//            for(te in this@MainActivity.database.myDao().getAll()) {
//                Log.d("database", te.toString())
//            }
            return messages.size
        } catch(e: Exception) {
            Log.e("async task", "couldn't do email", e)
            return 0
        }
    }

    override fun onPostExecute(result: Int) {
        Log.d("IMAP", "You have $result new messages.")
    }
}