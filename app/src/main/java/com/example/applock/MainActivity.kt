@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.applock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.applock.ui.AppListScreen
import com.example.applock.ui.IntruderLogScreen
import com.example.applock.ui.PermissionsScreen
import com.example.applock.ui.PinSetupScreen
import com.example.applock.util.PinStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppLockRoot()
            }
        }
    }
}

@Composable
fun AppLockRoot() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val startDestination = if (PinStore.isPinSet(context)) "home" else "setup_pin"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("setup_pin") {
            PinSetupScreen(onDone = {
                navController.navigate("home") { popUpTo("setup_pin") { inclusive = true } }
            })
        }
        composable("home") { HomeScreen(navController) }
        composable("app_list") { AppListScreen() }
        composable("intruder_log") { IntruderLogScreen() }
        composable("permissions") { PermissionsScreen() }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("AppLock") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { navController.navigate("permissions") }, modifier = Modifier.fillMaxWidth()) {
                Text("1) تفعيل الصلاحيات")
            }
            Button(onClick = { navController.navigate("app_list") }, modifier = Modifier.fillMaxWidth()) {
                Text("2) اختيار التطبيقات المقفولة")
            }
            Button(onClick = { navController.navigate("intruder_log") }, modifier = Modifier.fillMaxWidth()) {
                Text("3) سجل المحاولات الفاشلة")
            }
        }
    }
}
