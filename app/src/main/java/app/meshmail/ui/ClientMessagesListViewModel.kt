package app.meshmail.ui

import android.app.Application
import androidx.lifecycle.*
import app.meshmail.MeshmailApplication
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import com.geeksville.mesh.data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClientMessagesListViewModel(context: Application) : ViewModel() {
    val app: Application = context
    val database: MeshmailDatabase = (context as MeshmailApplication).database

    private val _currentFolder = MutableLiveData<String>()

    val messagesList: LiveData<List<MessageEntity>> = Transformations.switchMap(_currentFolder) { folder ->
        database.messageDao().getMessagesByFolderLive(folder)
    }

    fun setCurrentFolder(folder: String) {
        _currentFolder.value = folder
    }

    suspend fun markMessageRead(message: MessageEntity, hasBeenRead: Boolean = true) {
        message.hasBeenRead = hasBeenRead
        withContext(Dispatchers.IO) {
            database.messageDao().update(message)
        }
    }

    fun getPosition(message: MessageEntity): Int {
        return messagesList.value?.indexOf(message) ?: -1
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