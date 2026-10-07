package com.example.applock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.applock.util.PinStore

@Composable
fun PinSetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("اعمل رمز القفل الخاص بيك", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) pin = it },
                label = { Text("رمز جديد") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { if (it.length <= 8) confirm = it },
                label = { Text("تأكيد الرمز") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                when {
                    pin.length < 4 -> error = "الرمز لازم يكون 4 أرقام على الأقل"
                    pin != confirm -> error = "الرمزين مش متطابقين"
                    else -> {
                        PinStore.setPin(context, pin)
                        onDone()
                    }
                }
            }) { Text("حفظ") }
        }
    }
}
