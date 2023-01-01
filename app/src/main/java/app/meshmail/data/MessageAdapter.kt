package app.meshmail.data

import android.graphics.Typeface
import android.util.TypedValue
import java.text.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.meshmail.R
import java.util.*


class MessageAdapter(private val onClickListener: OnClickListener) :
    ListAdapter<MessageEntity, MessageAdapter.ViewHolder>(MessageDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderTextView: TextView = itemView.findViewById(R.id.sender_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
        val subjectTextView: TextView = itemView.findViewById(R.id.subject_text_view)
        val viewSwitcher: ViewSwitcher = itemView.findViewById(R.id.progress_view_switcher)
        val progBar: ProgressBar = itemView.findViewById(R.id.fragment_load_progress)

        private val normFace: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        private val boldFace: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)


        fun setTypeface(bold: Boolean) {
            var useFace = normFace
            if (bold) useFace = boldFace

            senderTextView.typeface = useFace
            dateTextView.typeface = useFace
            subjectTextView.typeface = useFace
        }

        fun setGrayedOut(grayed: Boolean) {
            if (grayed)  // light gray color
                subjectTextView.alpha = .5f

            else {      // otherwise, set it to the theme default
                subjectTextView.alpha = 1.0f
            }
        }
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
        holder.itemView.setOnClickListener {
            onClickListener.onClick(message)
        }

        holder.setTypeface(!message.hasBeenRead)
        holder.itemView.isEnabled = !message.isShadow
        holder.setGrayedOut(message.isShadow)

        if(message.fragsReceived == 0)
            holder.progBar.isIndeterminate = true
        else {
            holder.progBar.max = message.nFragments
            holder.progBar.progress = message.fragsReceived
            holder.progBar.isIndeterminate = false
        }

        if(message.isShadow) {
            holder.viewSwitcher.displayedChild = 0
        } else
            holder.viewSwitcher.displayedChild = 1
    }

    class OnClickListener(val clickListener: (message: MessageEntity) -> Unit) {
        fun onClick(message: MessageEntity) = clickListener(message)
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<MessageEntity>() {
        override fun areItemsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean {
            return oldItem.fingerprint == newItem.fingerprint
        }

        override fun areContentsTheSame(oldItem: MessageEntity, newItem: MessageEntity): Boolean {
            return oldItem == newItem
        }
    }


    private fun getFormattedDate(message: MessageEntity): String {
        if(message.receivedDate == null) return ""  // shadow messages will have a null date field.

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
            val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
            dateString = timeFormat.format(msgDate)
        }

        return dateString
    }

    fun getFormattedSender(message: MessageEntity): String {
        if(message.sender == null) return ""    // shadow messages won't have sender
        // if there is plain text before the < > section, just use that
        // if none, then extract the address between the < >'s and use that

        // following email regex from https://regexr.com/2rhq7 -- matches RFC2822 emails
        val emailRegex = """[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?""".toRegex()
        val sender = message.sender.toString()
        val emailMatch: MatchResult? = emailRegex.find(sender)
        var senderDisplay = ""      // default to show nothing in the worse case if totally unparseable
        if(emailMatch != null) {    // somehow this didn't contain any email. huh.
            if(emailMatch.groups.size > 0) {
                senderDisplay = emailMatch.groups[0]?.value ?: ""    // we're going to default to the username@domain.com
                val firstAngle = sender.findAnyOf(listOf("<"))
                if(firstAngle != null) {
                    val name = sender.substring(0, firstAngle.first).trim()
                    if(name != "") senderDisplay = name
                }
            }
        }

        return senderDisplay
    }

}