package app.meshmail

import android.app.Application

import androidx.room.Room
import app.meshmail.android.PrefsManager
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.StatusManager
import app.meshmail.service.MeshServiceManager
import app.meshmail.service.MessageFragmentSyncService

import com.geeksville.mesh.IMeshService

class MeshmailApplication : Application() {

    lateinit var prefs: PrefsManager
    var statusManager: StatusManager = StatusManager()
    var meshService: IMeshService? = null
    val meshServiceManager: MeshServiceManager = MeshServiceManager(this)
    var fragmentSyncService: MessageFragmentSyncService? = null


    // todo: restructure to remove allowmainthreadqueries
    val database: MeshmailDatabase by lazy {
        Room.databaseBuilder(
            this,
            MeshmailDatabase::class.java,
            "meshmail_database"
        ).fallbackToDestructiveMigration().allowMainThreadQueries().build()
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)
        statusManager.startUpdateThread()
    }

}