package app.meshmail.android

import android.content.Context
import androidx.preference.PreferenceManager


class PrefsManager(val context: Context, val name: String = "prefs") {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    //val sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    val sharedPreferenceContents = sharedPreferences.all

    fun getString(key: String, default: String=""): String {
        return if(sharedPreferenceContents.containsKey(key)) {
            sharedPreferenceContents[key] as String
        } else {
            val editor = sharedPreferences.edit()
            editor.putString(key,default)
            editor.apply()
            default
        }
    }

    fun getBoolean(key: String, default: Boolean=false): Boolean {
        return if(sharedPreferenceContents.containsKey(key)) {
            sharedPreferenceContents[key] as Boolean
        } else {
            val editor = sharedPreferences.edit()
            editor.putBoolean(key, default)
            editor.apply()
            default
        }
    }
}


