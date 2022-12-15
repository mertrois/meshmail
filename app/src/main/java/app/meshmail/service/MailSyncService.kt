package app.meshmail.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.room.Room
import app.meshmail.MeshmailApplication
import app.meshmail.android.Parameters


import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.data.MessageFragmentEntity
import app.meshmail.data.protobuf.MessageOuterClass
import app.meshmail.data.protobuf.MessageShadowOuterClass
import app.meshmail.data.protobuf.ProtocolMessageOuterClass
import app.meshmail.data.protobuf.ProtocolMessageTypeOuterClass

import app.meshmail.util.md5
import app.meshmail.util.toHex
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
                //todo: ensure this entire sequence is atomic
                Log.d(this.javaClass.name, "message not found in db, adding it now")
                var msgEnt = MessageEntity()
                msgEnt.subject = msg.subject
                msgEnt.body = extractReadableBody(msg)
                msgEnt.recipient = msg.allRecipients[0].toString()
                msgEnt.sender = msg.from[0].toString()
                msgEnt.serverId = mid
                msgEnt.receivedDate = msg.receivedDate

                msgEnt.isShadow = false
                msgEnt.fingerprint = md5(msgEnt.serverId!!).toHex().substring(0,8)

                // now we build the protobuf version of the message
                // todo: clean this up with a builder to simply copying values over
                var pbMessage = MessageOuterClass.Message.newBuilder()
                pbMessage.subject = msgEnt.subject
                pbMessage.body = msgEnt.body    // TODO: maybe perform zip compression here if needed
                pbMessage.recipient = msgEnt.recipient
                pbMessage.sender = msgEnt.sender
                pbMessage.serverId = msgEnt.serverId
                //pbMessage.receivedDate = msgEnt.receivedDate // TODO: figure out conversion of date type to include here
                pbMessage.fingerprint = msgEnt.fingerprint

                // create the protobuf for the message, the raw data will get put into fragments
                var pbMessage_bytes: ByteArray = pbMessage.build().toByteArray()
                msgEnt.protoBufSize = pbMessage_bytes.size
                var nFragments = Math.ceil(pbMessage_bytes.size / (Parameters.MAX_MESSAGE_FRAGMENT_SIZE *1.0)).roundToInt()
                msgEnt.nFragments = nFragments


                var pbProtocolMessage = ProtocolMessageOuterClass.ProtocolMessage.newBuilder()
                pbProtocolMessage.pmtype = ProtocolMessageTypeOuterClass.ProtocolMessageType.SHADOW_BROADCAST
                var pbMessageShadow = MessageShadowOuterClass.MessageShadow.newBuilder()
                pbMessageShadow.fingerprint = pbMessage.fingerprint
                pbMessageShadow.subject = pbMessage.subject
                pbMessageShadow.nFragments = nFragments
                pbProtocolMessage.messageShadow = pbMessageShadow.build()
                // this is ready to send over mesh network to announce a new message has come in
                var pbProtocolMessage_bytes: ByteArray = pbProtocolMessage.build().toByteArray()

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

                // put whole message in database
                database.messageDao().insert(msgEnt)
                meshServiceManager.enqueueForSending(pbProtocolMessage_bytes)
//                // aaaaand now announce the message shadow to the mesh
//                val dp = DataPacket(to=DataPacket.ID_BROADCAST,
//                    pbProtocolMessage_bytes,
//                    dataType=Parameters.MESHMAIL_PORT)
//                try {
//                    (application as MeshmailApplication).meshService?.send(dp)
//                } catch(e: Exception) {
//                    Log.e("sendMessage", "Message failed to send", e)
//                }



            } else {
                Log.d(this.javaClass.name, "message already exists in database")
            }
            // should only clear this flag if it's in the database. adding may have failed. check for exceptions
            // msg.setFlag(Flags.Flag.SEEN, true) // only do this if successfully entered into database
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
