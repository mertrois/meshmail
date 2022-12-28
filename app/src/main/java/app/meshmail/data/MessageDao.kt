package app.meshmail.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.reactivex.Flowable

@Dao
interface MessageDao {
    @Query("select * from messages")
    fun getAll(): List<MessageEntity>

    @Query("select * from messages where hasBeenRequested = 0")
    fun getUnrequestedMessages(): List<MessageEntity>

    @Query("select * from messages where hasBeenRequested = 0")
    fun getUnrequestedMessagesFlowable(): Flowable<List<MessageEntity>>

    @Query("select * from messages where isShadow = 0")
    fun getNonShadowMessagesLive(): LiveData<List<MessageEntity>>

    @Query("select * from messages where isShadow = 0 and folder = 'INBOX'")
    fun getInboxMessagesLive(): LiveData<List<MessageEntity>>

    @Query("select * from messages where isShadow = 0 and folder = :folder")
    fun getMessagesByFolderLive(folder: String): LiveData<List<MessageEntity>>

    @Query("select * from messages where isShadow = 1")
    fun getAllShadows(): List<MessageEntity>

    @Query("select * from messages where isShadow = 1 LIMIT :limit")
    fun getShadowsWithLimit(limit: Int=1): List<MessageEntity>

    @Insert
    fun insert(messageEntity: MessageEntity)

    @Query("SELECT * FROM messages WHERE serverId = :serverId LIMIT 1")
    fun getByServerId(serverId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE fingerprint = :fingerprint LIMIT 1")
    fun getByFingerprint(fingerprint: String): MessageEntity?

    @Update
    fun update(messageEntity: MessageEntity)

    @Delete
    fun delete(messageEntity: MessageEntity)

    @Query("delete from messages")
    fun deleteAll()
}
