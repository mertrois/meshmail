package app.meshmail.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.impl.Schedulers
import app.meshmail.MeshmailApplication
import app.meshmail.R
import app.meshmail.android.Parameters
import app.meshmail.android.PrefsManager
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.data.MessageFragmentEntity
import app.meshmail.data.StatusManager
import app.meshmail.data.protobuf.MessageOuterClass
import app.meshmail.data.protobuf.MessageShadowOuterClass.MessageShadow
import app.meshmail.data.protobuf.ProtocolMessageOuterClass.ProtocolMessage
import app.meshmail.data.protobuf.ProtocolMessageTypeOuterClass.ProtocolMessageType
import app.meshmail.util.md5
import app.meshmail.util.toHex
import com.google.protobuf.kotlin.toByteString
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.subscribe
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.mail.*
import javax.mail.event.MessageCountAdapter
import javax.mail.event.MessageCountEvent
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.search.FlagTerm
import kotlin.math.ceil
import kotlin.math.roundToInt


class MailSyncService : Service() {

    private var scheduledExecutor: ScheduledExecutorService? = null

    private val meshServiceManager: MeshServiceManager by lazy { (application as MeshmailApplication).meshServiceManager }

    private val database: MeshmailDatabase by lazy {
        (application as MeshmailApplication).database
    }

    private val prefs: PrefsManager by lazy {
        (application as MeshmailApplication).prefs
    }

    private val statusManager: StatusManager by lazy {
        (application as MeshmailApplication).statusManager
    }

    private var serviceRunning = false
    private var lastIdle: Date = Date()

    private var inbox: IMAPFolder? = null

