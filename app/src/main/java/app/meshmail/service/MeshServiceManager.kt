package app.meshmail.service

import app.meshmail.android.Parameters
import com.geeksville.mesh.DataPacket
import com.geeksville.mesh.IMeshService
import java.util.*

class MeshServiceManager {
    private var meshService: IMeshService? = null
    private var packetQ: PriorityQueue<DataPacket> = PriorityQueue<DataPacket>()

    fun serviceConnected(s: IMeshService) {
        meshService = s
    }

    fun serviceDisconnected() {
        meshService = null
    }

    fun send(dp: DataPacket) {
        meshService?.send(dp)
    }

    fun send() {
        meshService?.send(packetQ.remove())
    }

    fun enqueueForSending(data: ByteArray,
                          to: String = DataPacket.ID_BROADCAST,
                          dataType: Int = Parameters.MESHMAIL_PORT) {

        val dp = DataPacket(to, data, dataType)
        packetQ.add(dp)
        send()
    }

}