package app.meshmail.mail

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.room.Room
import app.meshmail.MainActivity
import app.meshmail.MeshmailApplication
import app.meshmail.data.AppDatabase
import app.meshmail.data.EmailOuterClass
import app.meshmail.data.EmailOuterClass.EmailOrBuilder
import app.meshmail.data.TestEntity
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

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "my_database"
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
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()
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
            if(database.myDao().getByMessageId(mid) == null) {
                Log.d(this.javaClass.name, "message not found in db, adding it now")
                val testEnt = TestEntity()
                testEnt.subject = msg.subject
                testEnt.from = msg.from[0].toString()
                testEnt.messageId = mid
                testEnt.body = extractReadableBody(msg)

                Log.d(this.javaClass.name, testEnt.body!!)

                database.myDao().insert(testEnt)
            } else {
                Log.d(this.javaClass.name, "message already exists in database")
            }
            // should only clear this flag if it's in the database. adding may have failed. check for exceptions
            msg.setFlag(Flags.Flag.SEEN, true) // only do this if successfully entered into database
        }

        // quick testing here
        val x = 1
        var em = EmailOuterClass.Email.newBuilder()
        em.setBody("the body ")
        em.setSubject("i am subjective")
        em.setTo("tooey@two.too")
        em.setFrom("frumpy@fro.om")
        var email = em.build()

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
