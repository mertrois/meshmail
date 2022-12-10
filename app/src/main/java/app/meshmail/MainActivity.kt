package app.meshmail

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.geeksville.mesh.IMeshService
import android.util.Log
import android.widget.TextView
import com.geeksville.mesh.DataPacket
import com.geeksville.mesh.MessageStatus
import com.geeksville.mesh.NodeInfo

//// imap

import java.util.Properties
import android.os.AsyncTask
import androidx.room.Room
import app.meshmail.R
import app.meshmail.data.AppDatabase
import app.meshmail.data.TestEntity
import app.meshmail.mail.MailSyncService
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var statusText: TextView
    private lateinit var inputText: TextView

    private lateinit var mailSyncService: MailSyncService

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
            // this@MainActivity.meshService = ...
            Log.d(MainActivity::class.java.simpleName, "service connected")
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(MainActivity::class.java.simpleName, "service disconnected")
            meshService = null
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
                    var data:DataPacket = intent?.getParcelableExtra("com.geeksville.mesh.Payload")!!

                    statusText.append(data.bytes?.decodeToString())
                    statusText.append("\n")
                }
            }
        }
    }

    var intentFilter = IntentFilter().apply {
        addAction("com.geeksville.mesh.NODE_CHANGE")
        addAction("com.geeksville.mesh.MESH_CONNECTED")
        addAction("com.geeksville.mesh.RECEIVED.309")
        addAction("com.geeksville.mesh.RECEIVED_OPAQUE")
        addAction("com.geeksville.mesh.RECEIVED_DATA")
        addAction("com.geeksville.mesh.MESSAGE_STATUS_CHANGED")
    }

    private fun sendMessage(s: String) {
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

        Intent(this, MailSyncService::class.java).also { intent -> startService(intent)}
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }



}



