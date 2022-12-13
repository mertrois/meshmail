package app.meshmail.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_entity")
data class TestEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Long? = null,
    var subject: String? = "",
    var from: String? = "",
    var messageId: String? = "",
    var body: String? = ""
)