    override fun onCreate() {
        super.onCreate()
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

        scheduledExecutor?.scheduleWithFixedDelay(
            { syncMail() },
            0,
            Parameters.MAIL_SYNC_PERIOD,
            TimeUnit.SECONDS
        )

        serviceRunning = true
        if(prefs.getBoolean("relay_mode", false)) {
            CoroutineScope(Dispatchers.IO).launch {
                while (serviceRunning) {
                    try {
                        configureIdle()
                    } catch (e: Exception) {
                        /*
                            if internet connection is lost, we'll get this exception. Add a delay before trying
                            again to prevent a runaway loop.
                         */
                        kotlinx.coroutines.delay(5000L)
                        Log.e("MailSyncService", "IMAP connection error", e)
                        statusManager.imapStatus.setStatus(false, "Connection to server lost")
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val chan = NotificationChannel(
            "app.meshmail.ESS",
            "Meshmail EmailSyncService",
            NotificationManager.IMPORTANCE_LOW
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET

        val manager: NotificationManager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)

        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, "MFSS")

        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle("Meshmail Email Sync Service")
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setChannelId("app.meshmail.ESS")
            .setSmallIcon(app.meshmail.R.drawable.gesture_24px)
            .build()

        // Start the service in the foreground
        startForeground(1, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        scheduledExecutor?.shutdown()
        serviceRunning = false  // disallow restart when server connect is closed
        inbox?.forceClose()     // if idling, cause return immediately
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun syncMail() {

        if(prefs.getBoolean("relay_mode", false)) {

            /*
                If the connection has been idling too long, sometimes the mail server will kill the connection quietly
                and no new mail will come in. This preemptively restarts it every x minutes. Gmail is known to kill at 10m
                so best to not let this go over 9
             */
            val now = Date()
            val elapsedSeconds = (now.time - lastIdle.time) / 1000
            if(elapsedSeconds > 60*7) {     // 7 minutes
                inbox?.forceClose()
                Log.d("MailSyncService", "Force-restarting connection")
            }

            // relay-side; get OUTBOUND, non-shadow, non-sent messages, pass to smtp
            val sendableMessages = database.messageDao().getReadyToSendMessages()
            for(message in sendableMessages) {
                sendMessageViaSMTP(message)
            }
        } else {    // client
            Log.d(this.javaClass.name, "client checking for messages in outbox...")
            handleOutboxMessages()
        }

        /*
            Here we will look for messages with haveBeenRequested=false and send out shadows,
            whether client or relay.
         */
        broadcastNecessaryMessageShadows()
    }

    /*
        Blocking call to set up a connection to server and wait for mail to arrive. Must be rearmed after
        new mail arrives--causes idle() to return
     */
    private fun configureIdle() {

        val newMessageHandler = object : MessageCountAdapter() {
            override fun messagesAdded(e: MessageCountEvent) {
                val emails = e.messages
                Log.d("MailSyncService", "received ${emails.size} new messages. storing")
                statusManager.imapStatus.setStatus(true, "Processing new mail")
                storeMessages(emails)
            }
        }

        val imapUsername = prefs.getString("imap_username", "")
        val imapPassword = prefs.getString("imap_password", "")

        val session = Session.getInstance(getMailProperties())
        val store: IMAPStore = session.getStore("imaps") as IMAPStore
        Log.d("MailSyncService", "connecting to server")
        store.connect(imapUsername, imapPassword)
        inbox = store.getFolder("INBOX") as IMAPFolder
        inbox?.open(Folder.READ_WRITE)
        inbox?.addMessageCountListener(newMessageHandler)
        statusManager.imapStatus.setStatus(true, "Listening for new mail...")
        Log.d("MailSyncService", "beginning idle")
        lastIdle = Date()   // log the time of the idle operation
        inbox?.idle()

        Log.d("MailSyncService", "leaving idle")
    }

    /*
        Older polling method, can be used as fallback
     */
    private fun getEmails(): Array<Message>? {
        val imapUsername = prefs.getString("imap_username","")
        val imapPassword = prefs.getString("imap_password","")

        val session = Session.getInstance(getMailProperties())
        val store = session.getStore("imaps")

        return try {
            store.connect(imapUsername, imapPassword)

            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_WRITE)
            statusManager.imapStatus.setStatus(true)
            //inbox.messages // simpler method, gets everything even those that have been seen; use for debugging.
            inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))  // gets only unseen (new) messages
        } catch(e: Exception) {
            Log.e(this.javaClass.toString(), "error checking mail or storing in db", e)
            statusManager.imapStatus.setStatus(false, "Error checking mail", e.toString())
            null
        }
    }


    private fun sendMessageViaSMTP(message: MessageEntity) {
        statusManager.smtpStatus.setStatus(null, "Connecting to server")
        Log.d("MailSyncService", "Sending via SMTP: ${message.fingerprint}")

        val senderName = prefs.getString("sender_name")
        val senderEmail = prefs.getString("sender_email")

        val smtpUsername = prefs.getString("smtp_username","")
        val smtpPassword = prefs.getString("smtp_password","")
        val properties = getMailProperties()

        val auth: Authenticator = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUsername, smtpPassword)
            }
        }

        val session = Session.getInstance(properties, auth)

