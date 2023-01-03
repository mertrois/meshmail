package app.meshmail.ui


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import android.widget.Toast
import app.meshmail.MeshmailApplication
import app.meshmail.R
import app.meshmail.android.PrefsManager
import app.meshmail.data.MessageEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import app.meshmail.util.md5
import app.meshmail.util.toHex
import java.util.*


class EditMessageFragment(m: MessageEntity) : Fragment() {
    private lateinit var app: MeshmailApplication
    private lateinit var prefs: PrefsManager
    private lateinit var fromField: EditText
    private lateinit var toField: EditText
    private lateinit var subjectField: EditText
    private lateinit var bodyField: EditText
    private lateinit var sendFAB: FloatingActionButton
    private val message: MessageEntity = MessageEntity( subject=m.subject, recipient = m.recipient, body=m.body,
                                                        serverId=m.serverId, receivedDate = m.receivedDate, folder= m.folder)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message_edit, container, false)

        setHasOptionsMenu(true)

        fromField = view.findViewById(R.id.message_from_edit)
        toField = view.findViewById(R.id.message_to_edit)
        subjectField = view.findViewById(R.id.message_subject_edit)
        bodyField = view.findViewById(R.id.message_body_field)

        val senderName = prefs.getString("sender_name")
        val senderEmail = prefs.getString("sender_email")
        fromField.setText("$senderName <$senderEmail>")
        fromField.isEnabled = false

        // new to: is the old recipient
        toField.setText(message.recipient)

        var subj = if(isReply()) "Re: " else ""
        subjectField.setText(subj + message.subject)

        val sig = prefs.getString("sender_signature")
        bodyField.setText(sig)
        formatBodyForReply()

        sendFAB = view.findViewById(R.id.fabSend)
        sendFAB.setOnClickListener { onSendFABClicked() }

        return view
    }

    private fun onSendFABClicked() {
        message.type = "OUTBOUND"   // mailSyncService will look for this and attempt to transmit
        message.folder = "OUTBOX"   // mailSyncService will look for this and generate fragments
        message.isShadow = false
        message.subject = subjectField.text.toString()
        message.body = bodyField.text.toString()
        message.recipient = toField.text.toString()
        message.sender = fromField.text.toString()
        message.fingerprint = md5(message.body + Date().toString() + message.subject + message.recipient).toHex().substring(0,8)


        try {
            app.database.messageDao().insert(message)
            Toast.makeText(app, "Message enqueued for transmission", Toast.LENGTH_SHORT).show()
        } catch(e: Exception) {
            Toast.makeText(app, "Error sending message", Toast.LENGTH_SHORT).show()
        }
        activity?.supportFragmentManager?.popBackStack()
    }


    @SuppressLint("SetTextI18n")
    private fun formatBodyForReply() {
        val sig = prefs.getString("sender_signature")
        if(isReply()) { // only reformat if it's a reply
            bodyField.setText("""$sig


===== On ${message.receivedDate} ${message.recipient} wrote: =====
    
${message.body}
            """)
        }
    }

    private fun isReply(): Boolean {
        return message.serverId != ""
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        app = requireActivity().application as MeshmailApplication
        prefs = app.prefs
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        //inflater.inflate(R.menu.menu_message, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
//        when(item.itemId) {
//            R.id.move_to_archive, R.id.move_to_trash, R.id.move_to_inbox -> {
//                message.folder = item.title.toString()
//                (app as MeshmailApplication).database.messageDao().update(message)
//                activity?.supportFragmentManager?.popBackStack()
//                return true
//            }
//        }
        return false
    }


}