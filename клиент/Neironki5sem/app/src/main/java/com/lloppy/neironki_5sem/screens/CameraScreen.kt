package com.lloppy.neironki_5sem.screens

import android.Manifest
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.lloppy.neironki_5sem.presentation.PredictionViewModel
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    intervalMillis: Long = 1_000L,
) {
    val context = LocalContext.current
    val permission = rememberPermissionState(Manifest.permission.CAMERA)
    val viewModel = remember { PredictionViewModel() }
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val processedBytes by viewModel.imageBytes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        if (!permission.status.isGranted) permission.launchPermissionRequest()
    }
    LaunchedEffect(permission.status.isGranted) {
        if (permission.status.isGranted) {
            while (true) {
                imageCapture.value?.takePicture(
                    executor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val buffer: ByteBuffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                            image.close()
                            viewModel.sendImage(bytes)
                        }

                        override fun onError(exception: ImageCaptureException) {}
                    })
                delay(intervalMillis)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    ProcessCameraProvider
                        .getInstance(ctx)
                        .also { future ->
                            future.addListener({
                                val provider = future.get()
                                imageCapture.value = ImageCapture
                                    .Builder()
                                    .build()
                                val preview = androidx.camera.core.Preview
                                    .Builder()
                                    .build()
                                    .also { it.surfaceProvider = surfaceProvider }
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    ctx as ComponentActivity,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture.value
                                )
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Processed image overlay
        processedBytes?.let { bytes ->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .height(400.dp)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(topStart = 20.dp))
            )
        }

        // UI indicators
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            if (isLoading) CircularProgressIndicator()
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                LaunchedEffect(it) { viewModel.clearError() }
            }
        }
    }
}
