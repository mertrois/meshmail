package app.meshmail.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageFragmentDao {
    @Query("SELECT * FROM message_fragments")
    fun getAll(): List<MessageFragmentEntity>

    @Query("SELECT * FROM message_fragments WHERE fingerprint = :fingerprint")
    fun getAllFragmentsOfMessage(fingerprint: String): List<MessageFragmentEntity>

    @Query("SELECT count(*) FROM message_fragments WHERE fingerprint = :fingerprint")
    fun getNumFragmentsAvailable(fingerprint: String): Int

    @Insert
    fun insert(messageFragmentEntity: MessageFragmentEntity)
}