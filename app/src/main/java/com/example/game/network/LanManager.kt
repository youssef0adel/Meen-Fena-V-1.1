package com.example.game.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.game.model.RoomState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.util.*

object LanManager {
    private const val TAG = "LanManager"
    private const val TCP_PORT = 8888
    private const val UDP_PORT = 8889

    private var serverSocket: ServerSocket? = null
    private var tcpServerJob: Job? = null
    private var udpBroadcastJob: Job? = null
    private var udpDiscoveryJob: Job? = null
    private var clientSocket: Socket? = null
    private var clientReadJob: Job? = null

    // For Host to manage client connections
    private val clientConnections = Collections.synchronizedList(mutableListOf<ClientConnection>())

    // LAN discovery results. Maps HostIP -> HostName
    val discoveredHosts = MutableStateFlow<Map<String, String>>(emptyMap())

    // Ingress TCP packets flow to be handled in the main ViewModel
    val incomingCommands = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64) // Pair<playerId/IP, CommandJSON>

    // Client connection state tracking
    val isClientConnected = MutableStateFlow(false)
    val clientConnectionError = MutableStateFlow<String?>(null)

    // Current local device ID for unique routing
    val localDeviceId: String = UUID.randomUUID().toString().substring(0, 8)

    // Retrieve local private IPv4 address
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val element = interfaces.nextElement()
                val addresses = element.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error getting IP", ex)
        }
        return "127.0.0.1"
    }

    // --- HOST METHODS ---

    fun startHost(hostName: String, roomCode: String) {
        stopAll()
        Log.d(TAG, "Starting Host at Port $TCP_PORT...")
        
        // Start TCP Server Socket
        tcpServerJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(TCP_PORT).apply {
                    reuseAddress = true
                }
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    Log.d(TAG, "Client connected: ${socket.inetAddress.hostAddress}")
                    val connection = ClientConnection(socket)
                    clientConnections.add(connection)
                    connection.startReading { clientIp, command ->
                        CoroutineScope(Dispatchers.IO).launch {
                            incomingCommands.emit(Pair(clientIp, command))
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "TCP Server failed", e)
            }
        }

        // Start UDP Broadcaster
        udpBroadcastJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val broadcastSocket = DatagramSocket().apply {
                    broadcast = true
                }
                val broadcastMsg = "WHO_AMONG_US_HOST|$hostName|${getLocalIpAddress()}|$roomCode"
                val packetData = broadcastMsg.toByteArray()
                
                while (isActive) {
                    try {
                        // Try broadcasting to general address
                        val address = InetAddress.getByName("255.255.255.255")
                        val packet = DatagramPacket(packetData, packetData.size, address, UDP_PORT)
                        broadcastSocket.send(packet)
                    } catch (e: Throwable) {
                        // Fallback to local subnet if general fails
                        try {
                            val subnetAddr = getBroadcastAddress()
                            if (subnetAddr != null) {
                                val packet = DatagramPacket(packetData, packetData.size, subnetAddr, UDP_PORT)
                                broadcastSocket.send(packet)
                            }
                        } catch (inner: Throwable) {
                            Log.e(TAG, "UDP Broadcast failed", inner)
                        }
                    }
                    delay(2000)
                }
                try { broadcastSocket.close() } catch (e: Throwable) {}
            } catch (outer: Throwable) {
                Log.e(TAG, "UDP Socket creation failed", outer)
            }
        }
    }

    private fun getBroadcastAddress(): InetAddress? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            for (interfaceAddress in networkInterface.interfaceAddresses) {
                val broadcast = interfaceAddress.broadcast
                if (broadcast != null) return broadcast
            }
        }
        return null
    }

    fun broadcastStateToClients(state: RoomState) {
        val stateStr = state.toSharedJsonString()
        val envelope = JSONObject().apply {
            put("type", "STATE_UPDATE")
            put("data", stateStr)
        }.toString()

        CoroutineScope(Dispatchers.IO).launch {
            synchronized(clientConnections) {
                val iterator = clientConnections.iterator()
                while (iterator.hasNext()) {
                    val conn = iterator.next()
                    if (!conn.write(envelope)) {
                        Log.d(TAG, "Removing dead connection: ${conn.ip}")
                        conn.close()
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun stopHost() {
        Log.d(TAG, "Stopping host server...")
        stopAll()
    }

    // --- CLIENT METHODS ---

    fun startDiscovery() {
        discoveredHosts.value = emptyMap()
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = CoroutineScope(Dispatchers.IO).launch {
            var ds: DatagramSocket? = null
            try {
                ds = DatagramSocket(UDP_PORT).apply {
                    reuseAddress = true
                }
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    ds.receive(packet)
                    val rxStr = String(packet.data, 0, packet.length).trim()
                    if (rxStr.startsWith("WHO_AMONG_US_HOST")) {
                        val parts = rxStr.split("|")
                        if (parts.size >= 4) {
                            val name = parts[1]
                            val ip = parts[2]
                            val rCode = parts[3]
                            if (ip != getLocalIpAddress()) { // Don't discover self
                                val current = discoveredHosts.value.toMutableMap()
                                current[ip] = "$name|$rCode"
                                discoveredHosts.value = current
                            }
                        } else if (parts.size == 3) {
                            val name = parts[1]
                            val ip = parts[2]
                            if (ip != getLocalIpAddress()) {
                                val current = discoveredHosts.value.toMutableMap()
                                current[ip] = "$name|99999" // Fallback code
                                discoveredHosts.value = current
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Discovery error", e)
            } finally {
                try { ds?.close() } catch (e: Exception) {}
            }
        }
    }

    fun stopDiscovery() {
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = null
    }

    fun connectToHost(hostIp: String, playerName: String, deviceId: String) {
        isClientConnected.value = false
        clientConnectionError.value = null
        
        clientReadJob?.cancel()
        clientReadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Connecting to Host: $hostIp...")
                val socket = withContext(Dispatchers.IO) {
                    Socket().apply {
                        connect(InetSocketAddress(hostIp, TCP_PORT), 4000)
                    }
                }
                clientSocket = socket
                isClientConnected.value = true
                
                // Immediately send JOIN Command
                val joinCommand = JSONObject().apply {
                    put("type", "JOIN")
                    put("playerName", playerName)
                    put("deviceId", deviceId)
                }.toString()
                
                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println(joinCommand)
                
                // Keep reading messages from host
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    incomingCommands.emit(Pair("HOST", line))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client Connection error", e)
                clientConnectionError.value = e.localizedMessage ?: "فشل الاتصال بالغرفة المحلية"
                isClientConnected.value = false
            } finally {
                disconnectFromHost()
            }
        }
    }

    fun sendCommandToHost(jsonString: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val socket = clientSocket
            if (socket != null && isClientConnected.value) {
                try {
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(jsonString)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send command to host", e)
                }
            }
        }
    }

    fun disconnectFromHost() {
        Log.d(TAG, "Disconnecting from host...")
        clientReadJob?.cancel()
        clientReadJob = null
        try { clientSocket?.close() } catch (e: Exception) {}
        clientSocket = null
        isClientConnected.value = false
    }

    // --- LIFECYCLE HELPER ---

    private fun stopAll() {
        tcpServerJob?.cancel()
        udpBroadcastJob?.cancel()
        udpDiscoveryJob?.cancel()
        clientReadJob?.cancel()

        tcpServerJob = null
        udpBroadcastJob = null
        udpDiscoveryJob = null
        clientReadJob = null

        try { serverSocket?.close() } catch (e: Exception) {}
        serverSocket = null

        synchronized(clientConnections) {
            clientConnections.forEach { it.close() }
            clientConnections.clear()
        }

        try { clientSocket?.close() } catch (e: Exception) {}
        clientSocket = null
        isClientConnected.value = false
    }

    // --- CLIENT WRAPPER CLASS ---
    private class ClientConnection(val socket: Socket) {
        val ip: String = socket.inetAddress.hostAddress ?: ""
        private var writer: PrintWriter? = null
        private var reader: BufferedReader? = null
        private var bgJob: Job? = null

        fun startReading(onCommandReceived: (String, String) -> Unit) {
            bgJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    writer = PrintWriter(socket.getOutputStream(), true)
                    reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    while (isActive) {
                        val line = reader?.readLine() ?: break
                        onCommandReceived(ip, line)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Connection lost for $ip")
                } finally {
                    close()
                }
            }
        }

        fun write(msg: String): Boolean {
            return try {
                val pr = writer ?: PrintWriter(socket.getOutputStream(), true).also { writer = it }
                pr.println(msg)
                !pr.checkError()
            } catch (e: Exception) {
                false
            }
        }

        fun close() {
            bgJob?.cancel()
            try { socket.close() } catch (e: Exception) {}
        }
    }
}
