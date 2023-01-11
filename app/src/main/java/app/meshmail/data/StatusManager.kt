package app.meshmail.data

import androidx.lifecycle.MutableLiveData
import app.meshmail.MeshmailApplication
import java.util.Date

class StatusManager(app: MeshmailApplication) {
    var imapStatus: MutableLiveData<String> = MutableLiveData<String>("")
    var imapLastChecked: Date = Date()

    fun setImapStatus(success: Boolean, message: String) {
        //postValue because may be called from a service thread
        imapLastChecked = Date()

        var status: String = ""
        if(success) {
            status = "Mail checked at ${imapLastChecked}"
        } else {
            status = "Check failed at ${imapLastChecked}"
        }
        imapStatus.postValue(status)
    }
}