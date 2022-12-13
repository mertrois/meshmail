package app.meshmail

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.geeksville.mesh.IMeshService
import android.util.Log
import android.widget.TextView

import app.meshmail.MeshmailApplication.Companion.prefs


import app.meshmail.data.protobuf.EmailOuterClass
import com.geeksville.mesh.DataPacket
import com.geeksville.mesh.MessageStatus
import com.geeksville.mesh.NodeInfo


import app.meshmail.mail.MailSyncService



class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var statusText: TextView
    private lateinit var inputText: TextView

    private var meshService: IMeshService? = null

    private val serviceIntent = Intent().apply {
        setClassName(
            "com.geeksville.mesh",
            "com.geeksville.mesh.service.MeshService"
        )
    }

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            meshService = IMeshService.Stub.asInterface(service)
            (getApplication() as MeshmailApplication).meshService = meshService // hacky, just for now
            // this@MainActivity.meshService = ...
            Log.d(MainActivity::class.java.simpleName, "service connected")
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(MainActivity::class.java.simpleName, "service disconnected")
            meshService = null
            (getApplication() as MeshmailApplication).meshService = null // hacky, change later.
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
                else -> {
                    val act = intent.action ?: ""
                    Log.d("onReceive", "received an action: $act")
                    try {
                        var data: DataPacket =
                            intent?.getParcelableExtra("com.geeksville.mesh.Payload")!!
                        var em = EmailOuterClass.Email.parseFrom(data.bytes)
                        statusText.append(em.toString())
                        statusText.append("\n")
                    } catch(e: Exception) {
                        Log.e("onReceive", "error decoding protobuf. unexpected input.")
                    }

                }
            }
        }
    }

    var intentFilter = IntentFilter().apply {
        addAction("com.geeksville.mesh.NODE_CHANGE")
        addAction("com.geeksville.mesh.MESH_CONNECTED")
        addAction("com.geeksville.mesh.RECEIVED.309")

    }

    fun sendMessage(s: String) {
        val dp = DataPacket(to=DataPacket.ID_BROADCAST,
            s.toByteArray(),
            dataType=309)
        try {
            meshService?.send(dp)
        } catch(e: Exception) {
            Log.e("sendMessage", "Message failed to send", e)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {

        val appMode = prefs?.getString("APP_MODE","MODE_CLIENT")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        inputText = findViewById(R.id.chatTextToSend)

        button = findViewById(R.id.button)
        button.setOnClickListener { v ->
            Log.d(MainActivity::class.java.simpleName, "button clicked")
            sendMessage(inputText.text.toString())
            inputText.text = ""
        }

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
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)


    }



}



