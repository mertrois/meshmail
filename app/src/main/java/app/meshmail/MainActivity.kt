package app.meshmail

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.geeksville.mesh.IMeshService
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat.registerReceiver
import androidx.room.Room

import app.meshmail.MeshmailApplication.Companion.prefs
import app.meshmail.android.Parameters
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.data.MessageFragmentEntity
import app.meshmail.data.protobuf.MessageFragmentOuterClass
import app.meshmail.data.protobuf.MessageFragmentRequestOuterClass
import app.meshmail.data.protobuf.MessageOuterClass
import app.meshmail.data.protobuf.MessageShadowOuterClass
import app.meshmail.data.protobuf.ProtocolMessageOuterClass
import app.meshmail.data.protobuf.ProtocolMessageTypeOuterClass


import com.geeksville.mesh.DataPacket
import com.geeksville.mesh.MessageStatus
import com.geeksville.mesh.NodeInfo


import app.meshmail.service.MailSyncService
import app.meshmail.service.MeshServiceManager
import app.meshmail.service.MessageFragmentSyncService
import com.google.protobuf.kotlin.toByteString



class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var statusText: TextView
    private lateinit var inputText: TextView

    private val meshServiceManager: MeshServiceManager by lazy { (application as MeshmailApplication).meshServiceManager }
    private val database: MeshmailDatabase by lazy { (application as MeshmailApplication).database }

    private val serviceIntent = Intent().apply {
        setClassName(
            "com.geeksville.mesh",
            "com.geeksville.mesh.service.MeshService"
        )
    }

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            meshServiceManager.serviceConnected(IMeshService.Stub.asInterface(service))
            Log.d(MainActivity::class.java.simpleName, "service connected")
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(MainActivity::class.java.simpleName, "service disconnected")
            meshServiceManager.serviceDisconnected()
        }
    }

    // Create a BroadcastReceiver to handle incoming broadcasts
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action){
                "com.geeksville.mesh.NODE_CHANGE" -> {
                    val ni: NodeInfo = intent.getParcelableExtra("com.geeksville.mesh.NodeInfo")!!
                }
                "com.geeksville.mesh.MESH_CONNECTED" -> {
                    val extra = intent.getStringExtra("com.geeksville.mesh.Connected")!!
                    // extra will just be "CONNECTED" or "DISCONNECTED"
                }
                "com.geeksville.mesh.MESSAGE_STATUS" -> {
                    val extra: MessageStatus = intent.getParcelableExtra("com.geeksville.mesh.Status")!!
                }
                "com.geeksville.mesh.RECEIVED.${Parameters.MESHMAIL_PORT}" -> {
                    val act = intent.action ?: ""
                    Log.d("onReceive", "received an action: $act")
                    try {
                        var data: DataPacket =
                            intent?.getParcelableExtra("com.geeksville.mesh.Payload")!!
                        var pbProtocolMessage = ProtocolMessageOuterClass.ProtocolMessage.parseFrom(data.bytes)
                        var resultStr: String = when(pbProtocolMessage.pmtype) {

                            ProtocolMessageTypeOuterClass.ProtocolMessageType.SHADOW_BROADCAST -> {
                                val pbMessageShadow: MessageShadowOuterClass.MessageShadow = pbProtocolMessage.messageShadow


                                // if client, see if there is a message in the DB with the fingerprint
                                // if not, add this message with shadow = true, filling in as much as we know
                                // does it really even matter if it's the client? wouldn't it apply to any device?
                                // since fingerprint should be reasonably unique?
                                if(database.messageDao().getByFingerprint(pbMessageShadow.fingerprint) == null) {
                                    var newMessage = MessageEntity()
                                    newMessage.fingerprint = pbMessageShadow.fingerprint
                                    newMessage.nFragments = pbMessageShadow.nFragments
                                    newMessage.subject = pbMessageShadow.subject
                                    newMessage.isShadow = true
                                    database.messageDao().insert(newMessage)
                                }


                                // we want to have a service running on the client that gets notified when new messages
                                // are created... it can see which fragments are missing, put fragment requests in a queue
                                // and start sending.
                                pbMessageShadow.let { ms ->
                                    /* just for debugging */
                                    var sb = StringBuilder()
                                    sb.appendLine("Received new Shadow:")
                                    sb.appendLine("Subject: ${ms.subject}")
                                    sb.appendLine("Fingerprint: ${ms.fingerprint}")
                                    sb.appendLine("Num fragments: ${ms.nFragments}")
                                    sb.toString()
                                }





                            /*
                            Handle a fragment request:
                            send a fragment in response
                             */
                            } ProtocolMessageTypeOuterClass.ProtocolMessageType.FRAGMENT_REQUEST -> {
                                val pbMessageFragmentRequest: MessageFragmentRequestOuterClass.MessageFragmentRequest = pbProtocolMessage.messageFragmentRequest
                                // look up this message fragment in local db
                                val messageFragmentEntity: MessageFragmentEntity =
                                    database.messageFragmentDao().getFragmentOfMessage(pbMessageFragmentRequest.m, pbMessageFragmentRequest.fingerprint)
                                // create a protobuf and populate it
                                var pbProtocolMessage = ProtocolMessageOuterClass.ProtocolMessage.newBuilder()
                                pbProtocolMessage.pmtype = ProtocolMessageTypeOuterClass.ProtocolMessageType.FRAGMENT_BROADCAST
                                val pbMessageFragment = MessageFragmentOuterClass.MessageFragment.newBuilder()
                                pbMessageFragment.fingerprint = messageFragmentEntity.fingerprint
                                pbMessageFragment.m           = messageFragmentEntity.m!!
                                pbMessageFragment.n           = messageFragmentEntity.n!!
                                pbMessageFragment.payload     = messageFragmentEntity.data?.toByteString()
                                pbProtocolMessage.messageFragment = pbMessageFragment.build()
                                var pbProtocolMessage_bytes: ByteArray = pbProtocolMessage.build().toByteArray()
                                // send it
                                meshServiceManager.enqueueForSending(pbProtocolMessage_bytes)
//                                val dp = DataPacket(to=DataPacket.ID_BROADCAST,
//                                    pbProtocolMessage_bytes,
//                                    dataType= Parameters.MESHMAIL_PORT)
//                                try {
//                                    (application as MeshmailApplication).meshService?.send(dp)
//                                } catch(e: Exception) {
//                                    Log.e("sendMessage", "Message failed to send", e)
//                                }
                                // debugging
                                pbMessageFragmentRequest.let { req ->
                                    /* just for debugging */
                                    var sb = StringBuilder()
                                    sb.appendLine("Received new Fragment Request:")
                                    sb.appendLine("Fingerprint: ${req.fingerprint}")
                                    sb.appendLine("Frag num: ${req.m}")
                                    sb.toString()
                                }

                            /*
                                Handle a received fragment:
                                add to database, see if we now have all the pieces to make a message and upgrade the message
                             */
                            } ProtocolMessageTypeOuterClass.ProtocolMessageType.FRAGMENT_BROADCAST -> {
                                var result: String = ""
                                val pbMessageFragment: MessageFragmentOuterClass.MessageFragment = pbProtocolMessage.messageFragment
                                // insert this message fragment into the database
                                var messageFragmentEntity: MessageFragmentEntity = MessageFragmentEntity()
                                messageFragmentEntity.data = pbMessageFragment.payload.toByteArray()
                                messageFragmentEntity.m = pbMessageFragment.m
                                messageFragmentEntity.n = pbMessageFragment.n
                                messageFragmentEntity.fingerprint = pbMessageFragment.fingerprint
                                database.messageFragmentDao().insert(messageFragmentEntity)
                                // now, does this give us a complete set of fragments?
                                result = "Fragment ${pbMessageFragment.m}/${pbMessageFragment.n} of ${pbMessageFragment.fingerprint} received."
                                if(database.messageFragmentDao().getNumFragmentsAvailable(pbMessageFragment.fingerprint) == pbMessageFragment.n) {
                                    // is there as message object, and is it a shadow?
                                    var message: MessageEntity = database.messageDao().getByFingerprint(pbMessageFragment.fingerprint)!!
                                    if(message != null && message.isShadow!!) {
                                        val fragments: List<MessageFragmentEntity> = database.messageFragmentDao().getAllFragmentsOfMessage(message.fingerprint)
                                        //fragments.sortedBy({ f -> f.m }) // this might not be necessary, already requested sorted from database.
                                        //val buffer = ArrayList<Byte>()
                                        var buffer: ByteArray = ByteArray(0)
                                        for(fragment in fragments) {
                                            buffer = buffer + fragment.data!!
                                        }
                                        // now we can conjure a protobuf message from the concatenated byte arrays
                                        var pbMessage = MessageOuterClass.Message.parseFrom(buffer)
                                        // update our Message in the DB
                                        message.body = pbMessage.body
                                        message.serverId = pbMessage.serverId
                                        message.recipient = pbMessage.recipient
                                        message.sender = pbMessage.sender
                                        //message.receivedDate = pbMessage.receivedDate // todo: figure out conversion
                                        message.isShadow = false // woohoo we are a fully-fledged message now
                                        database.messageDao().update(message)
                                        result = message.let { msg ->
                                            var sb = StringBuilder()
                                            sb.appendLine("Received new Message: ${msg?.fingerprint}")
                                            sb.appendLine("subject: ${msg?.subject}")
                                            sb.appendLine("body: ${msg?.body}")
                                            sb.toString()
                                        }
                                    }
                                }

                                // var fragments: List<MessageFragmentEntity> = database.messageFragmentDao().getAllFragmentsOfMessage(pbMessageFragment.fingerprint)
                                // debugging output
                                result
                            } else -> {
                                "unknown protocol message. don't know how to parse this yet"
                            }
                        }
                        statusText.append(resultStr)
                        statusText.append("\n")
                        Log.d("MainActivity", resultStr)
                    } catch(e: Exception) {
                        Log.e("onReceive", "error decoding protobuf. unexpected input.")
                    }
                }
                else -> {
                    Log.d("onReceive", "unknown packet type received")
                }
            }
        }
    }

    var intentFilter = IntentFilter().apply {
        addAction("com.geeksville.mesh.NODE_CHANGE")
        addAction("com.geeksville.mesh.MESH_CONNECTED")
        addAction("com.geeksville.mesh.RECEIVED.${Parameters.MESHMAIL_PORT}")
        addAction("com.geeksville.mesh.MESSAGE_STATUS")

    }

    // this was an early test function; can be removed
