package io.github.thomaswcode.notificationvibrator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import io.github.thomaswcode.notificationvibrator.ui.apps.AppsScreen
import io.github.thomaswcode.notificationvibrator.ui.log.ActivityLogScreen
import io.github.thomaswcode.notificationvibrator.ui.patterns.PatternsTab
import io.github.thomaswcode.notificationvibrator.ui.setup.SetupScreen
import io.github.thomaswcode.notificationvibrator.ui.setup.SystemSettings
import io.github.thomaswcode.notificationvibrator.ui.theme.NotificationVibratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NotificationVibratorTheme { MainScreen() }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    APPS("Apps", Icons.Default.Notifications),
    PATTERNS("Patterns", Icons.Default.Star),
    ACTIVITY("Activity", Icons.AutoMirrored.Filled.List),
    SETUP("Setup", Icons.Default.Settings),
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    // Open on Setup until notification access has been granted
    var tab by rememberSaveable {
        mutableStateOf(if (SystemSettings.hasNotificationAccess(context)) Tab.APPS else Tab.SETUP)
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        label = { Text(entry.label) },
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.APPS -> AppsScreen()
                Tab.PATTERNS -> PatternsTab()
                Tab.ACTIVITY -> ActivityLogScreen()
                Tab.SETUP -> SetupScreen()
            }
        }
    }
}
