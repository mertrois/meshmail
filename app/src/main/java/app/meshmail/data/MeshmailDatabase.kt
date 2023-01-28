package app.meshmail.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.meshmail.data.protobuf.MessageOuterClass
import app.meshmail.service.decompressData
import app.meshmail.service.millisToDate

@Database(entities = [MessageEntity::class, MessageFragmentEntity::class], version = 10, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MeshmailDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun messageFragmentDao(): MessageFragmentDao

    companion object {
        fun getDatabase(context: Context): MeshmailDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MeshmailDatabase::class.java,
                "meshmail_database"
            ).fallbackToDestructiveMigration().build()
        }
    }

    fun attemptToReconstituteMessage(message: MessageEntity): Boolean {
        // first get all the fragments of this message.
        if(! message.isShadow) {
            Log.d("MeshmailDatabase","Message is already reconstituted. Not trying")
            return false
        }

        val fragList: List<MessageFragmentEntity> = messageFragmentDao().getAllFragmentsOfMessage(message.fingerprint)
        if(fragList.size < message.nFragments) {    // too few, don't even bother looking for duplicates
            return false
        } else {
            val fragSet: MutableSet<Int> = mutableSetOf()
            for(frag in fragList) {
                fragSet.add(frag.m)
            }

            if(fragSet.size == message.nFragments) {   // now we're sure we have the full set w/ duplicates removed
                Log.d("MeshBroadcastReceiver","reconstituting message and upgrading to non-shadow...")

                var buffer = ByteArray(0)
                var fragSeq = 0                         // help us eliminate duplicates
                for(fragment in fragList) {
                    if(fragSeq == fragment.m) {         // avoid duplicates
                        buffer += fragment.data         // in case of e.g. m=0,0,0,0,1,1,1,1,2,2,2,2 etc...
                        fragSeq++
                    } else {
                        continue
                    }
                }
                val pbMessage: MessageOuterClass.Message
                try {
                    // now we can conjure a protobuf message from the concatenated byte arrays
                    pbMessage = MessageOuterClass.Message.parseFrom(buffer)
                    // update our Message in the DB
                    // message.body = pbMessage.body // old simple method
                    message.body = decompressData(pbMessage.data.toByteArray())
                    message.subject = pbMessage.subject
                    message.serverId = pbMessage.serverId
                    message.recipient = pbMessage.recipient
                    message.sender = pbMessage.sender
                    message.type = pbMessage.type
                } catch(e: Exception) {
                    Log.e("MeshmailDatabase","Error reconstituting fragments to message", e)
                    return false
                }

                /*
                this message has made the hop (relay-client or client-relay) so set this to true to prevent its shadow
                from being rebroadcast to to the other side ad-infinitum. No harm would be done as it would be marked as a duplicate,
                but causes unnecessary traffic.
                 */
                message.hasBeenRequested = true
                message.receivedDate = millisToDate(pbMessage.receivedDate)
                message.isShadow = false // woohoo we are a fully-fledged message now

                if(message.type == "INBOUND") {   // this receiver is running on the client
                    message.folder = "INBOX"        // mark as inbox so it shows up.
                } else if(message.type == "OUTBOUND") {   // this receiver is running on the relay.
                    message.hasBeenSent = false             // just arrived, so hasn't been sent via smtp server yet.
                    message.folder = "OUTBOX"
                }

                messageDao().update(message)
                val result = message.let { msg ->
                    val sb = StringBuilder()
                    sb.appendLine("Received new Message: ${msg.fingerprint}")
                    sb.appendLine("subject: ${msg.subject}")
                    sb.appendLine("body: ${msg.body}")
                    sb.toString()
                }
                Log.d("MeshBroadcastReceiver", result)

                return true
            }

        }
        return false
    }
}
