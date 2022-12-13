package app.meshmail.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageFragmentDao {
    @Query("SELECT * FROM message_fragments")
    fun getAll(): List<MessageFragmentEntity>

    @Query("SELECT * FROM message_fragments WHERE messageId = :messageId")
    fun getAllFragmentsOfMessage(messageId: Int): List<MessageFragmentEntity>

    @Query("SELECT count(*) FROM message_fragments WHERE messageId = :messageId")
    fun getNumFragmentsAvailable(messageId: Int): Int

    @Insert
    fun insert(messageFragmentEntity: MessageFragmentEntity)
}