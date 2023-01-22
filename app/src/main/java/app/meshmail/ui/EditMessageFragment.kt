package app.meshmail.ui


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.meshmail.MainActivity
import app.meshmail.MeshmailApplication
import app.meshmail.R
import app.meshmail.android.PrefsManager
import app.meshmail.data.MessageEntity
import app.meshmail.util.md5
import app.meshmail.util.toHex
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val REQUEST_SELECT_CONTACT = 1

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

        // the sender is set in app settings
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

        // setup launch address book to pick an email
        // todo: instead of long-press to launch, add contact icon to right side of field to press
        toField.setOnLongClickListener {
            val selectContactIntent = Intent(Intent.ACTION_PICK).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE
            }
            val ctx: MainActivity = context as MainActivity
            try {
                startActivityForResult(selectContactIntent, REQUEST_SELECT_CONTACT)
            } catch(e: Exception) {
                Toast.makeText(context, "Sorry, could not launch address book", Toast.LENGTH_SHORT).show()
            }
            true
        }
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_SELECT_CONTACT && resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = data?.data
            val projection = arrayOf(ContactsContract.Contacts._ID)
            val ctx = context as MainActivity
            if(contactUri != null) {
                try {
                    val cursor: Cursor? = ctx.contentResolver.query(contactUri, projection, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val id: String = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))

                        var email: String? = null
                        val c2: Cursor? = ctx.contentResolver.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        if (c2 != null && c2.moveToFirst()) {
                            email = c2.getString(c2.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA))
                            c2.close()
                        }

                        toField.setText(email)
                    }
                    cursor?.close()
                } catch(e: Exception) {
                    Toast.makeText(context, "Error getting selected email address", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
            CoroutineScope(Dispatchers.IO).launch {
                app.database.messageDao().insert(message)
            }
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

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        super.onOptionsItemSelected(item)
//        return false
//    }


}