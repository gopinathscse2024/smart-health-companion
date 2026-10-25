package com.smarthealth.companion.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Ble : Screen("ble", "BLE Link", Icons.Default.Bluetooth)
    object Analysis : Screen("analysis", "Analysis", Icons.Default.Analytics)
    object Forecast : Screen("forecast", "Forecast", Icons.Default.Timeline)
    object Respiratory : Screen("respiratory", "Respiratory", Icons.Default.Air)
    object SensorDetails : Screen("sensors", "Sensors", Icons.Default.Sensors)
    object Charts : Screen("charts", "Charts", Icons.Default.ShowChart)
    object Explorer : Screen("explorer", "Explorer", Icons.Default.TableChart)
    object Reports : Screen("reports", "Reports", Icons.Default.CloudDownload)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Privacy : Screen("privacy", "Privacy", Icons.Default.PrivacyTip)

    companion object {
        val bottomNavItems: List<Screen> get() = listOf(
            Dashboard,
            Analysis,
            Forecast,
            Respiratory,
            Charts
        )

        val drawerItems: List<Screen> get() = listOf(
            Dashboard,
            Ble,
            Analysis,
            Forecast,
            Respiratory,
            SensorDetails,
            Charts,
            Explorer,
            Reports,
            Profile,
            Privacy
        )
    }
}
