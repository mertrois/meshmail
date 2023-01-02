package app.meshmail.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
    ViewMessageFragment contains m of n byte arrays which are pieces of the protobuf.
 */
@Entity(tableName = "message_fragments")
data class MessageFragmentEntity(
    @PrimaryKey(autoGenerate = true) var id: Int? = null,
    var fingerprint: String = "",
    var m: Int = 0,
    var n: Int = 0,
    var data: ByteArray = byteArrayOf(),
)