package app.meshmail.service

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.*

import app.meshmail.android.Parameters
import app.meshmail.data.protobuf.ProtocolMessageOuterClass
import com.geeksville.mesh.DataPacket
import com.geeksville.mesh.IMeshService
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayDeque

class MeshServiceManager(context: Application) {
    //private var application: Application = context
    private var meshService: IMeshService? = null
    private var packetQ: ConcurrentLinkedQueue<SimpleDataPacket> = ConcurrentLinkedQueue<SimpleDataPacket>()
    private var workerRunning = false
    private var clearToSend = true
    private var msSinceLastSend: Long = 0
    /*
        Simplified version is needed because DataPacket adds system time in millis in constructor which
        prevents the queue's contains() method from working correctly and preventing duplicate packets.
        Don't want to modify DataPacket's equals operator
     */
    internal data class SimpleDataPacket(val data: ByteArray, val to: String, val dataType: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SimpleDataPacket

            if (!data.contentEquals(other.data)) return false
            if (to != other.to) return false
            if (dataType != other.dataType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + to.hashCode()
            result = 31 * result + dataType
            return result
        }

    }

    fun serviceConnected(service: IMeshService) {
        meshService = service
    }

    fun serviceDisconnected() {
        meshService = null // this going to null should kill worker thread.
    }

    private fun sendNow(dp: DataPacket) {
        Log.d("MeshServiceManager", "sending packet of ${dp.bytes?.size} bytes")
        meshService?.send(dp)
    }

    private fun sendFromQueue() {
        if(packetQ.isNotEmpty()) {
            val sdp = packetQ.poll()
            val dp = DataPacket(to=sdp.to, bytes=sdp.data, dataType=sdp.dataType)
            sendNow(dp)
        }
    }

    fun queueSize(): Int {
        return packetQ.size
    }

    // tells producers if the queue is welcoming more elements.
    // if it gets too low, the chances of a duplicate are higher.
    fun queueEmpty(): Boolean {
        return packetQ.size <= Parameters.MIN_DESIRED_QUEUE_SIZE
    }

    /*
        this would be a good time to send a packet from the queue, because the other side just sent
     */
    fun nudge() {
        clearToSend = true
        l("send queue nudged")
    }

    /*
    Application interface for adding a packet to the queue
     */
    fun enqueueForSending(data: ByteArray,
                          to: String = DataPacket.ID_BROADCAST,
                          dataType: Int = Parameters.MESHMAIL_PORT) {

        val sdp = SimpleDataPacket(data,to,dataType)

        if(packetQ.contains(sdp)) {
            // if many shadows are dumped at once, can create many duplicate packets that
            // flood the network
            l("ignoring duplicate packet in queue")
        } else {
            packetQ.add(sdp)
            l("added packet to queue")
        }
        if(!workerRunning) {
            startWorker()
        }
    }

    private fun l(msg: String) {
        Log.d("MeshServiceManager", msg)
    }

    private fun startWorker() {
        Thread {
            l("starting worker thread...")
            workerRunning = true
            clearToSend = true
            msSinceLastSend = 0

            while (packetQ.isNotEmpty() && meshService != null) {
                if(clearToSend || msSinceLastSend > Parameters.QUEUE_TIMEOUT_THRESHOLD ) {
                    if(msSinceLastSend > Parameters.QUEUE_TIMEOUT_THRESHOLD) l("queue timeout, sending")
                    sendFromQueue()
                    msSinceLastSend = 0
                    clearToSend = false
                }
                Thread.sleep(Parameters.SEND_QUEUE_WAIT)
                msSinceLastSend += Parameters.SEND_QUEUE_WAIT
            }
            workerRunning = false

        }.start()
    }

}