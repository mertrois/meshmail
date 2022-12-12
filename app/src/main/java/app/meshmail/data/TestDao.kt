package app.meshmail.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TestDao {
    @Query("SELECT * FROM test_entity")
    fun getAll(): List<TestEntity>

    @Insert
    fun insert(myEntity: TestEntity)

    @Query("SELECT * from test_entity where messageId = :id LIMIT 1")
    fun getByMessageId(id: String): TestEntity?
}