//    fun sendMessage(s: String) {
//        val dp = DataPacket(to=DataPacket.ID_BROADCAST,
//            s.toByteArray(),
//            dataType= Parameters.MESHMAIL_PORT)
//        try {
//            meshServiceManager.send(dp)
//        } catch(e: Exception) {
//            Log.e("sendMessage", "Message failed to send", e)
//        }
//    }



    override fun onCreate(savedInstanceState: Bundle?) {

        val appMode = prefs?.getString("APP_MODE","MODE_CLIENT")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        inputText = findViewById(R.id.chatTextToSend)

        button = findViewById(R.id.button)
        button.setOnClickListener { v ->
            Log.d(MainActivity::class.java.simpleName, "button clicked")
            //sendMessage(inputText.text.toString())
            //inputText.text = ""
        }

        // todo: remove; only for dev. Clean up before running.
        database.messageDao().deleteAll()
        database.messageFragmentDao().deleteAll()


        try {
            val res = getApplicationContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch(e: Exception) {
            // look for DeadObjectException if connection is broken
            // look for RemoteException
            Log.e("MainActivity","Error binding", e)
        }

        registerReceiver(receiver, intentFilter)

        if(appMode == "MODE_RELAY")
            Intent(this, MailSyncService::class.java).also { intent -> startService(intent)}

        Intent(this, MessageFragmentSyncService::class.java).also { intent -> startService(intent)}
    }


    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)


    }

}



