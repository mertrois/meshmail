package app.meshmail.ui

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.meshmail.MeshmailApplication
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity

class ClientMessagesListViewModel(context: Application) : ViewModel() {
    val database: MeshmailDatabase = (context as MeshmailApplication).database
    val messagesList: LiveData<List<MessageEntity>> = (context as MeshmailApplication).database.messageDao().getInboxMessagesLive()
}

// need a special factory because the ViewModel above needs access to the database, which requires application
// context
class ClientMessagesListViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ClientMessagesListViewModel::class.java)) {
            return ClientMessagesListViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}