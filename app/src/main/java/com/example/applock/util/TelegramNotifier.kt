package com.example.applock.util

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object TelegramNotifier {

    private const val BOT_TOKEN = "8619423709:AAESR9Hp8fOJ6kTgGVmxSrcJ_dXH8AtuOYA"
    private const val CHAT_ID = "8416321402"
    private const val TAG = "TelegramNotifier"

    suspend fun sendIntruderAlert(wrongPin: String, photo: Bitmap?) {
        withContext(Dispatchers.IO) {
            try {
                if (photo != null) {
                    sendPhoto(photo, caption = "⚠️ محاولة دخول خاطئة\nPIN المُدخل: $wrongPin")
                } else {
                    sendMessage("⚠️ محاولة دخول خاطئة\nPIN المُدخل: $wrongPin\n(تعذر التقاط صورة)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "فشل إرسال التنبيه", e)
            }
        }
    }

    private fun sendMessage(text: String) {
        val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendMessage")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val body = "chat_id=$CHAT_ID&text=${java.net.URLEncoder.encode(text, "UTF-8")}"
        conn.outputStream.use { it.write(body.toByteArray()) }
        conn.responseCode
        conn.disconnect()
    }

    private fun sendPhoto(photo: Bitmap, caption: String) {
        val boundary = "----AppLockBoundary${System.currentTimeMillis()}"
        val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendPhoto")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

        val stream = ByteArrayOutputStream()
        photo.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val imageBytes = stream.toByteArray()

        conn.outputStream.use { out ->
            fun writeField(name: String, value: String) {
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
                out.write("$value\r\n".toByteArray())
            }
            writeField("chat_id", CHAT_ID)
            writeField("caption", caption)

            out.write("--$boundary\r\n".toByteArray())
            out.write("Content-Disposition: form-data; name=\"photo\"; filename=\"intruder.jpg\"\r\n".toByteArray())
            out.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
            out.write(imageBytes)
            out.write("\r\n--$boundary--\r\n".toByteArray())
        }
        conn.responseCode
        conn.disconnect()
    }
}
