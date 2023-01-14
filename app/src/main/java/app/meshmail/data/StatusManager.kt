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

    }

    private val runnable = object: Runnable {
        override fun run() {
            imapStatus.renderStatusString()
            lastContact.renderStatusString()
            smtpStatus.renderStatusString()

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
abstract class StatusInstance(initialValue: String = "") {
    var timeUpdated: Date? = null
    public var renderedValue: MutableLiveData<String> = MutableLiveData<String>(initialValue)
    var logicalStatus: Boolean? = null
    var exception: String? = null
    var _message: String? = null

    init {
        renderStatusString()
    }

    fun setStatus(logic: Boolean?, msg: String = "", ex: String? = null) {
        timeUpdated = Date()
        logicalStatus = logic
        _message = msg
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

    abstract fun renderStatusString()
}




class IMAPStatusInstance : StatusInstance() {
    override fun renderStatusString() {
        var status = ""
        if(logicalStatus != null) {
            status = if(logicalStatus == true) "Mail checked " else "failed "
            if(timeUpdated != null) {
                status += "${getTimeDelta()} ago"
            }
        } else {
            status = "idle"
        }

        renderedValue.postValue(status)
    }
}

class LastContactStatusInstance : StatusInstance() {
    override fun renderStatusString() {
        if(timeUpdated != null)
            renderedValue.postValue("${getTimeDelta()} ago")
        else
            renderedValue.postValue("no contact yet")
    }
}

class SMTPStatusInstance : StatusInstance() {
    override fun renderStatusString() {
        var status = if(_message != null) _message else "idle"
        renderedValue.postValue(status)
    }
}

