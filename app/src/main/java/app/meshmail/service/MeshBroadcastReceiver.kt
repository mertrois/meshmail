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
import app.meshmail.data.protobuf.MessageFragmentRequestOuterClass.MessageFragmentRequest
import app.meshmail.data.protobuf.ProtocolMessageOuterClass.ProtocolMessage

class MeshBroadcastReceiver(context: Context): BroadcastReceiver() {
    private val database: MeshmailDatabase by lazy { (context as MeshmailApplication).database }
    private val meshServiceManager: MeshServiceManager by lazy { (context as MeshmailApplication).meshServiceManager }
    private val meshMailApplication: MeshmailApplication = context as MeshmailApplication

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

                    when(pbProtocolMessage.pmtype) {

                        ProtocolMessageType.SHADOW_BROADCAST ->
                            handleShadowBroadcast(pbProtocolMessage)

                        ProtocolMessageType.FRAGMENT_REQUEST ->
                            handleFragmentRequest(pbProtocolMessage)

                        ProtocolMessageType.FRAGMENT_BROADCAST ->
                            handleFragmentBroadcast(pbProtocolMessage)

                        else ->
                            Log.d("onReceive","unknown Meshmail protocol message. don't know how to parse this yet")

                    }

                } catch(e: Exception) {
                    Log.e("onReceive", "error decoding protobuf. unexpected input.", e)
                }
            }
            else -> {
                Log.d("onReceive", "unknown packet type received")
            }
        }
    }



    private fun handleShadowBroadcast(pbProtocolMessage: ProtocolMessageOuterClass.ProtocolMessage) {
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
        Log.d("MeshBroadcastReceiver",
        pbMessageShadow.let { ms ->
            /* just for debugging */
            val sb = StringBuilder()
            sb.appendLine("Received new Shadow:")
            sb.appendLine("Subject: ${ms.subject}")
            sb.appendLine("Fingerprint: ${ms.fingerprint}")
            sb.appendLine("Num fragments: ${ms.nFragments}")
            sb.toString()
        })

        meshMailApplication.fragmentSyncService?.nudge("handleShadowBroadcast")
    }

    private fun handleFragmentRequest(pbProtocolMessage: ProtocolMessageOuterClass.ProtocolMessage) {
        val pbMessageFragmentRequest: MessageFragmentRequest = pbProtocolMessage.messageFragmentRequest

        /*
            first, mark as read so we know the client has a shadow and is actively requesting fragments.
            We don't need to send out repeated shadows for this message in the future e.g. client was offline
        */
        val messageEntity: MessageEntity? =
            database.messageDao().getByFingerprint(pbMessageFragmentRequest.fingerprint)
        messageEntity?.hasBeenRequested = true
        if (messageEntity != null) {
            database.messageDao().update(messageEntity)
        }

        // look up this message fragment in local db
        val messageFragmentEntity: MessageFragmentEntity =
            database.messageFragmentDao().getFragmentOfMessage(pbMessageFragmentRequest.m, pbMessageFragmentRequest.fingerprint)

        // create a protobuf and populate it
        val pbProtocolMessageOut = ProtocolMessage.newBuilder()
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
        Log.d("MeshBroadcastReceiver",
        return pbMessageFragmentRequest.let { req ->
            /* just for debugging */
            val sb = StringBuilder()
            sb.appendLine("Received new Fragment Request:")
            sb.appendLine("Fingerprint: ${req.fingerprint}")
            sb.appendLine("Frag num: ${req.m}")
            sb.toString()
        })
    }

    private fun handleFragmentBroadcast(pbProtocolMessage: ProtocolMessageOuterClass.ProtocolMessage) {
        var result: String // for debugging
        val pbMessageFragment: MessageFragment = pbProtocolMessage.messageFragment
        // insert this message fragment into the database
        if(database.messageFragmentDao().getMatchingFragments(pbMessageFragment.fingerprint, pbMessageFragment.m).isNotEmpty()) {
            Log.d("MeshBroadcastReceiver","duplicate fragment received: ${pbMessageFragment.m} of ${pbMessageFragment.fingerprint}. Ignoring")
            return
        }
        val messageFragmentEntity = MessageFragmentEntity()
        messageFragmentEntity.data = pbMessageFragment.payload.toByteArray()
        messageFragmentEntity.m = pbMessageFragment.m
        messageFragmentEntity.n = pbMessageFragment.n
        messageFragmentEntity.fingerprint = pbMessageFragment.fingerprint
        database.messageFragmentDao().insert(messageFragmentEntity)

        // now, does this give us a complete set of fragments?
        result = "Fragment ${pbMessageFragment.m}/${pbMessageFragment.n} of ${pbMessageFragment.fingerprint} received."
        // if number of fragments on hand matches expected n, try to reconstitute the message
        if(database.messageFragmentDao().getNumFragmentsAvailable(pbMessageFragment.fingerprint) == pbMessageFragment.n) {
            // is there as message object, and is it a shadow?
            val message: MessageEntity = database.messageDao().getByFingerprint(pbMessageFragment.fingerprint)!!
            if(message != null && message.isShadow!!) {
                Log.d("MeshBroadcastReceiver","reconstituting message and upgrading to non-shadow")
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
                /*
                this message has made the hop (relay-client or client-relay) so set this to true to prevent it
                from being rebroadcast to to the other side ad-infinitum. No harm would be done as it would be marked as a duplicate,
                but causes unnecessary traffic.
                 */
                message.hasBeenRequested = true
                message.receivedDate = millisToDate(pbMessage.receivedDate)
                message.isShadow = false // woohoo we are a fully-fledged message now
                message.folder = "INBOX" // all newly received messages go to inbox.
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

        /*
        the relay just sent us a fragment, and we don't yet have them all, so let's go ahead a request another fragment
        now instead of waiting for the sync service to get around to it later. Even if we did just get the last one to make
        a complete message, that's also a nudge so we can move on to the next message that has missing frags.
         */
        meshMailApplication.fragmentSyncService?.nudge("handleFragmentBroadcast")

        // debugging output
        Log.d("MeshBroadcastReceiver", result)
    }
}