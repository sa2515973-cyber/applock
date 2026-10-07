package com.example.applock.util

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Silently snaps one photo from the front camera using CameraX and writes
 * it to the app's private files dir. Requires CAMERA permission and a
 * LifecycleOwner (the LockOverlayActivity) to bind to.
 */
object FrontCameraCapture {

    fun capture(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onResult: (path: String?) -> Unit
    ) {
        val dir = File(context.filesDir, "intruder_photos").apply { mkdirs() }
        val name = "intruder_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
        val outFile = File(dir, name)

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val imageCapture = ImageCapture.Builder().build()
                val selector = CameraSelector.DEFAULT_FRONT_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, imageCapture)

                val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            provider.unbindAll()
                            onResult(outFile.absolutePath)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("FrontCameraCapture", "capture failed", exception)
                            provider.unbindAll()
                            onResult(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("FrontCameraCapture", "bind failed", e)
                onResult(null)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
