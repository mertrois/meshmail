package app.meshmail.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import app.meshmail.MeshmailApplication
import app.meshmail.android.Parameters
import app.meshmail.data.MeshmailDatabase
import app.meshmail.data.MessageEntity
import app.meshmail.data.MessageFragmentEntity
import app.meshmail.data.protobuf.ProtocolMessageOuterClass.ProtocolMessage
import app.meshmail.data.protobuf.ProtocolMessageTypeOuterClass.ProtocolMessageType
import app.meshmail.data.protobuf.MessageFragmentRequestOuterClass.MessageFragmentRequest
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


class MessageFragmentSyncService : Service() {
    private var scheduledExecutor: ScheduledExecutorService? = null
    private val meshServiceManager: MeshServiceManager by lazy { (application as MeshmailApplication).meshServiceManager }
    private val database: MeshmailDatabase by lazy {
        (application as MeshmailApplication).database
    }
    private var future: ScheduledFuture<*>? = null;

    override fun onCreate() {
        super.onCreate()

        // store a reference to the service in the application for other modules to signal with
        (application as MeshmailApplication).fragmentSyncService = this

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        scheduleImmediate()
    }

    private fun scheduleImmediate() {
        future = scheduledExecutor!!.scheduleWithFixedDelay(
            { runSync() },
            0,
            Parameters.FRAGMENT_SYNC_PERIOD,
            TimeUnit.SECONDS
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //Toast.makeText(this, "MessageFragment sync service starting", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledExecutor!!.shutdown()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /*
        Called if an event occurs that makes this worth doing now instead of waiting.
     */
    fun nudge(source: String="") {
        Log.d("MessageFragmentSyncService","nudged by $source")
        future?.cancel(false).also {
            scheduleImmediate()
        }
    }


    /*
        Need to find balance between enqueuing too many at once (but more efficient) vs one at a time, more processor intensive, more fault tolerant.
        todo: implement an increasing delay algorithm on frag sync service... start with short delays and if the job runs and no fragments need to
        be requested, then bump the delay higher until a threshold is hit... so if a packet collision occurs, we're not waiting the full timeout period
        to get a fragment re-requested...
     */
    private fun runSync() {
        // get a list of messages that are shadows
        //val shadowMessages: List<MessageEntity> = database.messageDao().getAllShadows()  // old way of doing it causing excessive collisions
        val shadowMessages: List<MessageEntity> = database.messageDao().getShadowsWithLimit(1)
        // for each shadow message, get a list of missing fragments
        for(message in shadowMessages) {
            var neededFragments = (0 until message.nFragments!!).toMutableSet()
            val fragments: List<MessageFragmentEntity> = database.messageFragmentDao().getAllFragmentsOfMessage(message.fingerprint)
            for(fragment in fragments) {
                neededFragments.remove(fragment.m)
            }
            // send a fragment request for the first missing one
            if(neededFragments.size > 0) {
                // todo: enqueue more than just the first one...
                var pbProtocolMessage = ProtocolMessage.newBuilder()
                pbProtocolMessage.pmtype = ProtocolMessageType.FRAGMENT_REQUEST
                var pbMessageFragmentRequest = MessageFragmentRequest.newBuilder()
                pbMessageFragmentRequest.m = neededFragments.first()
                pbMessageFragmentRequest.fingerprint = message.fingerprint
                pbProtocolMessage.messageFragmentRequest = pbMessageFragmentRequest.build()
                var pbProtocolMessage_bytes: ByteArray = pbProtocolMessage.build().toByteArray()

                meshServiceManager.enqueueForSending(pbProtocolMessage_bytes)
            }
        }
    }

}

