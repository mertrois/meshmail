package app.meshmail.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
    MessageFragment contains m of n byte arrays which are pieces of the protobuf.
 */
@Entity(tableName = "message_fragments")
data class MessageFragmentEntity(
    @PrimaryKey(autoGenerate = true) var id: Int? = null,
    var fingerprint: String? = null,
    var m: Int? = null,
    var n: Int? = null,
    var data: ByteArray? = null,
)