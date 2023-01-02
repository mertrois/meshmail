package app.meshmail.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import app.meshmail.R
import java.util.*

/*
    Fragment that displays the status of the relay
    e.g. if the mail servers are reachable, number of messages forwarded
    last contact with a client, etc...
 */
class StatusRelayFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.relay_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update the uptime, CPU usage, memory usage, and disk usage
        // text views with the current values
        view.findViewById<TextView>(R.id.text_uptime).text = "Uptime: ${getUptime()} "
        view.findViewById<TextView>(R.id.text_last_contact).text = "Last Client Contact: ${getLastContact()} minutes ago"
        view.findViewById<TextView>(R.id.imap_server_status).text = "IMAP Server: ${getIMAPStatus()}"
        view.findViewById<TextView>(R.id.smtp_server_status).text = "SMTP Server: ${getSMTPStatus()}"
    }

    // todo: add another for the status of the meshtastic service "not found-->did you install meshtastic?" is it running?

    private fun getUptime(): String {
        // TODO: Implement this method to return the uptime as a string
        return "0 days 0 hours 0 minutes"
    }

    private fun getLastContact(): Int {
        // todo: return last contact with a client
        return 0
    }

    private fun getIMAPStatus(): Int {
        // TODO: IMAP server status
        return 0
    }

    private fun getSMTPStatus(): Int {
        // TODO: SMTP server status
        return 0
    }

}
