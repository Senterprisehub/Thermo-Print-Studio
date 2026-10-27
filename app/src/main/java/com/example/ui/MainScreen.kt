package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.printer.ConnectionState
import com.example.printer.ConnectionType
import com.example.ui.components.ConnectionPanel
import com.example.ui.components.HistoryBottomSheet
import com.example.ui.components.LiveReceiptPreview
import com.example.ui.components.LogoPickerSection
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDisconnected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ThermoPrintViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State collections
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val clinicName by viewModel.clinicName.collectAsStateWithLifecycle()
    val headerText by viewModel.headerText.collectAsStateWithLifecycle()
    val rbsValue by viewModel.rbsValue.collectAsStateWithLifecycle()
    val hba1cValue by viewModel.hba1cValue.collectAsStateWithLifecycle()
    val bpSystolic by viewModel.bpSystolic.collectAsStateWithLifecycle()
    val bpDiastolic by viewModel.bpDiastolic.collectAsStateWithLifecycle()
    val liveDateTime by viewModel.liveDateTime.collectAsStateWithLifecycle()
    val logoBitmap by viewModel.logoBitmap.collectAsStateWithLifecycle()
    val ditheredPreview by viewModel.ditheredPreview.collectAsStateWithLifecycle()
    val wifiIp by viewModel.wifiIp.collectAsStateWithLifecycle()
    val wifiPort by viewModel.wifiPort.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val debugLogs by viewModel.debugLogs.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val isPrinting by viewModel.isPrinting.collectAsStateWithLifecycle()
    val bluetoothSearchQuery by viewModel.bluetoothSearchQuery.collectAsStateWithLifecycle()
    val discoveredWifiPrinters by viewModel.discoveredWifiPrinters.collectAsStateWithLifecycle()
    val isScanningWifi by viewModel.isScanningWifi.collectAsStateWithLifecycle()

    var showHistorySheet by remember { mutableStateOf(false) }

    // Bluetooth permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.refreshBluetoothDevices()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                permissionLauncher.launch(needed.toTypedArray())
            } else {
                viewModel.refreshBluetoothDevices()
            }
        } else {
            viewModel.refreshBluetoothDevices()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ThermoPrint Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Live Connected Status Tag
                            val currentConn = connectionState
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (currentConn) {
                                                is ConnectionState.Connected -> StatusConnected
                                                is ConnectionState.Connecting -> StatusConnecting
                                                else -> StatusDisconnected
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (currentConn) {
                                        is ConnectionState.Connected -> "Connected: ${currentConn.deviceName}"
                                        is ConnectionState.Connecting -> "Connecting..."
                                        is ConnectionState.Error -> "Offline"
                                        ConnectionState.Disconnected -> "No printer connected"
                                    },
                                    fontSize = 11.sp,
                                    color = if (currentConn is ConnectionState.Connected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (currentConn is ConnectionState.Connected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Theme Toggle Button (Light / Dark)
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme (Light / Dark)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // History Button with Badge
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("history_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (historyList.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${historyList.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "History",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth > 840.dp
                val scrollState = rememberScrollState()

                if (isWideScreen) {
                    // Dual-pane layout for tablets / landscape
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Left Pane: Form Controls & Connectivity
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Connected Printer Quick Status Bar
                            ConnectedPrinterQuickBar(
                                connectionState = connectionState,
                                onDisconnect = { viewModel.disconnectPrinter() }
                            )

                            ClinicInfoSection(
                                clinicName = clinicName,
                                headerText = headerText,
                                liveDate = liveDateTime.first,
                                liveTime = liveDateTime.second,
                                onUpdateClinic = { viewModel.updateClinicName(it) },
                                onUpdateHeader = { viewModel.updateHeaderText(it) }
                            )

                            MedicalTestsSection(
                                rbsValue = rbsValue,
                                hba1cValue = hba1cValue,
                                bpSystolic = bpSystolic,
                                bpDiastolic = bpDiastolic,
                                onUpdateRbs = { viewModel.updateRbs(it) },
                                onUpdateHba1c = { viewModel.updateHba1c(it) },
                                onUpdateBpSys = { viewModel.updateBpSystolic(it) },
                                onUpdateBpDia = { viewModel.updateBpDiastolic(it) }
                            )

                            LogoPickerSection(
                                logoBitmap = logoBitmap,
                                ditheredBitmap = ditheredPreview,
                                onImageSelected = { viewModel.setLogoFromUri(it) },
                                onLoadPreset = { viewModel.loadPresetLogo() },
                                onClearLogo = { viewModel.clearLogo() }
                            )

                            ConnectionPanel(
                                connectionState = connectionState,
                                discoveredDevices = discoveredDevices,
                                isScanning = isScanning,
                                wifiIp = wifiIp,
                                wifiPort = wifiPort,
                                debugLogs = debugLogs,
                                bluetoothSearchQuery = bluetoothSearchQuery,
                                discoveredWifiPrinters = discoveredWifiPrinters,
                                isScanningWifi = isScanningWifi,
                                onUpdateWifiIp = { viewModel.updateWifiIp(it) },
                                onUpdateWifiPort = { viewModel.updateWifiPort(it) },
                                onUpdateBluetoothSearchQuery = { viewModel.updateBluetoothSearchQuery(it) },
                                onRefreshPaired = { viewModel.refreshBluetoothDevices() },
                                onStartBleScan = { viewModel.startBluetoothScan() },
                                onConnectDevice = { viewModel.connectDevice(it) },
                                onConnectWifi = { ip, port -> viewModel.connectWifi(ip, port) },
                                onScanSubnet = { viewModel.scanSubnet() },
                                onDisconnect = { viewModel.disconnectPrinter() }
                            )

                            PrintActionControls(
                                isPrinting = isPrinting,
                                connectionState = connectionState,
                                onPrint = { viewModel.printCurrentReceipt() },
                                onSelfTest = { viewModel.printSelfTest() },
                                onSystemPrint = { viewModel.printViaSystemDialog() }
                            )
                        }

                        // Right Pane: Live Receipt Preview
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Live Thermal Receipt Preview (58mm)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LiveReceiptPreview(
                                receiptData = viewModel.getReceiptDataForPreview(),
                                ditheredBitmap = ditheredPreview
                            )
                        }
                    }
                } else {
                    // Single-column layout for phones
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                            .widthIn(max = 600.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Connected Printer Quick Status Bar
                        ConnectedPrinterQuickBar(
                            connectionState = connectionState,
                            onDisconnect = { viewModel.disconnectPrinter() }
                        )

                        ClinicInfoSection(
                            clinicName = clinicName,
                            headerText = headerText,
                            liveDate = liveDateTime.first,
                            liveTime = liveDateTime.second,
                            onUpdateClinic = { viewModel.updateClinicName(it) },
                            onUpdateHeader = { viewModel.updateHeaderText(it) }
                        )

                        MedicalTestsSection(
                            rbsValue = rbsValue,
                            hba1cValue = hba1cValue,
                            bpSystolic = bpSystolic,
                            bpDiastolic = bpDiastolic,
                            onUpdateRbs = { viewModel.updateRbs(it) },
                            onUpdateHba1c = { viewModel.updateHba1c(it) },
                            onUpdateBpSys = { viewModel.updateBpSystolic(it) },
                            onUpdateBpDia = { viewModel.updateBpDiastolic(it) }
                        )

                        LogoPickerSection(
                            logoBitmap = logoBitmap,
                            ditheredBitmap = ditheredPreview,
                            onImageSelected = { viewModel.setLogoFromUri(it) },
                            onLoadPreset = { viewModel.loadPresetLogo() },
                            onClearLogo = { viewModel.clearLogo() }
                        )

                        ConnectionPanel(
                            connectionState = connectionState,
                            discoveredDevices = discoveredDevices,
                            isScanning = isScanning,
                            wifiIp = wifiIp,
                            wifiPort = wifiPort,
                            debugLogs = debugLogs,
                            bluetoothSearchQuery = bluetoothSearchQuery,
                            discoveredWifiPrinters = discoveredWifiPrinters,
                            isScanningWifi = isScanningWifi,
                            onUpdateWifiIp = { viewModel.updateWifiIp(it) },
                            onUpdateWifiPort = { viewModel.updateWifiPort(it) },
                            onUpdateBluetoothSearchQuery = { viewModel.updateBluetoothSearchQuery(it) },
                            onRefreshPaired = { viewModel.refreshBluetoothDevices() },
                            onStartBleScan = { viewModel.startBluetoothScan() },
                            onConnectDevice = { viewModel.connectDevice(it) },
                            onConnectWifi = { ip, port -> viewModel.connectWifi(ip, port) },
                            onScanSubnet = { viewModel.scanSubnet() },
                            onDisconnect = { viewModel.disconnectPrinter() }
                        )

                        PrintActionControls(
                            isPrinting = isPrinting,
                            connectionState = connectionState,
                            onPrint = { viewModel.printCurrentReceipt() },
                            onSelfTest = { viewModel.printSelfTest() },
                            onSystemPrint = { viewModel.printViaSystemDialog() }
                        )

                        Text(
                            text = "Live Receipt Preview (58mm Thermal)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        LiveReceiptPreview(
                            receiptData = viewModel.getReceiptDataForPreview(),
                            ditheredBitmap = ditheredPreview
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Animated Message Toast at bottom
            AnimatedVisibility(
                visible = uiMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                uiMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (msg.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .testTag("ui_toast_message")
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }

        // History Bottom Sheet
        if (showHistorySheet) {
            HistoryBottomSheet(
                historyList = historyList,
                onDismiss = { showHistorySheet = false },
                onLoadItem = { viewModel.loadHistoryItem(it) },
                onDeleteItem = { viewModel.deleteHistoryItem(it) },
                onClearAll = { viewModel.clearAllHistory() }
            )
        }
    }
}

@Composable
private fun ConnectedPrinterQuickBar(
    connectionState: ConnectionState,
    onDisconnect: () -> Unit
) {
    if (connectionState is ConnectionState.Connected) {
        val deviceName = connectionState.deviceName
        val typeName = connectionState.type.name.replace('_', ' ')
        val address = connectionState.address
        val isWifi = connectionState.type == ConnectionType.WIFI_NETWORK
        val errorColor = MaterialTheme.colorScheme.error

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connected_printer_indicator_bar"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isWifi) Icons.Default.Wifi else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ACTIVE PRINTER: $deviceName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "$typeName • $address",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                TextButton(
                    onClick = onDisconnect,
                    modifier = Modifier.testTag("quick_disconnect_btn")
                ) {
                    Text("Disconnect", fontSize = 12.sp, color = errorColor)
                }
            }
        }
    }
}

@Composable
private fun ClinicInfoSection(
    clinicName: String,
    headerText: String,
    liveDate: String,
    liveTime: String,
    onUpdateClinic: (String) -> Unit,
    onUpdateHeader: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("clinic_info_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Practice & Clinic Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Live DateTime Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$liveDate $liveTime",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = clinicName,
                onValueChange = onUpdateClinic,
                label = { Text("Clinic / Practice Name") },
                placeholder = { Text("e.g. Central Medical Centre") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clinic_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = headerText,
                onValueChange = onUpdateHeader,
                label = { Text("Receipt Header (Address, Tel, Reg No.)") },
                placeholder = { Text("123 Health Ave.\nTel: 555-0198") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_header_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun MedicalTestsSection(
    rbsValue: String,
    hba1cValue: String,
    bpSystolic: String,
    bpDiastolic: String,
    onUpdateRbs: (String) -> Unit,
    onUpdateHba1c: (String) -> Unit,
    onUpdateBpSys: (String) -> Unit,
    onUpdateBpDia: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("medical_tests_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Medical Diagnostic Tests",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // 1. RBS Input
            OutlinedTextField(
                value = rbsValue,
                onValueChange = onUpdateRbs,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Random Blood Glucose (RBS)")
                    }
                },
                placeholder = { Text("e.g. 118") },
                trailingIcon = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "mg/dL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rbs_value_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // 2. HbA1c Input
            OutlinedTextField(
                value = hba1cValue,
                onValueChange = onUpdateHba1c,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HbA1c Test (%)")
                    }
                },
                placeholder = { Text("e.g. 5.8") },
                trailingIcon = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hba1c_value_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // 3. Blood Pressure Double Input (Systolic / Diastolic)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Blood Pressure (BP)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = bpSystolic,
                        onValueChange = onUpdateBpSys,
                        label = { Text("Systolic") },
                        placeholder = { Text("120") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bp_systolic_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "/",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = bpDiastolic,
                        onValueChange = onUpdateBpDia,
                        label = { Text("Diastolic") },
                        placeholder = { Text("80") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bp_diastolic_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = "mmHg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintActionControls(
    isPrinting: Boolean,
    connectionState: ConnectionState,
    onPrint: () -> Unit,
    onSelfTest: () -> Unit,
    onSystemPrint: () -> Unit
) {
    val isConnected = connectionState is ConnectionState.Connected

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Big Prominent Main Print Button
        Button(
            onClick = onPrint,
            enabled = !isPrinting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("print_receipt_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            if (isPrinting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sending ESC/POS Data...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Print, contentDescription = "Print Receipt", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                val printLabel = when (connectionState) {
                    is ConnectionState.Connected -> "PRINT TO: ${connectionState.deviceName.take(18)}"
                    is ConnectionState.Connecting -> "CONNECTING PRINTER..."
                    else -> "PRINT RECEIPT"
                }
                Text(printLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Secondary Actions (Test Print & System Print / PDF)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSelfTest,
                enabled = !isPrinting && isConnected,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("self_test_print_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Print", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onSystemPrint,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("system_print_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PDF / Mopria", fontSize = 12.sp)
            }
        }
    }
}
