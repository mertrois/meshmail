package app.meshmail.android

import android.content.Context
import androidx.preference.PreferenceManager



class PrefsManager(val context: Context, val name: String = "prefs") {
    var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    //val sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    var sharedPreferenceContents = sharedPreferences.all

    fun refresh() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferenceContents = sharedPreferences.all
    }

    fun putInt(key: String, value: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getInt(key: String, default: Int=0): Int {
        refresh()
        return if(sharedPreferenceContents.containsKey(key)) {
            sharedPreferenceContents[key] as Int
        } else {
            val editor = sharedPreferences.edit()
            editor.putInt(key, default)
            editor.apply()
            default
        }
    }

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


