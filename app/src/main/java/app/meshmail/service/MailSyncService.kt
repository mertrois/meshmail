package app.meshmail.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import app.meshmail.MeshmailApplication
import app.meshmail.android.Parameters
import app.meshmail.MeshmailApplication.Companion.prefs
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.data.MessageFragmentEntity
import app.meshmail.data.protobuf.MessageOuterClass
import app.meshmail.data.protobuf.MessageShadowOuterClass
import app.meshmail.data.protobuf.MessageShadowOuterClass.MessageShadow
import app.meshmail.data.protobuf.ProtocolMessageOuterClass
import app.meshmail.data.protobuf.ProtocolMessageOuterClass.ProtocolMessage
import app.meshmail.data.protobuf.ProtocolMessageTypeOuterClass
import app.meshmail.data.protobuf.ProtocolMessageTypeOuterClass.ProtocolMessageType
import app.meshmail.util.md5
import app.meshmail.util.toHex
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.search.FlagTerm
import kotlin.math.roundToInt


class MailSyncService : Service() {

    private var scheduledExecutor: ScheduledExecutorService? = null

    private val meshServiceManager: MeshServiceManager by lazy { (application as MeshmailApplication).meshServiceManager }

    private val database: MeshmailDatabase by lazy {
        (application as MeshmailApplication).database
    }

    override fun onCreate() {
        super.onCreate()
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        scheduledExecutor!!.scheduleWithFixedDelay(
            { syncMail() },
            0,
            Parameters.MAIL_SYNC_PERIOD,
            TimeUnit.SECONDS
        )

        // todo: create a flowable query here... that listens for newly inserted messages with hasBeenRequested = false
        // either inbound or outbound...

//        val unrequestedMessages = database.messageDao().getUnrequestedMessagesFlowable()
//        val subResult = unrequestedMessages.subscribe { messageEntities ->
//            Log.d("MailSyncService", "got ${messageEntities.size} new messages via flowable")
//            for(message in messageEntities) {
//                Log.d("MailSyncService", "Flowable: ${message.subject}")
//                broadcastMessageShadow(message)
//            }
//        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //Toast.makeText(this, "mail sync service starting", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledExecutor!!.shutdown()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    private fun syncMail() {

        if(prefs?.getBoolean("relay_mode", false)!!) {
            // get newly arrive messages
            val emails: Array<Message>? = getEmails()
            Log.d(this.javaClass.name, "there are ${emails?.size} unseen emails in the inbox")
            // store them
            if (emails != null) storeMessages(emails)

            // todo: get a list of messages of type OUTBOUND, hasBeenSent=false and attempt to send them via SMTP
        }

        /*
        Here we will look for messages with haveBeenRequested=false and send out shadows, no matter if this is client
        or relay.
         */
        broadcastNecessaryMessageShadows()
    }

    private fun createOutboundEmail(subject: String, recipient: String, sender: String, body: String) {
        // todo: create a MessageEntity of type= OUTBOUND, make the necessary fragments; broadcast shadow
        // todo: factor out code from storeMessages that creates fragments and entity. creating an outbound email
        // should start by creating a javamail message object and passing that in for the sake of code reuse.
    }

