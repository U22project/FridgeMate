package com.example.fridgemate.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CameraScreen(navController: NavController, fridgeViewModel: FridgeViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var ocrResult by remember { mutableStateOf("ここにOCR結果が表示されます") }

    val outputDirectory = getOutputDirectory(context)
    val executor = ContextCompat.getMainExecutor(context)

    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder().build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("OCR結果:", style = MaterialTheme.typography.titleMedium)
            Text(ocrResult, modifier = Modifier.padding(vertical = 8.dp))

            Button(
                onClick = {
                    imageCapture?.let { capture ->
                        val photoFile = File(
                            outputDirectory,
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN)
                                .format(System.currentTimeMillis()) + ".jpg"
                        )

                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        capture.takePicture(
                            outputOptions,
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val savedUri = Uri.fromFile(photoFile)
                                    runTextRecognition(context, savedUri) { resultText ->
                                        ocrResult = resultText
                                        val cleanedList = resultText
                                            .split("\n")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                        fridgeViewModel.addFoodItems(cleanedList)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Toast.makeText(context, "撮影失敗: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("📸 撮影してOCR")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { navController.navigate("fridge") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("戻る")
            }
        }
    }
}

fun runTextRecognition(context: Context, imageUri: Uri, onResult: (String) -> Unit) {
    try {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text)
            }
            .addOnFailureListener {
                onResult("認識に失敗しました: ${it.message}")
            }
    } catch (e: Exception) {
        onResult("画像処理エラー: ${e.message}")
    }
}

fun getOutputDirectory(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, "fridgemate").apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
}
