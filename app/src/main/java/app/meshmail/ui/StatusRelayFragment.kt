package app.meshmail.ui

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.meshmail.MeshmailApplication
import app.meshmail.R
import app.meshmail.data.MeshmailDatabase
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
    lateinit var smtpStatusView: TextInputEditText
    lateinit var messagesSentView: TextInputEditText
    lateinit var outboxCountView: TextInputEditText
    lateinit var lastClientContactView: TextInputEditText
    lateinit var messagesForwardedView: TextInputEditText
    lateinit var outboxCount: LiveData<Int>
    lateinit var sentCount: LiveData<Int>
    lateinit var inboundCount: LiveData<Int>

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
        val db: MeshmailDatabase = (requireContext().applicationContext as MeshmailApplication).database

        // get refs to text views
        lastClientContactView = view.findViewById<TextInputEditText>(R.id.last_client_contact)
        imapStatusView = view.findViewById<TextInputEditText>(R.id.imap_server_status)
        messagesForwardedView = view.findViewById<TextInputEditText>(R.id.messages_forwarded)
        smtpStatusView = view.findViewById<TextInputEditText>(R.id.smtp_server_status)
        outboxCountView = view.findViewById<TextInputEditText>(R.id.smtp_queue_size)
        messagesSentView = view.findViewById<TextInputEditText>(R.id.smtp_messages_sent)


        /*
            The following three blocks monitor live queries and push to the views automagically
         */
        inboundCount = db.messageDao().getInboundMessagesCount()
        messagesForwardedView.setText(inboundCount.value.toString())
        inboundCount.observe(viewLifecycleOwner) { newCount ->
            messagesForwardedView.setText(newCount.toString())
        }

        outboxCount = db.messageDao().getOutboxCount()
        outboxCountView.setText(outboxCount.value.toString())
        outboxCount.observe(viewLifecycleOwner) { newCount ->
            outboxCountView.setText(newCount.toString())
        }

        sentCount = db.messageDao().getSentMessagesCount()
        messagesSentView.setText(sentCount.value.toString())
        sentCount.observe(viewLifecycleOwner) { newCount ->
            messagesSentView.setText(newCount.toString())
        }


        /*
            Set up observers of the status objects
         */
        statusManager.imapStatus.renderedValue.observe(viewLifecycleOwner) { newString ->
            imapStatusView.setText(newString)
        }

        statusManager.smtpStatus.renderedValue.observe(viewLifecycleOwner) { newString ->
            smtpStatusView.setText(newString)
        }

        statusManager.lastContact.renderedValue.observe(viewLifecycleOwner) { newString ->
            lastClientContactView.setText(newString)
        }
    }

    // todo: add another for the status of the meshtastic service "not found-->did you install meshtastic?" is it running?

    override fun onDestroyView() {
        super.onDestroyView()

        // remove all observers to prevent live data objects from calling nonexistent code

        inboundCount.removeObservers(viewLifecycleOwner)
        outboxCount.removeObservers(viewLifecycleOwner)
        sentCount.removeObservers(viewLifecycleOwner)

        statusManager.imapStatus.renderedValue.removeObservers(viewLifecycleOwner)
        statusManager.smtpStatus.renderedValue.removeObservers(viewLifecycleOwner)
        statusManager.lastContact.renderedValue.removeObservers(viewLifecycleOwner)
    }


}