    private fun storeMessages(messages: Array<Message>) {
        for (msg in messages) {
            val mid: String = msg.getHeader("Message-ID")[0]
            if(database.messageDao().getByServerId(mid) == null) {  // this message is not in the database
                //todo: ensure this entire sequence is atomic

                Log.d(this.javaClass.name, "message not found in db, adding it now")
                val msgEnt = MessageEntity()
                msgEnt.subject = msg.subject
                msgEnt.body = extractReadableBody(msg)
                msgEnt.recipient = msg.allRecipients[0].toString()
                msgEnt.sender = msg.from[0].toString()
                msgEnt.serverId = mid
                msgEnt.receivedDate = msg.receivedDate
                msgEnt.hasBeenRequested = false
                msgEnt.isShadow = false
                msgEnt.type = "INBOUND"
                msgEnt.hasBeenSent = false
                msgEnt.fingerprint = md5(msgEnt.serverId + msgEnt.body).toHex().substring(0,8)

                // now we build the protobuf version of the message
                // todo: clean this up with a builder to simply copying values over
                val pbMessage = MessageOuterClass.Message.newBuilder()
                pbMessage.subject = msgEnt.subject
                pbMessage.body = msgEnt.body    // TODO: maybe perform LZ compression here if needed
                pbMessage.recipient = msgEnt.recipient
                pbMessage.sender = msgEnt.sender
                pbMessage.serverId = msgEnt.serverId
                pbMessage.receivedDate = dateToMillis(msgEnt.receivedDate!!)
                pbMessage.fingerprint = msgEnt.fingerprint

                // create the protobuf for the message, the raw data will get put into fragments
                val pbMessage_bytes: ByteArray = pbMessage.build().toByteArray()
                msgEnt.protoBufSize = pbMessage_bytes.size
                // todo: calculate the largest max_message_fragment_size can be
                val nFragments = Math.ceil(pbMessage_bytes.size / (Parameters.MAX_MESSAGE_FRAGMENT_SIZE *1.0)).roundToInt()
                msgEnt.nFragments = nFragments


                // but first, we need to populate the database with the fragments so they're ready to be served
                // when requested

                // create fragments and put into database
                for(f in 0 until nFragments) {
                    try {
                        var messageFragmentEntity = MessageFragmentEntity()
                        messageFragmentEntity.fingerprint = pbMessage.fingerprint
                        messageFragmentEntity.n = msgEnt.nFragments
                        messageFragmentEntity.m = f
                        val a = f * Parameters.MAX_MESSAGE_FRAGMENT_SIZE
                        val b = if(f == nFragments-1) {
                                    msgEnt.protoBufSize!! // the last one should be the last byte of the full protobuf, not the hypothetical fragment size * n
                                } else {
                                    a + Parameters.MAX_MESSAGE_FRAGMENT_SIZE
                                }
                        val d = pbMessage_bytes.sliceArray(a until b)
                        messageFragmentEntity.data = d
                        database.messageFragmentDao().insert(messageFragmentEntity)
                    } catch(e: Exception) {
                        Log.e("MailSyncService","error creating fragment",e)
                    }
                }

                // put whole message in database. needs to go after fragments for future use of live data to trigger
                // sending a shadow.
                database.messageDao().insert(msgEnt)

                // finally broadcast the existence of this message to the network
                broadcastMessageShadow(msgEnt, "initial")


            } else {
                Log.d(this.javaClass.name, "message already exists in database")
            }
            // should only clear this flag if it's in the database. adding may have failed. check for exceptions
            // msg.setFlag(Flags.Flag.SEEN, true) // only do this if successfully entered into database
        }
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
        pbMessageShadow.subject = message.subject
        pbMessageShadow.nFragments = message.nFragments!!
        pbProtocolMessage.messageShadow = pbMessageShadow.build()
        // this is ready to send over mesh network to announce a new message has come in
        val pbProtocolMessage_bytes: ByteArray = pbProtocolMessage.build().toByteArray()
        // broadcast the message shadow
        meshServiceManager.enqueueForSending(pbProtocolMessage_bytes)
        Log.d("MailSyncService","broadcast message shadow: $reason")
    }

    private fun getEmails(): Array<Message>? {
        val imapUsername = prefs?.getString("imap_username","")
        val imapPassword = prefs?.getString("imap_password","")

        val properties = Properties().apply {
            put("mail.imap.ssl.enable", "true")
            put("mail.smtp.ssl.enable", "true")
            put("mail.imap.starttls.enable", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.imaps.host", prefs?.getString("imap_server",""))
            put("mail.smtps.host", prefs?.getString("smtp_server",""))
            put("mail.imaps.port", prefs?.getString("imap_server_port","0")?.toInt())
            put("mail.smtps.port", prefs?.getString("smtp_server_port","0")?.toInt())
        }

        val session = Session.getInstance(properties)
        val store = session.getStore("imaps")

        return try {
            store.connect(imapUsername, imapPassword)

            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_WRITE)

            inbox.getMessages() // simpler method, gets everything even those that have been seen; use for debugging.
            // inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))  // gets only unseen (new) messages
        } catch(e: Exception) {
            Log.e(this.javaClass.toString(), "error checking mail or storing in db", e)
            null
        }
    }


}
