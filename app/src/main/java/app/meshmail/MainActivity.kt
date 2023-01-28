package app.meshmail

import android.content.*
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.geeksville.mesh.IMeshService
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.meshmail.android.Parameters
import app.meshmail.android.PrefsManager
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.service.MailSyncService
import app.meshmail.service.MeshBroadcastReceiver
import app.meshmail.service.MeshServiceManager
import app.meshmail.service.MessageFragmentSyncService
import app.meshmail.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, ClientMessageListFragment.FragmentRequestListener {
    private val meshServiceManager: MeshServiceManager by lazy { (application as MeshmailApplication).meshServiceManager }
    private lateinit var prefs: PrefsManager
    private val serviceIntent = Intent().apply {
        setClassName(
            "com.geeksville.mesh",
            "com.geeksville.mesh.service.MeshService"
        )
    }

    private val database: MeshmailDatabase by lazy { (applicationContext as MeshmailApplication).database }

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, PreferenceFragment())
                    .addToBackStack("main")
                    .commit()
                return true
            }

            android.R.id.home -> {
                return if(supportFragmentManager.backStackEntryCount == 0) {
                    finish()
                    true
                } else {
                    supportFragmentManager.popBackStack()
                    true
                }
            } else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun loadMessageFragment(message: MessageEntity, mode: Int) {
        val fragment: Fragment = when (mode) {
            ClientMessageListFragment.FragmentRequestListener.MODE_VIEW -> ViewMessageFragment(message)
            ClientMessageListFragment.FragmentRequestListener.MODE_EDIT -> EditMessageFragment(message)
            else -> throw RuntimeException("Illegal argument to loadMessageFragment")
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("main")
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = (application as MeshmailApplication).prefs

        setContentView(R.layout.activity_main)

        // lock to portrait until code to recreate state of fragments can be completed
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // only for dev. Clean up before running.
//        CoroutineScope(Dispatchers.IO).launch {
//            database.messageDao().deleteAll()
//            database.messageFragmentDao().deleteAll()
//        }

        // load initial fragment based on mode the app is in
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, if(!prefs.getBoolean("relay_mode")) ClientMessageListFragment() else StatusRelayFragment())
            .commit()

        try {
            applicationContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch(e: Exception) {
            // todo: look for DeadObjectException if connection broken; RemoteException
            Log.e("MainActivity","Error binding", e)
        }

        registerReceiver(receiver, intentFilter)
        Intent(this, MailSyncService::class.java).also { intent ->  startForegroundService(intent)}

        Intent(this, MessageFragmentSyncService::class.java).also { intent -> startForegroundService(intent)}

        val appType: String = if(prefs.getBoolean("relay_mode")) "relay" else "client"
        supportActionBar?.title = "Meshmail — $appType"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // displays back arrow if true
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.elevation = 4.0f
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
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
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

}