        try {
            val email = MimeMessage(session)

            email.setFrom(InternetAddress(senderEmail, senderName))
            email.addRecipient(Message.RecipientType.TO, InternetAddress(message.recipient))
            email.subject = message.subject
            email.setText(message.body)

            // if an outbound message has a serverId set, it's because it is a reply, and we need to set
            // the appropriate headers before giving it to the smtp server
            if(message.serverId != null && message.serverId != "") {
                email.setHeader("In-Reply-To", message.serverId)
                email.setHeader("References", message.serverId)
            }

            Transport.send(email)
            Log.d("MailSyncService","mail sent successfully")
            statusManager.smtpStatus.setStatus(true, "Message successfully sent")
            message.hasBeenSent = true
            message.folder = "SENT"
            database.messageDao().update(message)
        } catch (e: MessagingException) {
            Log.e("MailSyncService","Message failed to send", e)
            statusManager.smtpStatus.setStatus(false, "Error sending message", e.toString())
            // todo: update the message in the db to indicate there was a problem; sort by types
            // if it was, for example server not reachable, we want to try again later...
            // if malformed address, send message back to client over mesh
            // if bad server address... pop up a toast or add to error log on status screen?
            // ...
        }

    }



    private fun handleOutboxMessages() {
        // get a list of messages that user wishes to send
        val outboxMessages: List<MessageEntity> = database.messageDao().getMessagesByFolder("OUTBOX")
        for(msg in outboxMessages) {
            val fragmentList = getFragmentsForMessage(msg)
            msg.folder = "SENT"             // this indicates we have made the fragments and ready to broadcast a shadow
            msg.type = "OUTBOUND"           // repeating for clarity
            msg.hasBeenRequested = false    // this will indicate to MailSyncService that the other end (relay) hasn't requested a fragment yet.
            msg.nFragments = fragmentList.count()
            storeMessageAndFragments(msg, fragmentList)
            // finally broadcast the existence of this message to the network
            broadcastMessageShadow(msg, "initial")
        }
    }

    /*
        Take a list of javamail messages and store them as MessageEntity, including generating their fragments
        to be sync'ed
     */
    private fun storeMessages(messages: Array<Message>) {
        for (msg in messages) {
            val mid: String = msg.getHeader("Message-ID")[0]
            if(database.messageDao().getByServerId(mid) == null) {  // this message is not in the database
                // so we'll add it.
                Log.d(this.javaClass.name, "message not found in db, adding it now")
                // convert the javamail message to MessageEntity for the local database
                val msgEnt = javamailMessageToMessageEntity(msg)
                // store the message entity (including its fragments)
                storeMessageEntity(msgEnt)
                msg.setFlag(Flags.Flag.SEEN, true)
                // finally broadcast the existence of this message to the network
                broadcastMessageShadow(msgEnt, "initial")
            } else {
                Log.d(this.javaClass.name, "message already exists in database")
            }

        }
    }

    private fun javamailMessageToMessageEntity(msg: Message): MessageEntity {
        val msgEnt = MessageEntity()
        msgEnt.subject = msg.subject
        msgEnt.body = extractReadableBody(msg)
        msgEnt.recipient = msg.allRecipients[0].toString()
        msgEnt.sender = msg.from[0].toString()
        msgEnt.serverId = msg.getHeader("Message-ID")[0]
        msgEnt.receivedDate = msg.receivedDate
        msgEnt.hasBeenRequested = false
        msgEnt.isShadow = false
        msgEnt.type = "INBOUND"
        msgEnt.hasBeenSent = false
        msgEnt.fingerprint = md5(msgEnt.serverId + msgEnt.body).toHex().substring(0,8)
        return msgEnt
    }


    private fun getFragmentsForMessage(msgEnt: MessageEntity): Iterable<MessageFragmentEntity> {
        // now we build the protobuf version of the message

        val pbMessage = MessageOuterClass.Message.newBuilder()
        pbMessage.subject = msgEnt.subject
        // todo: use another protobuf to encode data, enum for image, text, etc.
        pbMessage.body = "" //msgEnt.body
        pbMessage.data = compressString(msgEnt.body ?: "").toByteString()
        pbMessage.recipient = msgEnt.recipient
        pbMessage.sender = msgEnt.sender
        pbMessage.serverId = msgEnt.serverId
        pbMessage.receivedDate = dateToMillis(msgEnt.receivedDate)
        pbMessage.fingerprint = msgEnt.fingerprint
        pbMessage.type = msgEnt.type

        // create the protobuf for the message, the raw data will get put into fragments
        val pbMessageBytes: ByteArray = pbMessage.build().toByteArray()
        msgEnt.protoBufSize = pbMessageBytes.size
        // todo: calculate the largest max_message_fragment_size can be
        val nFragments = ceil(pbMessageBytes.size / (Parameters.MAX_MESSAGE_FRAGMENT_SIZE *1.0)).roundToInt()
        msgEnt.nFragments = nFragments


        // create fragments and put into database
        val fragmentList = ArrayList<MessageFragmentEntity>()
        for(f in 0 until nFragments) {
            val messageFragmentEntity = MessageFragmentEntity()
            messageFragmentEntity.fingerprint = pbMessage.fingerprint
            messageFragmentEntity.n = msgEnt.nFragments
            messageFragmentEntity.m = f
            val a = f * Parameters.MAX_MESSAGE_FRAGMENT_SIZE
            val b = if(f == nFragments-1) {
                msgEnt.protoBufSize!! // the last one should be the last byte of the full protobuf, not the hypothetical fragment size * n
            } else {
                a + Parameters.MAX_MESSAGE_FRAGMENT_SIZE
            }
            val d = pbMessageBytes.sliceArray(a until b)
            messageFragmentEntity.data = d
            fragmentList.add(messageFragmentEntity)
        }
        return fragmentList
    }


    /*
        Take a message entity, create a protobuf version, break up the protobuf into fragments, make fragments
        insert the fragments into the DB and the messageEntity
     */
    private fun storeMessageEntity(msgEnt: MessageEntity) {
        val fragmentList = getFragmentsForMessage(msgEnt)
        storeMessageAndFragments(msgEnt, fragmentList)
    }


    /*
        Called by a relay with an inbound message and fragments it wants to make available to serve to clients
        Called by a client with an outbound message and fragments ...
     */
    private fun storeMessageAndFragments(message: MessageEntity, fragments: Iterable<MessageFragmentEntity>) {
        class DBTransaction : Runnable {
            override fun run() {
                // insert fragments
                for(f in fragments)
                    database.messageFragmentDao().insert(f)
                // insert or update message
                if(database.messageDao().getByFingerprint(message.fingerprint) == null)
                    // insert if it's new (INBOUND, via IMAP)
                    database.messageDao().insert(message)
                else
                    // update if it's OUTBOUND (composer fragment already made message entity)
                    database.messageDao().update(message)
            }
        }
        database.runInTransaction(DBTransaction())
    }

    private fun broadcastNecessaryMessageShadows() {
        // get a list of messages that haven't been requested yet
        val unrequestedMessages: List<MessageEntity> = database.messageDao().getUnrequestedMessages()
        for(message in unrequestedMessages) {
            broadcastMessageShadow(message, "no response from client")
        }
    }

    private fun broadcastMessageShadow(message: MessageEntity, reason: String="") {
        val pbProtocolMessage = ProtocolMessage.newBuilder()
        pbProtocolMessage.pmtype = ProtocolMessageType.SHADOW_BROADCAST
        val pbMessageShadow = MessageShadow.newBuilder()
        pbMessageShadow.fingerprint = message.fingerprint
        pbMessageShadow.subject = if (message.subject.length > 50) message.subject.substring(0,50)+"..." else message.subject
        pbMessageShadow.nFragments = message.nFragments
        pbMessageShadow.sender = message.sender
        pbMessageShadow.receivedDate = dateToMillis(message.receivedDate)

        pbProtocolMessage.messageShadow = pbMessageShadow.build()
        // this is ready to send over mesh network to announce a new message has come in
        val pbProtocolMessageBytes: ByteArray = pbProtocolMessage.build().toByteArray()
        // broadcast the message shadow
        meshServiceManager.enqueueForSending(pbProtocolMessageBytes)
        Log.d("MailSyncService","broadcast message shadow: $reason")
        meshServiceManager.nudge()
    }

    private fun getMailProperties(): Properties {
        // rebuild each time in case properties change to avoid restart program.
        val properties = Properties().apply {

            // for idle
            put("mail.store.protocol", "imaps")
            put("mail.imaps.partialfetch", "false")

            // IMAP Properties
            put("mail.imap.ssl.enable", "true")
            put("mail.imap.starttls.enable", "true")
            put("mail.imaps.host", prefs.getString("imap_server",""))
            put("mail.imaps.port", prefs.getString("imap_server_port","0").toInt())

            // props for SSL Email
            put("mail.smtp.host", prefs.getString("smtp_server",""))
            put("mail.smtp.socketFactory.port", prefs.getString("smtp_server_port","0").toInt())
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.auth", "true")
            put("mail.smtp.port",  prefs.getString("smtp_server_port","0").toInt())

        }

        return properties
    }




}
