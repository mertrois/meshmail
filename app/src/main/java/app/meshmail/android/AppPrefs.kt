package app.meshmail.android

import android.content.Context



class PrefsManager(val context: Context, val name: String = "prefs") {
    val sharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
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
}


