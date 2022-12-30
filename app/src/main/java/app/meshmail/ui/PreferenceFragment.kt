package app.meshmail.ui


import android.os.Bundle
import android.view.*
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import app.meshmail.R

class PreferenceFragment : PreferenceFragmentCompat() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_preferences, menu)
        menu.clear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
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
        val idPreferenceCategory = findPreference<PreferenceCategory>("identity_preference_category")
        imapPreferenceCategory?.isEnabled = isEnabled
        smtpPreferenceCategory?.isEnabled = isEnabled

        // need to think more about the UX here. Should really be part of SMTP settings because it has to
        // match the account that's sending. But sender will want to know/be reminded/be able to set how
        // it's going out as well. But enabling both client/relay to set creates confusion.
        idPreferenceCategory?.isEnabled = !isEnabled
    }

}