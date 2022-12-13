package app.meshmail.mail

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.room.Room
import app.meshmail.MeshmailApplication


import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.data.protobuf.MessageOuterClass

import app.meshmail.util.md5
import com.geeksville.mesh.DataPacket


import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.search.FlagTerm



class MailSyncService : Service() {
    private val syncInterval: Long = 60 // sync interval in seconds
    private var scheduledExecutor: ScheduledExecutorService? = null

    val database: MeshmailDatabase by lazy {
        Room.databaseBuilder(
            this,
            MeshmailDatabase::class.java,
            "meshmail_database"
        ).fallbackToDestructiveMigration().build()
    }

    override fun onCreate() {
        super.onCreate()
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        scheduledExecutor!!.scheduleWithFixedDelay(
            { syncMail() },
            0,
            syncInterval,
            TimeUnit.SECONDS
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "mail sync service starting", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledExecutor!!.shutdown()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    ////////////////////////

    private fun syncMail() {
        val messages: Array<Message>? = getMessages()
        Log.d(this.javaClass.name, "there are ${messages?.size} unseen messages in the inbox")
        if(messages != null) storeMessages(messages)
    }

    private fun storeMessages(messages: Array<Message>) {
        for (msg in messages) {
            val mid: String = msg.getHeader("Message-ID")[0]
            if(database.messageDao().getByServerId(mid) == null) {
                Log.d(this.javaClass.name, "message not found in db, adding it now")
                var msgEnt = MessageEntity()
                msgEnt.subject = msg.subject
                msgEnt.body = extractReadableBody(msg)
                msgEnt.recipient = msg.allRecipients[0].toString()
                msgEnt.sender = msg.from[0].toString()
                msgEnt.serverId = mid
                msgEnt.receivedDate = msg.receivedDate

                msgEnt.isShadow = true // as of this moment, it's a shadow b/c the fragments db isn't fully populated.


                var fingerprint: String = md5(msgEnt.serverId!!).toString()
                fingerprint = fingerprint.substring(0,8)




                Log.d(this.javaClass.name, msgEnt.body!!)

                database.messageDao().insert(msgEnt)
            } else {
                Log.d(this.javaClass.name, "message already exists in database")
            }
            // should only clear this flag if it's in the database. adding may have failed. check for exceptions
            msg.setFlag(Flags.Flag.SEEN, true) // only do this if successfully entered into database
        }



        // quick testing here
        val x = 1
        var pbMessage = MessageOuterClass.Message.newBuilder()
//        var pbMessage = MessageOuterClass
        pbMessage.setBody("the body ")
        pbMessage.setSubject("i am subjective")
        pbMessage.setRecipient("tooey@two.too")
        pbMessage.setSender("frumpy@fro.om")
        var email = pbMessage.build()

        val emailBytes: ByteArray = email.toByteArray()

        //val deserializedEmail = EmailOuterClass.Email.parseFrom(emailBytes)

        val dp = DataPacket(to= DataPacket.ID_BROADCAST,
            emailBytes,
            dataType=309)
        try {
            (application as MeshmailApplication)?.meshService?.send(dp)
        } catch(e: Exception) {
            Log.e("sendMessage", "Message failed to send", e)
        }
        

    }

    private fun getMessages(): Array<Message>? {
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

        return try {
            store.connect(username, password)

            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_WRITE)

            //inbox.getMessages()
            inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
        } catch(e: Exception) {
            Log.e(this.javaClass.toString(), "error checking mail or storing in db", e)
            null
        }
    }


}
