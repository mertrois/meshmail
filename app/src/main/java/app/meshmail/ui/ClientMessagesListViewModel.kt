package app.meshmail.ui

import android.app.Application
import androidx.lifecycle.*
import app.meshmail.MeshmailApplication
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity

class ClientMessagesListViewModel(context: Application) : ViewModel() {
    val app: Application = context
    val database: MeshmailDatabase = (context as MeshmailApplication).database

    private val _currentFolder = MutableLiveData<String>("INBOX")

    //private val currentFolder: LiveData<String> = _currentFolder

    val messagesList: LiveData<List<MessageEntity>> = Transformations.switchMap(_currentFolder) { folder ->
        database.messageDao().getMessagesByFolderLive(folder)
    }

    fun setCurrentFolder(folder: String) {
        _currentFolder.value = folder
    }


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