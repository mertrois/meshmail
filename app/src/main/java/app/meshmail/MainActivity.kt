package app.meshmail

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils.replace
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.geeksville.mesh.IMeshService
import android.util.Log
import android.widget.TextView
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import app.meshmail.MeshmailApplication.Companion.prefs
import app.meshmail.android.Parameters
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.data.MessageFragmentEntity

import app.meshmail.data.protobuf.MessageFragmentRequestOuterClass.MessageFragmentRequest
import app.meshmail.data.protobuf.ProtocolMessageOuterClass.ProtocolMessage
import app.meshmail.data.protobuf.ProtocolMessageTypeOuterClass.ProtocolMessageType
import app.meshmail.data.protobuf.MessageFragmentOuterClass.MessageFragment
import app.meshmail.data.protobuf.MessageShadowOuterClass.MessageShadow
import app.meshmail.data.protobuf.MessageOuterClass.Message
import com.geeksville.mesh.DataPacket

import app.meshmail.service.MailSyncService
import app.meshmail.service.MeshServiceManager
import app.meshmail.service.MessageFragmentSyncService
import com.google.protobuf.kotlin.toByteString
import org.osgeo.proj4j.parser.Proj4Keyword.a
import app.meshmail.ui.PreferenceFragment


class MainActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    //private lateinit var statusText: TextView


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
                        val pbProtocolMessage = ProtocolMessage.parseFrom(data.bytes)
                        val resultStr: String = when(pbProtocolMessage.pmtype) {

                            ProtocolMessageType.SHADOW_BROADCAST -> {
                                val pbMessageShadow: MessageShadow = pbProtocolMessage.messageShadow


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
                                pbMessageShadow.let { ms ->
                                    /* just for debugging */
                                    val sb = StringBuilder()
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
                            } ProtocolMessageType.FRAGMENT_REQUEST -> {
                                val pbMessageFragmentRequest: MessageFragmentRequest = pbProtocolMessage.messageFragmentRequest
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
                                pbMessageFragmentRequest.let { req ->
                                    /* just for debugging */
                                    val sb = StringBuilder()
                                    sb.appendLine("Received new Fragment Request:")
                                    sb.appendLine("Fingerprint: ${req.fingerprint}")
                                    sb.appendLine("Frag num: ${req.m}")
                                    sb.toString()
                                }

                            /*
                                Handle a received fragment:
                                add to database, see if we now have all the pieces to make a message and upgrade the message
                             */
                            } ProtocolMessageType.FRAGMENT_BROADCAST -> {
                                var result: String
                                val pbMessageFragment: MessageFragment = pbProtocolMessage.messageFragment
                                // insert this message fragment into the database
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
                                        val pbMessage = Message.parseFrom(buffer)
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
                                result
                            } else -> {
                                "unknown protocol message. don't know how to parse this yet"
                            }
                        }
//                        statusText.append(resultStr)
//                        statusText.append("\n")
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
    }

    private var intentFilter = IntentFilter().apply {
        addAction("com.geeksville.mesh.NODE_CHANGE")
        addAction("com.geeksville.mesh.MESH_CONNECTED")
        addAction("com.geeksville.mesh.RECEIVED.${Parameters.MESHMAIL_PORT}")
        addAction("com.geeksville.mesh.MESSAGE_STATUS")
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val relayMode = prefs?.getBoolean("relay_mode", false)

        // for now, load prefs frag to mainactivity right away
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, PreferenceFragment())
            .commit()

        // todo: remove; only for dev. Clean up before running.
        database.messageDao().deleteAll()
        database.messageFragmentDao().deleteAll()

        try {
            applicationContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch(e: Exception) {
            // look for DeadObjectException if connection is broken
            // look for RemoteException
            Log.e("MainActivity","Error binding", e)
        }

        registerReceiver(receiver, intentFilter)

        if(relayMode!!)
            Intent(this, MailSyncService::class.java).also { intent -> startService(intent)}

        Intent(this, MessageFragmentSyncService::class.java).also { intent -> startService(intent)}
    }


    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)


    }


    /*
    Preference related stuff
     */

//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//    }


    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment ?: return false
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

}



