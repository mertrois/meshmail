package app.meshmail.data



import java.text.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.meshmail.R
import java.util.*


class MessageAdapter : ListAdapter<MessageEntity, MessageAdapter.ViewHolder>(MessageDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderTextView: TextView = itemView.findViewById(R.id.sender_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
        val subjectTextView: TextView = itemView.findViewById(R.id.subject_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        holder.senderTextView.text = getFormattedSender(message)
        holder.dateTextView.text = getFormattedDate(message)
        holder.subjectTextView.text = message.subject?.trim()
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<MessageEntity>() {
        override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean {
            return oldItem.fingerprint == newItem.fingerprint
        }

        override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean {
            return oldItem == newItem
        }
    }


    fun getFormattedDate(message: MessageEntity): String {
        val now: Calendar = Calendar.getInstance() // get our current date/time for reference
        val msgDate: Date? = message.receivedDate
        val msgCal: Calendar = Calendar.getInstance()
        msgCal.time = msgDate

        var dateString = ""

        // if it's more than 12 hours ago, display as a date, otherwise display as time
        if(now.timeInMillis - msgCal.timeInMillis > 12 * 60 * 60 * 1000) {
            val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT) // options are short, medium, long, full
            dateString = dateFormat.format(msgDate)
        } else {
            val timeFormat = DateFormat.getTimeInstance()
            dateString = timeFormat.format(msgDate)
        }

        return dateString
    }

    fun getFormattedSender(message: MessageEntity): String {
        // if there is plain text before the < > section, just use that
        // if none, then extract the address between the < >'s and use that

        val regex = Regex("(?<name>.*)<(?<addr>[^<>]+@[^<>]+)>")
        val sender = regex.find(message.sender.toString())
        var senderDisplay = ""
        if(sender != null) {
            val name = sender.groups["name"]?.value
            val addr = sender.groups["addr"]?.value
            if(name?.trim() == "")
                senderDisplay = addr!!
            else
                senderDisplay = name!!
        }

        return senderDisplay
    }

}