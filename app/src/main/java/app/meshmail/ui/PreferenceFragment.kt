package app.meshmail.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import app.meshmail.R

class PreferenceFragment : PreferenceFragmentCompat() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val relayPreference = findPreference<SwitchPreference>("relay_mode")
        if(relayPreference != null)
            grayOutCategories(relayPreference.isChecked)

        relayPreference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val isOn = newValue as Boolean
                grayOutCategories(isOn)
                true
            }
    }

    fun grayOutCategories(isEnabled: Boolean) {
        val imapPreferenceCategory = findPreference<PreferenceCategory>("imap_preference_category")
        val smtpPreferenceCategory = findPreference<PreferenceCategory>("smtp_preference_category")
        imapPreferenceCategory?.isEnabled = isEnabled
        smtpPreferenceCategory?.isEnabled = isEnabled
    }

}