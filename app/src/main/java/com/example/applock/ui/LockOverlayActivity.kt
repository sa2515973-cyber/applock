package com.example.applock.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.applock.data.AppDatabase
import com.example.applock.data.Attempt
import com.example.applock.service.LockAccessibilityService
import com.example.applock.util.FrontCameraCapture
import com.example.applock.util.PinStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Full-screen activity shown over a locked app's window. Asks for the
 * owner's PIN. On a wrong entry it silently snaps a front-camera photo
 * and logs the exact PIN that was typed, then clears the field so the
 * person can try again (they are never told the attempt was logged).
 */
class LockOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: run {
            finish(); return
        }
        val appLabel = try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(targetPackage, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            targetPackage
        }

        setContent {
            MaterialTheme {
                LockScreenContent(
                    appLabel = appLabel,
                    onUnlocked = {
                        LockAccessibilityService.instance?.markUnlocked(targetPackage)
                        finish()
                    },
                    onWrongPin = { enteredPin ->
                        logWrongAttempt(targetPackage, appLabel, enteredPin)
                    },
                    onCancel = {
                        // Send the user home instead of back into the locked app.
                        finish()
                    }
                )
            }
        }
    }

    private fun logWrongAttempt(pkg: String, label: String, enteredPin: String) {
        FrontCameraCapture.capture(this, this) { photoPath ->
            lifecycleScope.launch {
                AppDatabase.get(applicationContext).attemptDao().insert(
                    Attempt(
                        packageName = pkg,
                        appLabel = label,
                        enteredPin = enteredPin,
                        photoPath = photoPath,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}

@Composable
private fun LockScreenContent(
    appLabel: String,
    onUnlocked: () -> Unit,
    onWrongPin: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔒", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(16.dp))
            Text("$appLabel مقفول", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) { pin = it; error = false } },
                label = { Text("ادخل الرمز") },
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            if (error) {
                Spacer(Modifier.height(8.dp))
                Text("رمز غلط", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Row {
                OutlinedButton(onClick = onCancel) { Text("رجوع للرئيسية") }
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    if (PinStore.checkPin(context, pin)) {
                        onUnlocked()
                    } else {
                        onWrongPin(pin)
                        error = true
                        pin = ""
                    }
                }) { Text("فتح") }
            }
        }
    }
}
