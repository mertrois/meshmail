package app.meshmail.service

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.*

import app.meshmail.android.Parameters
import com.geeksville.mesh.DataPacket
import com.geeksville.mesh.IMeshService
import kotlin.collections.ArrayDeque

class MeshServiceManager(context: Application) {
    private var application: Application = context
    private var meshService: IMeshService? = null
    private var packetQ: ArrayDeque<DataPacket> = ArrayDeque<DataPacket>()
    private var workerRunning = false


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
            sendNow(packetQ.removeFirst())
        }
    }

    /*
    Application interface for adding a packet to the queue
     */
    fun enqueueForSending(data: ByteArray,
                          to: String = DataPacket.ID_BROADCAST,
                          dataType: Int = Parameters.MESHMAIL_PORT) {

        val dp = DataPacket(to, data, dataType)
        packetQ.add(dp)
        if(!workerRunning) {
            startWorker()
        }
    }

    private fun startWorker() {
        Thread {
            Log.d("MeshServiceManager","worker thread started")
            workerRunning = true
            while (packetQ.isNotEmpty() && meshService != null) {
                Log.d("MeshServiceManager","There are ${packetQ.size} packets in the queue")
                sendFromQueue()
                Thread.sleep(Parameters.SEND_QUEUE_WAIT)  // todo: put this in parameters for tuning
            }
            workerRunning = false
        }.start()
    }

}