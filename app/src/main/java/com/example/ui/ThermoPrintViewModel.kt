package com.example.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryRepository
import com.example.data.local.PrintHistoryEntity
import com.example.data.model.ReceiptData
import com.example.printer.ConnectionState
import com.example.printer.ConnectionType
import com.example.printer.DiscoveredDevice
import com.example.printer.EscPosBuilder
import com.example.printer.FloydSteinbergDitherer
import com.example.printer.PrinterConnectionManager
import com.example.printer.SystemPrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class UiMessage(
    val text: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class ThermoPrintViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val historyRepo = HistoryRepository(db.printHistoryDao())
    val connectionManager = PrinterConnectionManager(application)

    // History Flow from Room DB
    val historyList: StateFlow<List<PrintHistoryEntity>> = historyRepo.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Theme Mode
    private val _themeMode = MutableStateFlow(AppThemeMode.LIGHT)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Receipt Form Fields
    private val _clinicName = MutableStateFlow("Central Medical Centre")
    val clinicName: StateFlow<String> = _clinicName.asStateFlow()

    private val _headerText = MutableStateFlow("123 Health Ave.\nTel: 555-0198")
    val headerText: StateFlow<String> = _headerText.asStateFlow()

    private val _rbsValue = MutableStateFlow("")
    val rbsValue: StateFlow<String> = _rbsValue.asStateFlow()

    private val _hba1cValue = MutableStateFlow("")
    val hba1cValue: StateFlow<String> = _hba1cValue.asStateFlow()

    private val _bpSystolic = MutableStateFlow("")
    val bpSystolic: StateFlow<String> = _bpSystolic.asStateFlow()

    private val _bpDiastolic = MutableStateFlow("")
    val bpDiastolic: StateFlow<String> = _bpDiastolic.asStateFlow()

    // Live Date & Time
    private val _liveDateTime = MutableStateFlow(Pair("", ""))
    val liveDateTime: StateFlow<Pair<String, String>> = _liveDateTime.asStateFlow()

    // Logo & Floyd-Steinberg preview
    private val _logoBitmap = MutableStateFlow<Bitmap?>(null)
    val logoBitmap: StateFlow<Bitmap?> = _logoBitmap.asStateFlow()

    private val _ditheredPreview = MutableStateFlow<Bitmap?>(null)
    val ditheredPreview: StateFlow<Bitmap?> = _ditheredPreview.asStateFlow()

    // WiFi Form Inputs
    private val _wifiIp = MutableStateFlow("192.168.1.100")
    val wifiIp: StateFlow<String> = _wifiIp.asStateFlow()

    private val _wifiPort = MutableStateFlow("9100")
    val wifiPort: StateFlow<String> = _wifiPort.asStateFlow()

    // Search & Filter States
    private val _bluetoothSearchQuery = MutableStateFlow("")
    val bluetoothSearchQuery: StateFlow<String> = _bluetoothSearchQuery.asStateFlow()

    private val _discoveredWifiPrinters = MutableStateFlow<List<String>>(listOf("192.168.1.100", "192.168.0.100", "192.168.1.200"))
    val discoveredWifiPrinters: StateFlow<List<String>> = _discoveredWifiPrinters.asStateFlow()

    private val _isScanningWifi = MutableStateFlow(false)
    val isScanningWifi: StateFlow<Boolean> = _isScanningWifi.asStateFlow()

    // UI Message Toast
    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    // Active Print In Progress
    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    // Connection manager state proxies
    val connectionState = connectionManager.connectionState
    val debugLogs = connectionManager.debugLogs
    val discoveredDevices = connectionManager.discoveredDevices
    val isScanning = connectionManager.isScanning

    init {
        startLiveClock()
        // Set default sample medical logo
        loadPresetLogo()
    }

    private fun startLiveClock() {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (isActive) {
                val now = Date()
                _liveDateTime.value = Pair(dateFormat.format(now), timeFormat.format(now))
                delay(1000)
            }
        }
    }

    fun toggleTheme() {
        _themeMode.value = when (_themeMode.value) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
        }
    }

    fun setTheme(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun updateClinicName(name: String) { _clinicName.value = name }
    fun updateHeaderText(header: String) { _headerText.value = header }
    fun updateRbs(value: String) { _rbsValue.value = value }
    fun updateHba1c(value: String) { _hba1cValue.value = value }
    fun updateBpSystolic(sys: String) { _bpSystolic.value = sys }
    fun updateBpDiastolic(dia: String) { _bpDiastolic.value = dia }
    fun updateWifiIp(ip: String) { _wifiIp.value = ip }
    fun updateWifiPort(port: String) { _wifiPort.value = port }
    fun updateBluetoothSearchQuery(query: String) { _bluetoothSearchQuery.value = query }

    fun selectWifiPrinter(ip: String) {
        _wifiIp.value = ip
        connectWifi(ip, _wifiPort.value)
    }

    fun showMessage(msg: String, isError: Boolean = false) {
        _uiMessage.value = UiMessage(msg, isError)
        viewModelScope.launch {
            delay(5000)
            if (_uiMessage.value?.text == msg) {
                _uiMessage.value = null
            }
        }
    }

    fun dismissMessage() {
        _uiMessage.value = null
    }

    // ==========================================
    // LOGO PROCESSING
    // ==========================================

    fun setLogoFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, uri)
                _logoBitmap.value = bitmap
                _ditheredPreview.value = FloydSteinbergDitherer.createDitheredPreview(bitmap)
                connectionManager.log("Custom logo loaded and dithered.")
                withContext(Dispatchers.Main) {
                    showMessage("Logo uploaded & dithered successfully")
                }
            } catch (e: Exception) {
                connectionManager.log("Failed to load logo image: ${e.message}")
                withContext(Dispatchers.Main) {
                    showMessage("Could not load image: ${e.message}", true)
                }
            }
        }
    }

    fun loadPresetLogo() {
        viewModelScope.launch(Dispatchers.Default) {
            // Generate clean medical emblem bitmap
            val bmp = Bitmap.createBitmap(160, 100, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }

            // Draw medical cross
            canvas.drawRect(70f, 20f, 90f, 80f, paint)
            canvas.drawRect(40f, 40f, 120f, 60f, paint)

            // Outer ring
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawCircle(80f, 50f, 42f, paint)

            _logoBitmap.value = bmp
            _ditheredPreview.value = FloydSteinbergDitherer.createDitheredPreview(bmp)
        }
    }

    fun clearLogo() {
        _logoBitmap.value = null
        _ditheredPreview.value = null
        connectionManager.log("Logo removed.")
        showMessage("Logo removed")
    }

    // ==========================================
    // PRINTER ACTIONS
    // ==========================================

    fun refreshBluetoothDevices() {
        connectionManager.refreshPairedDevices()
    }

    fun startBluetoothScan() {
        connectionManager.startBleScan()
    }

    fun connectDevice(device: DiscoveredDevice) {
        viewModelScope.launch {
            val raw = device.rawDevice
            if (raw == null) {
                showMessage("Device not available for connection", true)
                return@launch
            }

            val success = if (device.type == ConnectionType.BLUETOOTH_CLASSIC) {
                connectionManager.connectBluetoothClassic(raw)
            } else {
                connectionManager.connectBluetoothBle(raw)
            }

            if (success) {
                showMessage("Connected to ${device.name}")
            } else {
                showMessage("Failed to connect to ${device.name}", true)
            }
        }
    }

    fun connectWifi(ip: String, portStr: String) {
        viewModelScope.launch {
            val port = portStr.toIntOrNull() ?: 9100
            val success = connectionManager.connectWifiPrinter(ip, port)
            if (success) {
                showMessage("Connected to WiFi Printer ($ip:$port)")
            } else {
                showMessage("Failed to connect to WiFi Printer", true)
            }
        }
    }

    fun scanSubnet() {
        viewModelScope.launch {
            val port = _wifiPort.value.toIntOrNull() ?: 9100
            _isScanningWifi.value = true
            showMessage("Scanning local Wi-Fi network for port $port printers...")
            try {
                val found = connectionManager.scanLocalSubnetForPrinters(port) { ip ->
                    _wifiIp.value = ip
                    val current = _discoveredWifiPrinters.value.toMutableList()
                    if (!current.contains(ip)) {
                        current.add(0, ip)
                        _discoveredWifiPrinters.value = current
                    }
                }
                if (found.isNotEmpty()) {
                    _wifiIp.value = found.first()
                    val merged = (found + _discoveredWifiPrinters.value).distinct()
                    _discoveredWifiPrinters.value = merged
                    showMessage("Found ${found.size} printer(s)! Selected: ${found.first()}")
                } else {
                    showMessage("Subnet scan finished. No active port $port printers responded.", true)
                }
            } catch (e: Exception) {
                showMessage("Scan error: ${e.message}", true)
            } finally {
                _isScanningWifi.value = false
            }
        }
    }

    fun disconnectPrinter() {
        connectionManager.disconnect()
        showMessage("Disconnected from printer")
    }

    fun printCurrentReceipt() {
        val currentClinic = _clinicName.value.trim()
        if (currentClinic.isEmpty()) {
            showMessage("Clinic name is required", true)
            return
        }

        val rbs = _rbsValue.value.trim()
        val hba1c = _hba1cValue.value.trim()
        val bpSys = _bpSystolic.value.trim()
        val bpDia = _bpDiastolic.value.trim()

        if (rbs.isEmpty() && hba1c.isEmpty() && bpSys.isEmpty() && bpDia.isEmpty()) {
            showMessage("Please enter at least one test value", true)
            return
        }

        val connState = connectionState.value
        if (connState !is ConnectionState.Connected) {
            showMessage("Please connect a printer first (Bluetooth or Wi-Fi)", true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isPrinting.value = true
            connectionManager.log("Generating ESC/POS receipt commands...")

            try {
                val (dateStr, timeStr) = _liveDateTime.value
                val receiptData = ReceiptData(
                    clinicName = currentClinic,
                    headerText = _headerText.value,
                    rbsValue = rbs.ifEmpty { null },
                    hba1cValue = hba1c.ifEmpty { null },
                    bpSystolic = bpSys.ifEmpty { null },
                    bpDiastolic = bpDia.ifEmpty { null },
                    dateFormatted = dateStr,
                    timeFormatted = timeStr,
                    logoBitmap = _logoBitmap.value
                )

                // Dither logo if present
                val logoRaster = _logoBitmap.value?.let {
                    FloydSteinbergDitherer.processBitmapToEscPosRaster(it)
                }

                val payload = EscPosBuilder.buildReceiptPayload(receiptData, logoRaster)
                val result = connectionManager.sendPrintData(payload)

                if (result.isSuccess) {
                    // Save into Room DB
                    val historyRecord = PrintHistoryEntity(
                        clinicName = currentClinic,
                        headerText = _headerText.value,
                        rbsValue = rbs.ifEmpty { null },
                        hba1cValue = hba1c.ifEmpty { null },
                        bpSystolic = bpSys.ifEmpty { null },
                        bpDiastolic = bpDia.ifEmpty { null },
                        dateTimeFormatted = "$dateStr $timeStr",
                        connectionType = connState.type.name,
                        printerName = connState.deviceName
                    )
                    historyRepo.insertRecord(historyRecord)

                    withContext(Dispatchers.Main) {
                        showMessage("Receipt successfully sent to printer!")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showMessage("Print error: ${result.exceptionOrNull()?.message}", true)
                    }
                }
            } catch (e: Exception) {
                connectionManager.log("Print failure: ${e.message}")
                withContext(Dispatchers.Main) {
                    showMessage("Print error: ${e.message}", true)
                }
            } finally {
                _isPrinting.value = false
            }
        }
    }

    fun printSelfTest() {
        val connState = connectionState.value
        if (connState !is ConnectionState.Connected) {
            showMessage("Connect a printer first to run test print", true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isPrinting.value = true
            try {
                val payload = EscPosBuilder.buildSelfTestPayload()
                val result = connectionManager.sendPrintData(payload)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        showMessage("Diagnostic self-test printed!")
                    } else {
                        showMessage("Test print failed: ${result.exceptionOrNull()?.message}", true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("Test print error: ${e.message}", true)
                }
            } finally {
                _isPrinting.value = false
            }
        }
    }

    fun printViaSystemDialog() {
        val (dateStr, timeStr) = _liveDateTime.value
        val receiptData = ReceiptData(
            clinicName = _clinicName.value.trim().ifEmpty { "Clinic" },
            headerText = _headerText.value,
            rbsValue = _rbsValue.value.ifEmpty { null },
            hba1cValue = _hba1cValue.value.ifEmpty { null },
            bpSystolic = _bpSystolic.value.ifEmpty { null },
            bpDiastolic = _bpDiastolic.value.ifEmpty { null },
            dateFormatted = dateStr,
            timeFormatted = timeStr,
            logoBitmap = _logoBitmap.value
        )
        SystemPrintHelper.printReceipt(getApplication(), receiptData)
    }

    // ==========================================
    // HISTORY ACTIONS
    // ==========================================

    fun loadHistoryItem(item: PrintHistoryEntity) {
        _clinicName.value = item.clinicName
        _headerText.value = item.headerText
        _rbsValue.value = item.rbsValue ?: ""
        _hba1cValue.value = item.hba1cValue ?: ""
        _bpSystolic.value = item.bpSystolic ?: ""
        _bpDiastolic.value = item.bpDiastolic ?: ""
        showMessage("Loaded test record from ${item.dateTimeFormatted}")
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            historyRepo.deleteRecord(id)
            showMessage("History record deleted")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepo.clearAll()
            showMessage("All history cleared")
        }
    }

    fun getReceiptDataForPreview(): ReceiptData {
        val (dateStr, timeStr) = _liveDateTime.value
        return ReceiptData(
            clinicName = _clinicName.value,
            headerText = _headerText.value,
            rbsValue = _rbsValue.value,
            hba1cValue = _hba1cValue.value,
            bpSystolic = _bpSystolic.value,
            bpDiastolic = _bpDiastolic.value,
            dateFormatted = dateStr,
            timeFormatted = timeStr,
            logoBitmap = _logoBitmap.value
        )
    }
}
