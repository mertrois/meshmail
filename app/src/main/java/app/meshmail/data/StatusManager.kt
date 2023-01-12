package app.meshmail.data

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import java.util.Date

class StatusManager() {

    var imapStatus: IMAPStatusInstance = IMAPStatusInstance()
    var smtpStatus: SMTPStatusInstance = SMTPStatusInstance()
    var lastContact: LastContactStatusInstance = LastContactStatusInstance()

    val handler = Handler()

    init {
        // nothing
    }

    private val runnable = object: Runnable {
        override fun run() {
            imapStatus.renderStatusString()
            smtpStatus.renderStatusString()
            lastContact.renderStatusString()
            handler.postDelayed(this, 1000)
        }
    }

    fun startUpdateThread() {
        handler.post(runnable)
    }
}

/*
    If logicalStatus is null, then status is undefined i.e. don't know
    true means operational
    false means error
    todo: move time, status, exception, message into constructor to be able to initialize from prefs file
 */
open class StatusInstance(initialValue: String = "") {
    var timeUpdated: Date? = null
    public var renderedValue: MutableLiveData<String> = MutableLiveData<String>(initialValue)
    var logicalStatus: Boolean? = null
    var exception: String? = null
    var message: String? = null

    init {
        renderStatusString()
    }

    fun setStatus(logic: Boolean?, msg: String = "", ex: String? = null) {
        timeUpdated = Date()
        logicalStatus = logic
        message = msg
        exception = ex
        renderStatusString()
    }

    /*
        Todo: render anything < 5 sec ago as "just now" to prevent toggling between 0/1
     */
    fun getTimeDelta() : String {
        val now = Date()
        if(timeUpdated != null) {
            val delta = now.time - timeUpdated!!.time
            return "${delta/1000}s"
        }
        return ""
    }

    open fun renderStatusString() {
        renderedValue.postValue("todo: override me")
    }
}




class IMAPStatusInstance : StatusInstance() {
    override fun renderStatusString() {
        var status = ""

        if(logicalStatus != null) {
            if(logicalStatus == true) status = "Mail checked "
            else status = "failed "
        }
        if(timeUpdated != null) {
            status += "${getTimeDelta()} ago"
        }
        renderedValue.postValue(status)
    }
}

class LastContactStatusInstance : StatusInstance() {
    override fun renderStatusString() {
        if(timeUpdated != null)
            renderedValue.postValue("${getTimeDelta()} ago")
    }
}

class SMTPStatusInstance : StatusInstance() {
    override fun renderStatusString() {
        var status = ""

        if(message != null)
            status += message

        renderedValue.postValue(status)
    }
}