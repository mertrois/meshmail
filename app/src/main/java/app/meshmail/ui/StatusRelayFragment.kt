package app.meshmail.ui

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import app.meshmail.MeshmailApplication
import app.meshmail.R
import app.meshmail.data.StatusManager
import app.meshmail.service.MeshServiceManager
import com.google.android.material.textfield.TextInputEditText
import java.util.*

/*
    Fragment that displays the status of the relay
    e.g. if the mail servers are reachable, number of messages forwarded
    last contact with a client, etc...
 */
class StatusRelayFragment : Fragment() {

    lateinit var statusManager: StatusManager

    lateinit var imapStatusView: TextInputEditText



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.relay_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusManager = (requireContext().applicationContext as MeshmailApplication).statusManager

        imapStatusView = view.findViewById<TextInputEditText>(R.id.imap_server_status)

        statusManager.imapStatus.renderedValue.observe(viewLifecycleOwner) { newString ->
            imapStatusView.setText(newString)
        }


        view.findViewById<TextView>(R.id.last_client_contact).text = ""
        view.findViewById<TextView>(R.id.smtp_queue_size).text = "6"

        view.findViewById<TextView>(R.id.smtp_server_status).text = ""
    }

    // todo: add another for the status of the meshtastic service "not found-->did you install meshtastic?" is it running?

    override fun onDestroyView() {
        super.onDestroyView()
        statusManager.imapStatus.renderedValue.removeObservers(viewLifecycleOwner)
    }


}

