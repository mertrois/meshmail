package app.meshmail.mail

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.room.Room
import app.meshmail.data.AppDatabase
import app.meshmail.data.TestEntity
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


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
        Log.d(this.javaClass.name, "there are ${messages?.size} messages in the inbox")
        if(messages != null) storeMessages(messages)
    }

    private fun storeMessages(messages: Array<Message>) {
        for (msg in messages) {
            val testEnt = TestEntity()
            testEnt.subject = msg.subject
            testEnt.from = msg.from[0].toString()
            testEnt.messageId = msg.getHeader("Message-ID")[0]
            database.myDao().insert(testEnt)
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
            inbox.open(Folder.READ_ONLY)

            inbox.getMessages()
        } catch(e: Exception) {
            Log.e(this.javaClass.toString(), "error checking mail or storing in db", e)
            null
        }
    }


}
