package com.example.applock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.applock.data.AppDatabase
import com.example.applock.data.Attempt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntruderLogScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val attempts by db.attemptDao().observeAll().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سجل المحاولات الفاشلة") },
                actions = {
                    TextButton(onClick = { scope.launch { db.attemptDao().clearAll() } }) {
                        Text("مسح الكل")
                    }
                }
            )
        }
    ) { padding ->
        if (attempts.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("مفيش محاولات فاشلة لحد دلوقتي")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(attempts) { attempt ->
                    AttemptRow(attempt, dateFormat) {
                        scope.launch { db.attemptDao().delete(attempt.id) }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AttemptRow(attempt: Attempt, dateFormat: SimpleDateFormat, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        if (attempt.photoPath != null) {
            AsyncImage(
                model = attempt.photoPath,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(attempt.appLabel, style = MaterialTheme.typography.titleMedium)
            Text("الرمز المكتوب: ${attempt.enteredPin}", style = MaterialTheme.typography.bodyMedium)
            Text(dateFormat.format(Date(attempt.timestampMillis)), style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "حذف")
        }
    }
}
