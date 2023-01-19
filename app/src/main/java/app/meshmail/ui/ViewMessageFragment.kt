package app.meshmail.ui

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.EditText
import app.meshmail.ui.ClientMessageListFragment.FragmentRequestListener
import androidx.fragment.app.Fragment
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.forEach
import app.meshmail.MeshmailApplication
import app.meshmail.R
import app.meshmail.data.MessageEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ViewMessageFragment(message: MessageEntity) : Fragment() {
    private lateinit var app: Application
    private lateinit var fromField: TextView
    private lateinit var toField: TextView
    private lateinit var dateField: TextView
    private lateinit var subjectField: TextView
    private lateinit var bodyField: TextView
    private lateinit var replyFAB: FloatingActionButton
    private val message = message

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message_view, container, false)

        fromField = view.findViewById(R.id.message_from_field)
        toField = view.findViewById(R.id.message_to_field)
        dateField = view.findViewById(R.id.message_date_field)
        subjectField = view.findViewById(R.id.message_subject_field)
        bodyField = view.findViewById(R.id.message_body_field)
        replyFAB = view.findViewById(R.id.fabSend)

        fromField.setText("From: ${message.sender}")
        toField.setText("To: ${message.recipient}")
        dateField.setText(message.receivedDate.toString()) // improve
        subjectField.setText(message.subject)
        bodyField.setText(message.body)


        setHasOptionsMenu(true)

        replyFAB.setOnClickListener {
            val reply: MessageEntity = message.copy(folder="DRAFTS", isShadow=false, type="OUTBOUND", hasBeenSent=false)
            reply.recipient = message.sender
            (activity as FragmentRequestListener).loadMessageFragment(reply, FragmentRequestListener.MODE_EDIT)
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        app = requireActivity().application
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_message, menu)
        menu.forEach { item ->
            if(item.title == message.folder)
                item.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when(item.itemId) {
            R.id.move_to_archive, R.id.move_to_trash, R.id.move_to_inbox -> {
                message.folder = item.title.toString()
                CoroutineScope(Dispatchers.IO).launch {
                    (app as MeshmailApplication).database.messageDao().update(message)
                }
                activity?.supportFragmentManager?.popBackStack()
                return true
            }
        }
        return false
    }


}