package com.example.applock.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.applock.data.AppDatabase
import com.example.applock.data.LockedApp
import kotlinx.coroutines.launch

private data class InstalledAppInfo(val packageName: String, val label: String)

@Composable
fun AppListScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()

    val lockedPackages by db.lockedAppDao().observeAll()
        .collectAsState(initial = emptyList())
    val lockedSet = remember(lockedPackages) { lockedPackages.map { it.packageName }.toSet() }

    val installedApps = remember {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // user-installed apps only
            .map { InstalledAppInfo(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("اختر التطبيقات") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(installedApps) { app ->
                val isLocked = lockedSet.contains(app.packageName)
                ListItem(
                    headlineContent = { Text(app.label) },
                    supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
                    trailingContent = {
                        Switch(
                            checked = isLocked,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    if (checked) {
                                        db.lockedAppDao().insert(LockedApp(app.packageName, app.label))
                                    } else {
                                        db.lockedAppDao().delete(LockedApp(app.packageName, app.label))
                                    }
                                }
                            }
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
