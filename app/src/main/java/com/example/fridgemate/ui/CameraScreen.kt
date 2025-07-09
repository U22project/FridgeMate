package com.example.fridgemate.ui

import android.content.Context
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.fridgemate.api.ImageUploader
import com.google.accompanist.permissions.*
import com.example.fridgemate.api.TextFilterApi

private const val OCR_DEFAULT_RESULT = "ここにOCR結果が表示されます"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(navController: NavController, fridgeViewModel: FridgeViewModel = viewModel()) {
    // カメラパーミッションの状態を管理
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // 初回表示時にパーミッションリクエスト
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // パーミッション未許可時はリクエストUIを表示
    if (!cameraPermissionState.status.isGranted) {
        PermissionRequestUI { cameraPermissionState.launchPermissionRequest() }
        return
    }

    // 必要なコンテキストやライフサイクル、View等を取得
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var ocrResult by remember { mutableStateOf(OCR_DEFAULT_RESULT) }
    val outputDirectory = getOutputDirectory(context)
    val executor = ContextCompat.getMainExecutor(context)

    // カメラのセットアップ
    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        imageCapture = ImageCapture.Builder().build()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
    }

    // メインUI表示
    CameraScreenContent(
        previewView = previewView,
        ocrResult = ocrResult,
        onCaptureClick = {
            imageCapture?.let { capture ->
                // 撮影・OCR処理
                takePictureAndProcess(
                    capture = capture,
                    outputDirectory = outputDirectory,
                    executor = executor,
                    context = context,
                    onOcrResult = { result -> ocrResult = result },
                    fridgeViewModel = fridgeViewModel
                )
            }
        },
        onBackClick = { navController.navigate("fridge") }
    )
}

@Composable
private fun PermissionRequestUI(onRequest: () -> Unit) {
    // パーミッション未許可時の案内UI
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("カメラの使用を許可してください")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequest) {
            Text("パーミッションを許可する")
        }
    }
}

@Composable
private fun CameraScreenContent(
    previewView: PreviewView,
    ocrResult: String,
    onCaptureClick: () -> Unit,
    onBackClick: () -> Unit
) {
    // カメラプレビューとOCR結果、ボタンUI
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
                onClick = onCaptureClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("📸 撮影してOCR")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("戻る")
            }
        }
    }
}

private fun takePictureAndProcess(
    capture: ImageCapture,
    outputDirectory: File,
    executor: java.util.concurrent.Executor,
    context: Context,
    onOcrResult: (String) -> Unit,
    fridgeViewModel: FridgeViewModel
) {
    // 撮影画像のファイル名を生成
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN).format(System.currentTimeMillis()) + ".jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    // 画像を撮影し、保存後にOCR・API処理
    capture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // サーバーに画像を送信しOCR結果を取得
                ImageUploader.sendImageToServer(photoFile) { resultText ->
                    if (resultText == null) {
                        onOcrResult("OCR結果の取得に失敗しました")
                        return@sendImageToServer
                    }
                    onOcrResult("整形中...")
                    // OCR結果を整形APIでフィルタ
                    TextFilterApi.filterTextFromOcr(resultText) { filteredText ->
                        onOcrResult(filteredText)
                        // 改行ごとに分割し、ViewModelに登録
                        val cleanedList = filteredText
                            .split("\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        fridgeViewModel.addFoodItems(cleanedList)
                    }
                }
                Toast.makeText(context, "撮影成功: ${photoFile.name}", Toast.LENGTH_SHORT).show()
            }
            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "撮影失敗: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

// 画像保存用ディレクトリを取得
fun getOutputDirectory(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, "fridgemate").apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
}