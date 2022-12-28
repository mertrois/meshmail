package app.meshmail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.meshmail.data.MessageEntity



class MessageFragment(message: MessageEntity) : Fragment() {

    private lateinit var fromField: TextView
    private lateinit var toField: TextView
    private lateinit var dateField: TextView
    private lateinit var subjectField: TextView
    private lateinit var bodyField: TextView
    private val message = message

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message, container, false)

        fromField = view.findViewById(R.id.message_from_field)
        toField = view.findViewById(R.id.message_to_field)
        dateField = view.findViewById(R.id.message_date_field)
        subjectField = view.findViewById(R.id.message_subject_field)
        bodyField = view.findViewById(R.id.message_body_field)

        fromField.setText(message.sender)
        toField.setText(message.recipient)
        dateField.setText(message.receivedDate.toString()) // improve
        subjectField.setText(message.subject)
        bodyField.setText(message.body)

        return view
    }


}