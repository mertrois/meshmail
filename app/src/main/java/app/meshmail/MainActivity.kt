package app.meshmail

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.geeksville.mesh.IMeshService
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.meshmail.MeshmailApplication.Companion.prefs
import app.meshmail.android.Parameters
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.service.MailSyncService
import app.meshmail.service.MeshBroadcastReceiver
import app.meshmail.service.MeshServiceManager
import app.meshmail.service.MessageFragmentSyncService
import app.meshmail.ui.ClientMessageListFragment
import app.meshmail.ui.PreferenceFragment
import app.meshmail.ui.StatusRelayFragment


class MainActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, ClientMessageListFragment.FragmentRequestListener {
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
            R.id.empty_trash -> {
                // get a reference to the current fragment
                val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.fragment_container)
                // see if non null, and is of the correct type
                if((currentFragment != null) && (currentFragment is ClientMessageListFragment)) {
                    (currentFragment as ClientMessageListFragment).emptyTrash()
                }
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

    override fun loadMessageFragment(message: MessageEntity) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, MessageFragment(message))
            .addToBackStack("main")
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // todo: remove; only for dev. Clean up before running.

        // database.messageDao().deleteAll()
        // database.messageFragmentDao().deleteAll()

        // load initial fragment based on mode the app is in
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, if(prefs?.getBoolean("relay_mode") == false) ClientMessageListFragment() else StatusRelayFragment())
            .commit()

        try {
            applicationContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch(e: Exception) {
            // todo: look for DeadObjectException if connection broken; RemoteException
            Log.e("MainActivity","Error binding", e)
        }

        /* todo: make all the services run in background, not tied to activity lifecycle
            to continue syncing mail and/or message fragments and protocol messages
            while scree off/activity closed, etc.
         */
        registerReceiver(receiver, intentFilter)
        Intent(this, MailSyncService::class.java).also { intent -> startService(intent)}
        Intent(this, MessageFragmentSyncService::class.java).also { intent -> startService(intent)}

        val appType: String = if(prefs?.getBoolean("relay_mode") == true) "relay" else "client"
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
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
            title = pref.title
            return true
        }

}



