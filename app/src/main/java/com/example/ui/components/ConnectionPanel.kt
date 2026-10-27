package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printer.ConnectionState
import com.example.printer.ConnectionType
import com.example.printer.DiscoveredDevice
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDisconnected

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectionPanel(
    connectionState: ConnectionState,
    discoveredDevices: List<DiscoveredDevice>,
    isScanning: Boolean,
    wifiIp: String,
    wifiPort: String,
    debugLogs: List<String>,
    bluetoothSearchQuery: String = "",
    discoveredWifiPrinters: List<String> = emptyList(),
    isScanningWifi: Boolean = false,
    onUpdateWifiIp: (String) -> Unit,
    onUpdateWifiPort: (String) -> Unit,
    onUpdateBluetoothSearchQuery: (String) -> Unit = {},
    onRefreshPaired: () -> Unit,
    onStartBleScan: () -> Unit,
    onConnectDevice: (DiscoveredDevice) -> Unit,
    onConnectWifi: (String, String) -> Unit,
    onScanSubnet: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showLogs by remember { mutableStateOf(false) }

    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> StatusConnected
            is ConnectionState.Connecting -> StatusConnecting
            else -> StatusDisconnected
        },
        label = "status_color"
    )

    // Filter bluetooth devices by search query
    val filteredBluetoothDevices = remember(discoveredDevices, bluetoothSearchQuery) {
        if (bluetoothSearchQuery.isBlank()) {
            discoveredDevices
        } else {
            val q = bluetoothSearchQuery.trim().lowercase()
            discoveredDevices.filter {
                it.name.lowercase().contains(q) || it.address.lowercase().contains(q)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_panel_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ==========================================
            // CONNECTED PRINTER STATUS BANNER
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_printer_status_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                                .border(2.dp, statusColor.copy(alpha = 0.4f), CircleShape)
                                .testTag("connection_status_dot")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val titleText = when (connectionState) {
                                is ConnectionState.Connected -> "Connected: ${connectionState.deviceName}"
                                is ConnectionState.Connecting -> "Connecting to ${connectionState.deviceName}..."
                                is ConnectionState.Error -> "Connection Error"
                                ConnectionState.Disconnected -> "No Printer Connected"
                            }
                            Text(
                                text = titleText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val subtitleText = when (connectionState) {
                                is ConnectionState.Connected -> {
                                    val typeBadge = when (connectionState.type) {
                                        ConnectionType.BLUETOOTH_CLASSIC -> "Bluetooth SPP (Classic)"
                                        ConnectionType.BLUETOOTH_LE -> "Bluetooth BLE"
                                        ConnectionType.WIFI_NETWORK -> "Wi-Fi Network (TCP)"
                                    }
                                    "$typeBadge • ${connectionState.address}"
                                }
                                is ConnectionState.Connecting -> "Establishing communication channel..."
                                is ConnectionState.Error -> connectionState.message
                                ConnectionState.Disconnected -> "Search Bluetooth or Wi-Fi printer below to connect"
                            }
                            Text(
                                text = subtitleText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.Connecting) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier.testTag("disconnect_printer_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.LinkOff,
                                contentDescription = "Disconnect",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Tabs for Bluetooth vs Wi-Fi Search
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bluetooth (SPP / BLE)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_bluetooth")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wi-Fi Network", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_wifi")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // TAB 0: BLUETOOTH PRINTER SEARCH & CONNECT
            // ==========================================
            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Search Filter Field
                    OutlinedTextField(
                        value = bluetoothSearchQuery,
                        onValueChange = onUpdateBluetoothSearchQuery,
                        label = { Text("Search Bluetooth Printers") },
                        placeholder = { Text("Search by name or MAC (e.g., POS, MTP, RPP)...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (bluetoothSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { onUpdateBluetoothSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bt_search_query_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scan & Discovery Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onStartBleScan,
                            enabled = !isScanning,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("scan_ble_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Searching...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = "Scan", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search Nearby", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onRefreshPaired,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("refresh_paired_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Paired", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Results header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filteredBluetoothDevices.isNotEmpty()) {
                                "Found ${filteredBluetoothDevices.size} printer(s)"
                            } else {
                                "Available Devices"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isScanning) {
                            Text(
                                text = "Scanning BLE channels...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredBluetoothDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.BluetoothSearching,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (bluetoothSearchQuery.isNotEmpty())
                                        "No printer matching '$bluetoothSearchQuery'"
                                    else
                                        "No Bluetooth printers in list.\nTap 'Search Nearby' or 'Load Paired' to find printers.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredBluetoothDevices) { device ->
                                    val isCurrent = when (connectionState) {
                                        is ConnectionState.Connected -> connectionState.address == device.address
                                        else -> false
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { onConnectDevice(device) }
                                            .testTag("bt_device_${device.address}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (isCurrent) Icons.Default.CheckCircle else Icons.Default.Print,
                                                    contentDescription = null,
                                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = device.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${device.address} • ${if (device.isBonded) "Paired SPP" else "BLE 4.0+"}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { onConnectDevice(device) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                ),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(if (isCurrent) "Connected" else "Connect", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // TAB 1: WI-FI PRINTER SEARCH & CONNECT
            // ==========================================
            if (selectedTab == 1) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Manual IP and Port Configuration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = wifiIp,
                            onValueChange = onUpdateWifiIp,
                            label = { Text("Printer IP Address") },
                            placeholder = { Text("192.168.1.100") },
                            modifier = Modifier
                                .weight(2f)
                                .testTag("wifi_ip_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = wifiPort,
                            onValueChange = onUpdateWifiPort,
                            label = { Text("Port") },
                            placeholder = { Text("9100") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wifi_port_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Wi-Fi Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onConnectWifi(wifiIp, wifiPort) },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("wifi_connect_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect Wi-Fi", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onScanSubnet,
                            enabled = !isScanningWifi,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("wifi_scan_subnet_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isScanningWifi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Searching...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.WifiFind, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search Wi-Fi Subnet", fontSize = 12.sp)
                            }
                        }
                    }

                    if (isScanningWifi) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Select / Discovered Wi-Fi Printers
                    Text(
                        text = "Quick IP Selection & Discovered Printers",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val allWifiIps = remember(discoveredWifiPrinters, wifiIp) {
                        (listOf("192.168.1.100", "192.168.0.100", "192.168.1.200", "10.0.0.100") + discoveredWifiPrinters).distinct()
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allWifiIps.forEach { ip ->
                            val isSelected = wifiIp == ip
                            AssistChip(
                                onClick = {
                                    onUpdateWifiIp(ip)
                                    onConnectWifi(ip, wifiPort)
                                },
                                label = { Text(ip, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("wifi_chip_$ip")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Debug Logs Console Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogs = !showLogs }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = "Debug",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showLogs) "Hide Debug Console" else "Show Debug Console (${debugLogs.size} logs)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = showLogs) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2F3C))
                        .padding(8.dp)
                        .testTag("debug_console_box")
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(debugLogs) { logLine ->
                            Text(
                                text = logLine,
                                color = Color(0xFFA3E4B7),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
