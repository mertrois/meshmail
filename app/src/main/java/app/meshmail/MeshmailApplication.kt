package app.meshmail

import android.app.Application

import androidx.room.Room
import app.meshmail.android.PrefsManager
import app.meshmail.data.MeshmailDatabase
import app.meshmail.service.MeshServiceManager
import app.meshmail.service.MessageFragmentSyncService

import com.geeksville.mesh.IMeshService

class MeshmailApplication : Application() {

    companion object {
        var prefs: PrefsManager? = null
    }
    var meshService: IMeshService? = null
    val meshServiceManager: MeshServiceManager = MeshServiceManager(this)
    var fragmentSyncService: MessageFragmentSyncService? = null

    // todo: restructure to remove allowmainthreadqueries ... only avoiding premature optimization in development
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
    }






}