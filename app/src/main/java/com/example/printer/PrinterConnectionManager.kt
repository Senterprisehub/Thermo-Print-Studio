package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Connecting(val deviceName: String, val type: ConnectionType) : ConnectionState()
    data class Connected(val deviceName: String, val address: String, val type: ConnectionType) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

enum class ConnectionType {
    BLUETOOTH_CLASSIC,
    BLUETOOTH_LE,
    WIFI_NETWORK
}

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val type: ConnectionType,
    val isBonded: Boolean = false,
    val rawDevice: BluetoothDevice? = null
)

class PrinterConnectionManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _debugLogs = MutableStateFlow<List<String>>(emptyList())
    val debugLogs: StateFlow<List<String>> = _debugLogs.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Sockets & GATT handles
    private var classicSocket: BluetoothSocket? = null
    private var bleGatt: BluetoothGatt? = null
    private var bleWriteChar: BluetoothGattCharacteristic? = null
    private var wifiSocket: Socket? = null

    // UUIDs
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val BLE_TARGET_SERVICES = listOf(
        UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455"), // IS1678 UART
        UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"), // Generic UART
        UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb")  // Microchip / Wechat UART
    )

    private val BLE_EXACT_WRITE_CHARS = listOf(
        UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3"), // IS1678 TX/RX
        UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"), // FFE1
        UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
    )

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    fun log(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$time] $msg"
        _debugLogs.value = (_debugLogs.value + entry).takeLast(100)
    }

    fun clearLogs() {
        _debugLogs.value = emptyList()
    }

    // ==========================================
    // 1. BLUETOOTH SCANNING & DISCOVERY
    // ==========================================

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            log("Bluetooth is disabled or not available")
            return
        }

        try {
            val paired = bluetoothAdapter.bondedDevices?.map { device ->
                DiscoveredDevice(
                    name = device.name ?: "Unknown Device",
                    address = device.address,
                    type = ConnectionType.BLUETOOTH_CLASSIC,
                    isBonded = true,
                    rawDevice = device
                )
            } ?: emptyList()

            _discoveredDevices.value = paired
            log("Loaded ${paired.size} paired Bluetooth device(s)")
        } catch (e: Exception) {
            log("Error reading paired devices: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            log("Bluetooth not available for scanning")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            log("BLE Scanner unavailable")
            return
        }

        _isScanning.value = true
        log("Started scanning for Bluetooth & BLE printers...")

        val currentList = _discoveredDevices.value.toMutableList()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    val name = device.name
                    val address = device.address
                    if (!name.isNullOrBlank() && currentList.none { it.address == address }) {
                        val newDev = DiscoveredDevice(
                            name = name,
                            address = address,
                            type = ConnectionType.BLUETOOTH_LE,
                            isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
                            rawDevice = device
                        )
                        currentList.add(newDev)
                        _discoveredDevices.value = currentList.toList()
                        log("Discovered: $name ($address)")
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                log("BLE Scan failed with code: $errorCode")
                _isScanning.value = false
            }
        }

        try {
            scanner.startScan(scanCallback)
            scope.launch {
                delay(12000)
                try {
                    scanner.stopScan(scanCallback)
                } catch (e: Exception) {}
                _isScanning.value = false
                log("Bluetooth scan completed.")
            }
        } catch (e: Exception) {
            log("Scan launch error: ${e.message}")
            _isScanning.value = false
        }
    }

    // ==========================================
    // 2. CONNECT BLUETOOTH (CLASSIC & BLE)
    // ==========================================

    @SuppressLint("MissingPermission")
    suspend fun connectBluetoothClassic(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        val deviceName = device.name ?: "BT Printer"
        _connectionState.value = ConnectionState.Connecting(deviceName, ConnectionType.BLUETOOTH_CLASSIC)
        log("Connecting to $deviceName (SPP ${device.address})...")

        try {
            bluetoothAdapter?.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            classicSocket = socket
            _connectionState.value = ConnectionState.Connected(deviceName, device.address, ConnectionType.BLUETOOTH_CLASSIC)
            log("Connected to $deviceName via Bluetooth SPP!")
            return@withContext true
        } catch (e: Exception) {
            log("Bluetooth SPP connection failed: ${e.message}")
            _connectionState.value = ConnectionState.Error("Failed to connect SPP: ${e.message}")
            return@withContext false
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectBluetoothBle(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        val deviceName = device.name ?: "BLE Printer"
        _connectionState.value = ConnectionState.Connecting(deviceName, ConnectionType.BLUETOOTH_LE)
        log("Connecting GATT to $deviceName (${device.address})...")

        val connectionDeferred = CompletableDeferred<Boolean>()

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    log("GATT connected. Discovering services...")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    log("GATT disconnected.")
                    if (_connectionState.value is ConnectionState.Connected) {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                    if (!connectionDeferred.isCompleted) {
                        connectionDeferred.complete(false)
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    log("Services discovered. Searching for print characteristic...")
                    var targetChar: BluetoothGattCharacteristic? = null

                    for (service in gatt.services) {
                        for (characteristic in service.characteristics) {
                            val props = characteristic.properties
                            val isWritable = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                                    (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0

                            if (isWritable) {
                                if (BLE_EXACT_WRITE_CHARS.contains(characteristic.uuid)) {
                                    targetChar = characteristic
                                    log("Exact thermal print characteristic matched: ${characteristic.uuid}")
                                    break
                                }
                                if (targetChar == null) {
                                    targetChar = characteristic
                                    log("Fallback print characteristic selected: ${characteristic.uuid}")
                                }
                            }
                        }
                        if (targetChar != null && BLE_EXACT_WRITE_CHARS.contains(targetChar.uuid)) {
                            break
                        }
                    }

                    if (targetChar != null) {
                        bleGatt = gatt
                        bleWriteChar = targetChar
                        _connectionState.value = ConnectionState.Connected(deviceName, device.address, ConnectionType.BLUETOOTH_LE)
                        log("BLE Printer Ready! Target characteristic: ${targetChar.uuid}")
                        if (!connectionDeferred.isCompleted) connectionDeferred.complete(true)
                    } else {
                        log("No writable characteristic found on BLE device.")
                        gatt.disconnect()
                        if (!connectionDeferred.isCompleted) connectionDeferred.complete(false)
                    }
                } else {
                    log("Service discovery failed with status $status")
                    if (!connectionDeferred.isCompleted) connectionDeferred.complete(false)
                }
            }
        }

        try {
            val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
            bleGatt = gatt

            val success = withTimeoutOrNull(10000) { connectionDeferred.await() } ?: false
            if (!success) {
                _connectionState.value = ConnectionState.Error("BLE connection timed out.")
                disconnect()
            }
            return@withContext success
        } catch (e: Exception) {
            log("BLE Connect error: ${e.message}")
            _connectionState.value = ConnectionState.Error("BLE Error: ${e.message}")
            return@withContext false
        }
    }

    // ==========================================
    // 3. WIFI / NETWORK PRINTER CONNECTIVITY
    // ==========================================

    suspend fun connectWifiPrinter(ipAddress: String, port: Int = 9100): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        val trimmedIp = ipAddress.trim()
        if (trimmedIp.isBlank()) {
            _connectionState.value = ConnectionState.Error("Please enter a valid IP address")
            return@withContext false
        }

        val targetName = "WiFi Printer ($trimmedIp:$port)"
        _connectionState.value = ConnectionState.Connecting(targetName, ConnectionType.WIFI_NETWORK)
        log("Connecting to WiFi printer at $trimmedIp:$port...")

        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(trimmedIp, port), 4000)
            socket.soTimeout = 8000
            wifiSocket = socket

            _connectionState.value = ConnectionState.Connected(targetName, "$trimmedIp:$port", ConnectionType.WIFI_NETWORK)
            log("Successfully connected to WiFi ESC/POS Printer at $trimmedIp:$port!")
            return@withContext true
        } catch (e: Exception) {
            log("WiFi Printer connection failed: ${e.message}")
            _connectionState.value = ConnectionState.Error("WiFi connection error: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Scans local subnet for printers responding on standard port 9100.
     */
    suspend fun scanLocalSubnetForPrinters(port: Int = 9100, onFound: (String) -> Unit): List<String> = withContext(Dispatchers.IO) {
        val foundIps = mutableListOf<String>()
        log("Starting subnet scan for Port $port printers...")

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt == 0) {
                log("Not connected to Wi-Fi. Subnet scan skipped.")
                return@withContext foundIps
            }

            val baseIp = Formatter.formatIpAddress(ipInt).substringBeforeLast(".")
            log("Scanning subnet $baseIp.1 to $baseIp.254 on port $port...")

            val jobs = mutableListOf<Job>()
            for (i in 1..254) {
                val host = "$baseIp.$i"
                val job = launch {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(host, port), 600)
                        socket.close()
                        synchronized(foundIps) {
                            foundIps.add(host)
                        }
                        log("Found active printer on $host:$port")
                        withContext(Dispatchers.Main) {
                            onFound(host)
                        }
                    } catch (ignored: Exception) {}
                }
                jobs.add(job)
                if (i % 30 == 0) delay(100)
            }
            jobs.forEach { it.join() }
            log("Subnet scan finished. Found ${foundIps.size} printer(s).")
        } catch (e: Exception) {
            log("Subnet scan error: ${e.message}")
        }
        return@withContext foundIps
    }

    // ==========================================
    // 4. PRINT DATA TRANSMISSION (CHUNKED & PACED)
    // ==========================================

    @SuppressLint("MissingPermission")
    suspend fun sendPrintData(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val state = _connectionState.value
        if (state !is ConnectionState.Connected) {
            return@withContext Result.failure(IllegalStateException("No printer connected."))
        }

        log("Transmitting ${data.size} bytes of ESC/POS print payload to ${state.deviceName}...")

        try {
            when (state.type) {
                ConnectionType.BLUETOOTH_CLASSIC -> {
                    val socket = classicSocket ?: return@withContext Result.failure(IllegalStateException("Classic socket is null"))
                    val out: OutputStream = socket.outputStream
                    val chunkSize = 64
                    var offset = 0
                    while (offset < data.size) {
                        val len = minOf(chunkSize, data.size - offset)
                        out.write(data, offset, len)
                        out.flush()
                        offset += len
                        delay(20)
                    }
                    log("Classic Bluetooth transmission completed successfully.")
                }

                ConnectionType.BLUETOOTH_LE -> {
                    val gatt = bleGatt ?: return@withContext Result.failure(IllegalStateException("BLE GATT is null"))
                    val characteristic = bleWriteChar ?: return@withContext Result.failure(IllegalStateException("BLE characteristic is null"))

                    val chunkSize = 20 // Ultra stable 20-byte chunks for BLE thermal printers
                    var offset = 0
                    while (offset < data.size) {
                        val len = minOf(chunkSize, data.size - offset)
                        val chunk = data.copyOfRange(offset, offset + len)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(characteristic, chunk, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                        } else {
                            @Suppress("DEPRECATION")
                            characteristic.value = chunk
                            @Suppress("DEPRECATION")
                            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                            @Suppress("DEPRECATION")
                            gatt.writeCharacteristic(characteristic)
                        }
                        offset += len
                        delay(35) // 35ms pacing prevents printer buffer overflow
                    }
                    log("BLE transmission completed successfully.")
                }

                ConnectionType.WIFI_NETWORK -> {
                    val socket = wifiSocket ?: return@withContext Result.failure(IllegalStateException("WiFi socket is null"))
                    val out = socket.getOutputStream()
                    out.write(data)
                    out.flush()
                    log("WiFi Network transmission completed successfully.")
                }
            }
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            log("Transmission error: ${e.message}")
            disconnect()
            return@withContext Result.failure(e)
        }
    }

    // ==========================================
    // 5. DISCONNECT
    // ==========================================

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            classicSocket?.close()
        } catch (e: Exception) {}
        classicSocket = null

        try {
            bleGatt?.disconnect()
            bleGatt?.close()
        } catch (e: Exception) {}
        bleGatt = null
        bleWriteChar = null

        try {
            wifiSocket?.close()
        } catch (e: Exception) {}
        wifiSocket = null

        _connectionState.value = ConnectionState.Disconnected
        log("Printer disconnected.")
    }
}
