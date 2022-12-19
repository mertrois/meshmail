package app.meshmail.ui


import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import app.meshmail.R

class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)


    }
}