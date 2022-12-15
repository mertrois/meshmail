package app.meshmail

import android.app.Application
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.preference.PreferenceManager
import app.meshmail.android.PrefsManager
//import com.facebook.flipper.android.AndroidFlipperClient
//import com.facebook.flipper.android.utils.FlipperUtils
//import com.facebook.flipper.plugins.inspector.DescriptorMapping
//import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
//import com.facebook.soloader.SoLoader
import com.geeksville.mesh.IMeshService

class MeshmailApplication : Application() {

    companion object {
        var prefs: PrefsManager? = null
    }
    var meshService: IMeshService? = null


    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)


//        SoLoader.init(this, false)
//
//        if (BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(this)) {
//            val client = AndroidFlipperClient.getInstance(this)
//            client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
//            client.start()
//        }
    }






}