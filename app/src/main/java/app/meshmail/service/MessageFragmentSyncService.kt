package app.meshmail.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.room.Room
import app.meshmail.MeshmailApplication
import app.meshmail.android.Parameters
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.data.MessageFragmentEntity
import app.meshmail.data.protobuf.MessageFragmentRequestOuterClass
import app.meshmail.data.protobuf.MessageShadowOuterClass
import app.meshmail.data.protobuf.ProtocolMessageOuterClass
import app.meshmail.data.protobuf.ProtocolMessageTypeOuterClass
import com.geeksville.mesh.DataPacket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class MessageFragmentSyncService : Service() {
    private var scheduledExecutor: ScheduledExecutorService? = null
    private val meshServiceManager: MeshServiceManager by lazy { (application as MeshmailApplication).meshServiceManager }
    private val database: MeshmailDatabase by lazy {
        (application as MeshmailApplication).database
    }

    override fun onCreate() {
        super.onCreate()

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        scheduledExecutor!!.scheduleWithFixedDelay(
            { runSync() },
            0,
            Parameters.FRAGMENT_SYNC_PERIOD,
            TimeUnit.SECONDS
        )
    }
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "MessageFragment sync service starting", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledExecutor!!.shutdown()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    ////////////////////////

    private fun runSync() {
        // get a list of messages that are shadows
        val shadowMessages: List<MessageEntity> = database.messageDao().getAllShadows()
        // for each shadow message, get a list of missing fragments
        for(message in shadowMessages) {
            var neededFragments = (0 until message.nFragments!!).toMutableSet()
            val fragments: List<MessageFragmentEntity> = database.messageFragmentDao().getAllFragmentsOfMessage(message.fingerprint)
            for(fragment in fragments) {
                neededFragments.remove(fragment.m)
            }
            // send a fragment request for the first missing one
            if(neededFragments.size > 0) {
                // this should always run outside some weird race condition
                // todo: factor this into a class that handles sending messages from a queue with a small delay set by parameter
                var pbProtocolMessage = ProtocolMessageOuterClass.ProtocolMessage.newBuilder()
                pbProtocolMessage.pmtype = ProtocolMessageTypeOuterClass.ProtocolMessageType.FRAGMENT_REQUEST
                var pbMessageFragmentRequest = MessageFragmentRequestOuterClass.MessageFragmentRequest.newBuilder()
                pbMessageFragmentRequest.m = neededFragments.first()
                pbMessageFragmentRequest.fingerprint = message.fingerprint
                pbProtocolMessage.messageFragmentRequest = pbMessageFragmentRequest.build()
                var pbProtocolMessage_bytes: ByteArray = pbProtocolMessage.build().toByteArray()

                meshServiceManager.enqueueForSending(pbProtocolMessage_bytes)
//                val dp = DataPacket(to=DataPacket.ID_BROADCAST,
//                    pbProtocolMessage_bytes,
//                    dataType= Parameters.MESHMAIL_PORT)
//                try {
//                    (application as MeshmailApplication).meshService?.send(dp)
//                } catch(e: Exception) {
//                    Log.e("sendMessage", "Message failed to send", e)
//                }
            }

        }
    }

}

