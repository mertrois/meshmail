package app.meshmail.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import app.meshmail.MeshmailApplication
import app.meshmail.android.Parameters
import app.meshmail.android.Parameters.Companion.FRAG_SYNC_SHADOWS_TO_ANALYZE
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
    private var future: ScheduledFuture<*>? = null
    private var syncRunning = false

    override fun onCreate() {
        super.onCreate()

        // store a reference to the service in the application for other modules to signal with
        (application as MeshmailApplication).fragmentSyncService = this

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        scheduleImmediate()
    }

    private fun scheduleImmediate() {
        future = scheduledExecutor?.scheduleWithFixedDelay(
            { runSync() },
            0,
            Parameters.FRAGMENT_SYNC_PERIOD,
            TimeUnit.SECONDS
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduledExecutor?.shutdown()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /*
        Called if an event occurs that makes this worth doing now instead of waiting.
     */
    fun nudge(source: String="") {
        Log.d("MessageFragmentSyncService","nudged by $source")
        if(syncRunning == true)
            return
        else {
            if(future?.cancel(false) == true) {
                scheduleImmediate()
            }
        }
    }

    private fun l(msg: String) {
        Log.d("MessageFragmentSyncService", msg)
    }
    /*
        Need to find balance between enqueuing too many at once (but more efficient) vs one at a time, more processor intensive, more fault tolerant.
        todo: implement an increasing delay algorithm on frag sync service... start with short delays and if the job runs and no fragments need to
        be requested, then bump the delay higher until a threshold is hit... so if a packet collision occurs, we're not waiting the full timeout period
        to get a fragment re-requested...
     */
    private fun runSync() {
        syncRunning = true
        // if the radio queue already has a bunch of packets, and the queue strips out duplicates
        // don't bother running right now, it's just extra cycles.
        if(! meshServiceManager.queueEmpty()) {
            l("Queue still full (${meshServiceManager.queueSize()}), not adding new fragment requests.")
            syncRunning = false
            return
        }
        // get a list of messages that are shadows
        //val shadowMessages: List<MessageEntity> = database.messageDao().getAllShadows()  // old way of doing it causing excessive collisions
        val shadowMessages: List<MessageEntity> = database.messageDao().getShadowsWithLimit(FRAG_SYNC_SHADOWS_TO_ANALYZE)
        // for each shadow message, get a list of missing fragments
        for(message in shadowMessages) { // breadth first request one frag for each shadow round robin
            val neededFragments = (0 until message.nFragments).toMutableSet()
            val fragments: List<MessageFragmentEntity> = database.messageFragmentDao().getAllFragmentsOfMessage(message.fingerprint)
            for(fragment in fragments) {
                neededFragments.remove(fragment.m)
            }
            // send a fragment request for the first missing one
            var fragsAdded = 0
            if(neededFragments.isNotEmpty()) {
                for(frag in neededFragments) {
                    if(fragsAdded >= Parameters.MAX_FRAGS_AT_ONCE)
                        break
                    val pbProtocolMessage = ProtocolMessage.newBuilder()
                    pbProtocolMessage.pmtype = ProtocolMessageType.FRAGMENT_REQUEST
                    val pbMessageFragmentRequest = MessageFragmentRequest.newBuilder()
                    pbMessageFragmentRequest.m = frag
                    pbMessageFragmentRequest.fingerprint = message.fingerprint
                    pbProtocolMessage.messageFragmentRequest = pbMessageFragmentRequest.build()
                    val pbProtocolMessageBytes: ByteArray = pbProtocolMessage.build().toByteArray()
                    pbMessageFragmentRequest.let {
                        Log.d("MessageFragmentSyncService", "Requesting fragment ${it.m} of ${it.fingerprint}")
                    }
                    meshServiceManager.enqueueForSending(pbProtocolMessageBytes)
                    fragsAdded++
                }
            } else {
                database.attemptToReconstituteMessage(message)
            }
        }
        syncRunning = false
        return
    }

}

