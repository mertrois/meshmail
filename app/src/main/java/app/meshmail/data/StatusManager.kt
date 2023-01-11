package app.meshmail.data

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import java.util.Date

class StatusManager() {

    var imapStatus: IMAPStatusInstance = IMAPStatusInstance()

    val handler = Handler()
    private val runnable = object: Runnable {
        override fun run() {
            imapStatus.renderStatusString()
            handler.postDelayed(this, 1000)
        }
    }

    fun startUpdateThread() {
        handler.post(runnable)
    }
}

open class StatusInstance(initialValue: String = "") {
    protected var timeUpdated: Date? = null
    public var renderedValue: MutableLiveData<String> = MutableLiveData<String>(initialValue)
    protected var logicalStatus: Boolean? = null
    protected var exception: String? = null
    protected var message: String? = null

    init {
        renderStatusString()
    }

    public fun setStatus(logic: Boolean, msg: String = "", ex: String? = null) {
        timeUpdated = Date()
        logicalStatus = logic
        message = msg
        exception = ex
        renderStatusString()
    }

    open fun renderStatusString() {
        renderedValue.postValue("todo: override me")
    }
}

class IMAPStatusInstance : StatusInstance() {
    override fun renderStatusString() {
        var status = ""
        val now = Date()
        if(logicalStatus != null) {
            if(logicalStatus == true) status = "Succesfully checked "
            else status = "failed "
        }
        if(timeUpdated != null) {
            val delta = now.time - timeUpdated!!.time
            status += "${delta/1000}s ago"
        }
        renderedValue.postValue(status)
    }
}