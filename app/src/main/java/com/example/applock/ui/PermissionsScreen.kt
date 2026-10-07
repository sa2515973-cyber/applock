@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.applock.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsScreen() {
    val context = LocalContext.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored here, re-check happens via button state if desired */ }

    Scaffold(topBar = { TopAppBar(title = { Text("الصلاحيات") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("التطبيق محتاج 3 صلاحيات عشان يشتغل صح:")

            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }, modifier = Modifier.fillMaxWidth()) {
                Text("1) فتح إعدادات Accessibility")
            }

            Button(onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("2) السماح بالظهور فوق التطبيقات")
            }

            Button(onClick = {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("3) إذن الكاميرا")
            }

            Text(
                "بعد ما تفعّل خدمة الـ Accessibility بتاعة AppLock من الإعدادات، " +
                    "رجّع هنا واختار التطبيقات اللي عايز تقفلها.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
