package com.example.applock.ui

import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.applock.data.AppDatabase
import com.example.applock.data.Attempt
import com.example.applock.service.LockAccessibilityService
import com.example.applock.util.FrontCameraCapture
import com.example.applock.util.PinStore
import com.example.applock.util.TelegramNotifier
import com.example.applock.util.WrongAttemptTracker
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LockOverlayActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
        private const val MAX_WRONG_ATTEMPTS = 2
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

        val initialAttempts = WrongAttemptTracker.getCount(applicationContext, targetPackage)

        setContent {
            MaterialTheme {
                LockScreenContent(
                    appLabel = appLabel,
                    initialAttempts = initialAttempts,
                    maxAttempts = MAX_WRONG_ATTEMPTS,
                    onUnlocked = {
                        WrongAttemptTracker.reset(applicationContext, targetPackage)
                        LockAccessibilityService.instance?.markUnlocked(targetPackage)
                        finish()
                    },
                    onWrongPin = { enteredPin ->
                        val attemptCount = WrongAttemptTracker.increment(applicationContext, targetPackage)
                        logWrongAttempt(targetPackage, appLabel, enteredPin)
                        attemptCount
                    },
                    onLockout = {
                        LockAccessibilityService.instance?.selfDisable()
                        finish()
                    },
                    onCancel = {
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
                val bitmap = photoPath?.let { BitmapFactory.decodeFile(it) }
                TelegramNotifier.sendIntruderAlert(enteredPin, bitmap)
            }
        }
    }
}

@Composable
private fun LockScreenContent(
    appLabel: String,
    initialAttempts: Int,
    maxAttempts: Int,
    onUnlocked: () -> Unit,
    onWrongPin: (String) -> Int,
    onLockout: () -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val entranceScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.85f,
        animationSpec = tween(durationMillis = 280),
        label = "entranceScale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "entranceAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .graphicsLayer {
                    scaleX = entranceScale
                    scaleY = entranceScale
                    alpha = entranceAlpha
                }
        ) {
            Text("🔒", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(16.dp))
            Text(
                "$appLabel مقفول",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) { pin = it; error = false } },
                label = { Text("ادخل الرمز") },
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = shakeOffset.value.dp)
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
                        val attemptCount = onWrongPin(pin)
                        error = true
                        pin = ""
                        coroutineScope.launch {
                            shakeOffset.animateTo(10f, tween(50))
                            shakeOffset.animateTo(-10f, tween(50))
                            shakeOffset.animateTo(6f, tween(50))
                            shakeOffset.animateTo(0f, tween(50))
                        }
                        if (attemptCount >= maxAttempts) {
                            onLockout()
                        }
                    }
                }) { Text("فتح") }
            }
        }
    }
}
