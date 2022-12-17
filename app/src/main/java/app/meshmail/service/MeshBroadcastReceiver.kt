package app.meshmail.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.meshmail.MeshmailApplication
import app.meshmail.android.Parameters
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.data.MessageFragmentEntity
import app.meshmail.data.protobuf.*
import com.geeksville.mesh.DataPacket
import com.google.protobuf.kotlin.toByteString
import app.meshmail.data.protobuf.ProtocolMessageTypeOuterClass.ProtocolMessageType
import app.meshmail.data.protobuf.MessageFragmentOuterClass.MessageFragment

class MeshBroadcastReceiver(context: Context): BroadcastReceiver() {
    private val database: MeshmailDatabase by lazy { (context as MeshmailApplication).database }
    private val meshServiceManager: MeshServiceManager by lazy { (context as MeshmailApplication).meshServiceManager }

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action){
            "com.geeksville.mesh.NODE_CHANGE" -> {
                // val ni: NodeInfo = intent.getParcelableExtra("com.geeksville.mesh.NodeInfo")!!
            }
            "com.geeksville.mesh.MESH_CONNECTED" -> {
                //val extra = intent.getStringExtra("com.geeksville.mesh.Connected")!!
                // extra will just be "CONNECTED" or "DISCONNECTED"
            }
            "com.geeksville.mesh.MESSAGE_STATUS" -> {
                //val extra: MessageStatus = intent.getParcelableExtra("com.geeksville.mesh.Status")!!
            }
            "com.geeksville.mesh.RECEIVED.${Parameters.MESHMAIL_PORT}" -> {
                val act = intent.action ?: ""
                Log.d("onReceive", "received an action: $act")
                try {
                    val data: DataPacket = intent.getParcelableExtra("com.geeksville.mesh.Payload")!!
                    val pbProtocolMessage = ProtocolMessageOuterClass.ProtocolMessage.parseFrom(data.bytes)

                    val resultStr: String = when(pbProtocolMessage.pmtype) {

                        ProtocolMessageType.SHADOW_BROADCAST ->
                            handleShadowBroadcast(pbProtocolMessage)

                        ProtocolMessageType.FRAGMENT_REQUEST ->
                            handleFragmentRequest(pbProtocolMessage)

                        ProtocolMessageType.FRAGMENT_BROADCAST ->
                            handleFragmentBroadcast(pbProtocolMessage)

                        else ->
                            "unknown protocol message. don't know how to parse this yet"

                    }
                    Log.d("MainActivity", resultStr)
                } catch(e: Exception) {
                    Log.e("onReceive", "error decoding protobuf. unexpected input.", e)
                }
            }
            else -> {
                Log.d("onReceive", "unknown packet type received")
            }
        }
    }



    private fun handleShadowBroadcast(pbProtocolMessage: ProtocolMessageOuterClass.ProtocolMessage): String {
        val pbMessageShadow: MessageShadowOuterClass.MessageShadow = pbProtocolMessage.messageShadow


        // if client, see if there is a message in the DB with the fingerprint
        // if not, add this message with shadow = true, filling in as much as we know
        // does it really even matter if it's the client? wouldn't it apply to any device?
        // since fingerprint should be reasonably unique?
        if(database.messageDao().getByFingerprint(pbMessageShadow.fingerprint) == null) {
            val newMessage = MessageEntity()
            newMessage.fingerprint = pbMessageShadow.fingerprint
            newMessage.nFragments = pbMessageShadow.nFragments
            newMessage.subject = pbMessageShadow.subject
            newMessage.isShadow = true
            database.messageDao().insert(newMessage)
        }

        // we want to have a service running on the client that gets notified when new messages
        // are created... it can see which fragments are missing, put fragment requests in a queue
        // and start sending.
        return pbMessageShadow.let { ms ->
            /* just for debugging */
            val sb = StringBuilder()
            sb.appendLine("Received new Shadow:")
            sb.appendLine("Subject: ${ms.subject}")
            sb.appendLine("Fingerprint: ${ms.fingerprint}")
            sb.appendLine("Num fragments: ${ms.nFragments}")
            sb.toString()
        }
    }

    private fun handleFragmentRequest(pbProtocolMessage: ProtocolMessageOuterClass.ProtocolMessage): String {
        val pbMessageFragmentRequest: MessageFragmentRequestOuterClass.MessageFragmentRequest = pbProtocolMessage.messageFragmentRequest
        // look up this message fragment in local db
        val messageFragmentEntity: MessageFragmentEntity =
            database.messageFragmentDao().getFragmentOfMessage(pbMessageFragmentRequest.m, pbMessageFragmentRequest.fingerprint)
        // create a protobuf and populate it
        val pbProtocolMessageOut = ProtocolMessageOuterClass.ProtocolMessage.newBuilder()
        pbProtocolMessageOut.pmtype = ProtocolMessageType.FRAGMENT_BROADCAST
        val pbMessageFragment = MessageFragment.newBuilder()
        pbMessageFragment.fingerprint = messageFragmentEntity.fingerprint
        pbMessageFragment.m           = messageFragmentEntity.m!!
        pbMessageFragment.n           = messageFragmentEntity.n!!
        pbMessageFragment.payload     = messageFragmentEntity.data?.toByteString()
        pbProtocolMessageOut.messageFragment = pbMessageFragment.build()
        val pbProtocolMessageBytes: ByteArray = pbProtocolMessageOut.build().toByteArray()
        // send it
        meshServiceManager.enqueueForSending(pbProtocolMessageBytes)

        // debugging
        return pbMessageFragmentRequest.let { req ->
            /* just for debugging */
            val sb = StringBuilder()
            sb.appendLine("Received new Fragment Request:")
            sb.appendLine("Fingerprint: ${req.fingerprint}")
            sb.appendLine("Frag num: ${req.m}")
            sb.toString()
        }
    }

    private fun handleFragmentBroadcast(pbProtocolMessage: ProtocolMessageOuterClass.ProtocolMessage): String {
        var result: String // for debugging
        val pbMessageFragment: MessageFragment = pbProtocolMessage.messageFragment
        // insert this message fragment into the database
        if(database.messageFragmentDao().getMatchingFragments(pbMessageFragment.fingerprint, pbMessageFragment.m).isNotEmpty()) {
            return "duplicate fragment received: ${pbMessageFragment.m} of ${pbMessageFragment.fingerprint}. Ignoring"
        }
        val messageFragmentEntity = MessageFragmentEntity()
        messageFragmentEntity.data = pbMessageFragment.payload.toByteArray()
        messageFragmentEntity.m = pbMessageFragment.m
        messageFragmentEntity.n = pbMessageFragment.n
        messageFragmentEntity.fingerprint = pbMessageFragment.fingerprint
        database.messageFragmentDao().insert(messageFragmentEntity)

        // now, does this give us a complete set of fragments?
        result = "Fragment ${pbMessageFragment.m}/${pbMessageFragment.n} of ${pbMessageFragment.fingerprint} received."

        if(database.messageFragmentDao().getNumFragmentsAvailable(pbMessageFragment.fingerprint) == pbMessageFragment.n) {
            // is there as message object, and is it a shadow?
            val message: MessageEntity = database.messageDao().getByFingerprint(pbMessageFragment.fingerprint)!!
            if(message != null && message.isShadow!!) {
                val fragments: List<MessageFragmentEntity> = database.messageFragmentDao().getAllFragmentsOfMessage(message.fingerprint)
                //fragments.sortedBy({ f -> f.m }) // this might not be necessary, already requested sorted from database.
                //val buffer = ArrayList<Byte>()
                var buffer = ByteArray(0)
                for(fragment in fragments) {
                    buffer += fragment.data!!
                }
                // now we can conjure a protobuf message from the concatenated byte arrays
                val pbMessage = MessageOuterClass.Message.parseFrom(buffer)
                // update our Message in the DB
                message.body = pbMessage.body
                message.serverId = pbMessage.serverId
                message.recipient = pbMessage.recipient
                message.sender = pbMessage.sender
                //message.receivedDate = pbMessage.receivedDate // todo: figure out conversion
                message.isShadow = false // woohoo we are a fully-fledged message now
                database.messageDao().update(message)
                result = message.let { msg ->
                    val sb = StringBuilder()
                    sb.appendLine("Received new Message: ${msg.fingerprint}")
                    sb.appendLine("subject: ${msg.subject}")
                    sb.appendLine("body: ${msg.body}")
                    sb.toString()
                }
            }
        }

        // var fragments: List<MessageFragmentEntity> = database.messageFragmentDao().getAllFragmentsOfMessage(pbMessageFragment.fingerprint)
        // debugging output
        return result
    }
}