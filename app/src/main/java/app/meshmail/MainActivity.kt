package app.meshmail

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.geeksville.mesh.IMeshService
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.meshmail.MeshmailApplication.Companion.prefs
import app.meshmail.android.Parameters
import app.meshmail.data.MeshmailDatabase
import app.meshmail.service.MailSyncService
import app.meshmail.service.MeshBroadcastReceiver
import app.meshmail.service.MeshServiceManager
import app.meshmail.service.MessageFragmentSyncService
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

    private val receiver by lazy { MeshBroadcastReceiver(application) }

    private var intentFilter = IntentFilter().apply {
        addAction("com.geeksville.mesh.NODE_CHANGE")
        addAction("com.geeksville.mesh.MESH_CONNECTED")
        addAction("com.geeksville.mesh.RECEIVED.${Parameters.MESHMAIL_PORT}")
        addAction("com.geeksville.mesh.MESSAGE_STATUS")
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


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
            // todo: look for DeadObjectException if connection broken; RemoteException
            Log.e("MainActivity","Error binding", e)
        }

        registerReceiver(receiver, intentFilter)

        if(prefs?.getBoolean("relay_mode", false)!!)
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



