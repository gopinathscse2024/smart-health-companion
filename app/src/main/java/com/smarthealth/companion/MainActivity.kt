package com.smarthealth.companion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.smarthealth.companion.model.BleConnectionState
import com.smarthealth.companion.model.RealtimeAlertItem
import com.smarthealth.companion.theme.SmartHealthTheme
import com.smarthealth.companion.theme.StatusAlert
import com.smarthealth.companion.theme.StatusNormal
import com.smarthealth.companion.theme.StatusWarning
import com.smarthealth.companion.theme.TealPrimary
import com.smarthealth.companion.ui.analytics.AnalysisCenterScreen
import com.smarthealth.companion.ui.ble.BleConnectionScreen
import com.smarthealth.companion.ui.charts.VisualChartsScreen
import com.smarthealth.companion.ui.dashboard.DashboardScreen
import com.smarthealth.companion.ui.explorer.TableExplorerScreen
import com.smarthealth.companion.ui.forecast.ForecastScreen
import com.smarthealth.companion.ui.navigation.Screen
import com.smarthealth.companion.ui.privacy.PrivacySettingsScreen
import com.smarthealth.companion.ui.profile.ProfileScreen
import com.smarthealth.companion.ui.reports.ReportsScreen
import com.smarthealth.companion.ui.respiratory.RespiratoryScreen
import com.smarthealth.companion.ui.sensor.SensorDetailsScreen
import com.smarthealth.companion.ui.viewmodel.MainViewModel
import com.smarthealth.companion.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        ViewModelFactory((application as SmartHealthApp).repository)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle runtime permission grants gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAppPermissions()

        setContent {
            SmartHealthTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    var currentScreenRoute by remember { mutableStateOf(Screen.Dashboard.route) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val bleState by viewModel.bleConnectionState.collectAsState()
    val activeAlert by viewModel.activeAlert.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Smart Health Companion",
                        style = MaterialTheme.typography.titleLarge,
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Energy Harvesting ESP32-S3 AI Suite",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Screen.drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 13.sp) },
                        selected = currentScreenRoute == screen.route,
                        onClick = {
                            currentScreenRoute = screen.route
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = TealPrimary.copy(alpha = 0.15f),
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val activeScreen = Screen.drawerItems.find { it.route == currentScreenRoute }
                        Text(
                            text = activeScreen?.title ?: "Smart Health AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Quick BLE State Indicator Icon
                        val bleDotColor = when (bleState) {
                            BleConnectionState.NOTIFICATION_READY, BleConnectionState.CONNECTED -> StatusNormal
                            BleConnectionState.SCANNING, BleConnectionState.CONNECTING -> StatusWarning
                            BleConnectionState.ERROR -> StatusAlert
                            else -> Color(0xFF747D8C)
                        }
                        IconButton(onClick = { currentScreenRoute = Screen.Ble.route }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(bleDotColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = "Bluetooth Status",
                                    tint = bleDotColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontSize = 10.sp) },
                            selected = currentScreenRoute == screen.route,
                            onClick = { currentScreenRoute = screen.route },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealPrimary,
                                selectedTextColor = TealPrimary,
                                indicatorColor = TealPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // In-App Real-time Alert Broadcast Banner
                if (activeAlert != null) {
                    RealtimeInAppAlertBanner(
                        alert = activeAlert!!,
                        onDismiss = { viewModel.dismissAlert() }
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreenRoute) {
                        Screen.Dashboard.route -> DashboardScreen(viewModel = viewModel, onNavigate = { currentScreenRoute = it })
                        Screen.Ble.route -> BleConnectionScreen(viewModel = viewModel)
                        Screen.Analysis.route -> AnalysisCenterScreen(viewModel = viewModel)
                        Screen.Forecast.route -> ForecastScreen(viewModel = viewModel)
                        Screen.Respiratory.route -> RespiratoryScreen(viewModel = viewModel)
                        Screen.SensorDetails.route -> SensorDetailsScreen(viewModel = viewModel)
                        Screen.Charts.route -> VisualChartsScreen(viewModel = viewModel)
                        Screen.Explorer.route -> TableExplorerScreen(viewModel = viewModel)
                        Screen.Reports.route -> ReportsScreen(viewModel = viewModel)
                        Screen.Profile.route -> ProfileScreen(viewModel = viewModel)
                        Screen.Privacy.route -> PrivacySettingsScreen(viewModel = viewModel)
                        else -> DashboardScreen(viewModel = viewModel, onNavigate = { currentScreenRoute = it })
                    }
                }
            }
        }
    }
}

@Composable
fun RealtimeInAppAlertBanner(
    alert: RealtimeAlertItem,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = alert.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF7F1D1D),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.message,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF7F1D1D)
            )

            if (alert.guidance.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEE2E2))
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = alert.guidance,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF991B1B),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
