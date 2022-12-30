package app.meshmail.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageFragmentDao {
    @Query("SELECT * FROM message_fragments")
    fun getAll(): List<MessageFragmentEntity>

    @Query("SELECT * FROM message_fragments WHERE fingerprint = :fingerprint order by m ASC")
    fun getAllFragmentsOfMessage(fingerprint: String): List<MessageFragmentEntity>

    /* this should only ever return one or none */
    @Query("SELECT * FROM message_fragments WHERE fingerprint = :fingerprint AND m = :m")
    fun getMatchingFragments(fingerprint: String, m: Int): List<MessageFragmentEntity>

    @Query("SELECT count(*) FROM message_fragments WHERE fingerprint = :fingerprint")
    fun getNumFragmentsAvailable(fingerprint: String): Int

    @Query("SELECT * from message_fragments where fingerprint = :fingerprint and m = :m limit 1")
    fun getFragmentOfMessage(m: Int, fingerprint: String): MessageFragmentEntity

    @Insert
    fun insert(messageFragmentEntity: MessageFragmentEntity)

    @Query("delete from message_fragments")
    fun deleteAll()

    @Query("delete from message_fragments where fingerprint = :fingerprint")
    fun deleteByFingerprint(fingerprint: String)
}