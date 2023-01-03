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
                try {
                    val data: DataPacket = intent.getParcelableExtra("com.geeksville.mesh.Payload")!!
                    val pbProtocolMessage = ProtocolMessageOuterClass.ProtocolMessage.parseFrom(data.bytes)

                    try {
                        when (pbProtocolMessage.pmtype) {

                            ProtocolMessageType.SHADOW_BROADCAST ->
                                handleShadowBroadcast(pbProtocolMessage)

                            ProtocolMessageType.FRAGMENT_REQUEST ->
                                handleFragmentRequest(pbProtocolMessage)

                            ProtocolMessageType.FRAGMENT_BROADCAST ->
                                handleFragmentBroadcast(pbProtocolMessage)

                            else ->
                                Log.d(
                                    "onReceive",
                                    "unknown Meshmail protocol message. don't know how to parse this yet"
                                )
                        }
                    } catch(e: Exception) {
                        Log.d("MeshBroadcastReceiver", "Error decoding packet", e)
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
            // fixme: need to set folder based on type
            newMessage.folder = "INBOX" // this ensures fragments will show up in inbox (with progress bar)
            // when inflated to non-shadow messages, type will come through and folder will be set appropriately
            // remove to hide shadows from the inbox.
            newMessage.receivedDate = millisToDate(pbMessageShadow.receivedDate)
            newMessage.fingerprint = pbMessageShadow.fingerprint
            newMessage.nFragments = pbMessageShadow.nFragments
            newMessage.subject   = pbMessageShadow.subject
            newMessage.sender   = pbMessageShadow.sender
            newMessage.isShadow = true  // we don't yet have the body
            newMessage.hasBeenRequested = true  // this indicates to the client not to send out shadow broadcasts back to the originator
                                                // or for the case of the relay getting an OUTBOUND message, hasBeenRequested indicates the client already knows about it, don't send shadow broadcast
            database.messageDao().insert(newMessage)
        } else {
            Log.d("MeshBroadcastReceiver","Duplicate shadow broadcast received ${pbMessageShadow.fingerprint}")
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
        meshServiceManager.nudge()
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
        meshServiceManager.nudge()

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

        val pbMessageFragment: MessageFragment = pbProtocolMessage.messageFragment

        Log.d("MeshBroadcastReceiver","Fragment ${pbMessageFragment.m}/${pbMessageFragment.n} of ${pbMessageFragment.fingerprint} received.")

        // if this fragment is already in the database, ignore it
        if(database.messageFragmentDao().getMatchingFragments(pbMessageFragment.fingerprint, pbMessageFragment.m).isNotEmpty()) {
            Log.d("MeshBroadcastReceiver","duplicate fragment received: ${pbMessageFragment.m}/${pbMessageFragment.n} of ${pbMessageFragment.fingerprint}. Ignoring")
        } else {    // add it to fragment database
            val messageFragmentEntity = MessageFragmentEntity()
            messageFragmentEntity.data = pbMessageFragment.payload.toByteArray()
            messageFragmentEntity.m = pbMessageFragment.m
            messageFragmentEntity.n = pbMessageFragment.n
            messageFragmentEntity.fingerprint = pbMessageFragment.fingerprint
            database.messageFragmentDao().insert(messageFragmentEntity)
        }

        val message: MessageEntity? = database.messageDao().getByFingerprint(pbMessageFragment.fingerprint)
        if(message != null) {
            // update the received count in the message -- redundant since we can just count fragments,
            // but changes get automagically pushed to the UI progress bar this way
            message.fragsReceived = database.messageFragmentDao().getNumFragmentsAvailable(pbMessageFragment.fingerprint)
            database.messageDao().update(message)
            database.attemptToReconstituteMessage(message)
        } else {
            Log.d("MeshBroadcastReceiver","Orphan fragment received, no shadow with fingerprint ${pbMessageFragment.fingerprint}")
        }

        /*
        network just sent us a fragment, and we probably don't yet have them all, so let's go ahead a request another fragment
        now instead of waiting for the sync service to get around to it later. Even if we did just get the last one to make
        a complete message, that's also a nudge so we can move on to the next message that has missing frags.
         */
        meshMailApplication.fragmentSyncService?.nudge("handleFragmentBroadcast")
        meshServiceManager.nudge()

    }
}