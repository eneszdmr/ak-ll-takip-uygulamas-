package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.utils.OCRParser
import com.example.utils.ScannedProductInfo
import com.example.utils.TurkishProductKnowledgeBase
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

enum class ScanStep {
  STEP_1_BARCODE_AND_BOX,
  STEP_2_EXPIRY_OCR
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun LiveProductScannerDialog(
  initialStep: ScanStep,
  onProductScanned: (ScannedProductInfo) -> Unit,
  onExpiryDateScanned: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()

  var currentStep by remember { mutableStateOf(initialStep) }
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  var liveDetectedBarcode by remember { mutableStateOf<String?>(null) }
  var liveDetectedOcrText by remember { mutableStateOf("") }
  var liveDetectedExpiry by remember { mutableStateOf<String?>(null) }
  var isResolving by remember { mutableStateOf(false) }
  var statusMessage by remember {
    mutableStateOf(
      if (initialStep == ScanStep.STEP_1_BARCODE_AND_BOX) {
        "Kamerayı ilaç/ürün kutusundaki İTS Karekoda, Barkoda veya Kutu Ön Yüzüne tutun."
      } else {
        "Kamerayı kutu üzerindeki Son Kullanma Tarihine (SKT / EXP) tutun."
      }
    )
  }
  var showManualEntry by remember { mutableStateOf(false) }
  var manualCodeOrText by remember { mutableStateOf("") }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasCameraPermission = granted
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  // Zero-permission Android Photo Picker for scanning from gallery image
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      isResolving = true
      statusMessage = "Seçilen fotoğraf analiz ediliyor..."
      try {
        val image = InputImage.fromFilePath(context, uri)
        val barcodeScanner = BarcodeScanning.getClient()
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        barcodeScanner.process(image)
          .addOnSuccessListener { barcodes ->
            val rawValue = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
            textRecognizer.process(image)
              .addOnSuccessListener { visionText ->
                val ocrText = visionText.text
                scope.launch {
                  if (currentStep == ScanStep.STEP_2_EXPIRY_OCR) {
                    val dateFromOcr = OCRParser.parseExpiryFromOCR(ocrText)
                    val dateFromBarcode = if (rawValue != null) {
                      TurkishProductKnowledgeBase.resolveBarcodeOrITS(rawValue).expiryDate
                    } else null
                    val finalDate = dateFromOcr ?: dateFromBarcode
                    isResolving = false
                    if (finalDate != null) {
                      onExpiryDateScanned(finalDate)
                    } else {
                      statusMessage = "Fotoğrafta geçerli bir SKT/EXP tarihi bulunamadı."
                    }
                  } else {
                    val info = if (rawValue != null) {
                      TurkishProductKnowledgeBase.resolveBarcodeOrITS(rawValue, ocrText)
                    } else if (ocrText.isNotBlank()) {
                      TurkishProductKnowledgeBase.analyzeBoxOcrTextWithInternet(ocrText)
                    } else null

                    isResolving = false
                    if (info != null && (info.name.isNotBlank() || info.gtin != null || info.expiryDate != null)) {
                      onProductScanned(info)
                    } else {
                      statusMessage = "Fotoğraftan barkod veya ürün metni okunamadı."
                    }
                  }
                }
              }
              .addOnFailureListener {
                isResolving = false
                statusMessage = "Metin okuma hatası: ${it.localizedMessage}"
              }
          }
          .addOnFailureListener {
            isResolving = false
            statusMessage = "Barkod okuma hatası: ${it.localizedMessage}"
          }
      } catch (e: Exception) {
        isResolving = false
        statusMessage = "Fotoğraf yüklenemedi: ${e.localizedMessage}"
      }
    }
  }

  // Handle when a live barcode is detected in Step 1
  LaunchedEffect(liveDetectedBarcode) {
    val rawCode = liveDetectedBarcode ?: return@LaunchedEffect
    if (currentStep == ScanStep.STEP_1_BARCODE_AND_BOX && !isResolving) {
      isResolving = true
      statusMessage = "Ürün adı ve ne işe yaradığı internetten aranıyor..."
      val resolved = TurkishProductKnowledgeBase.resolveBarcodeOrITS(rawCode, liveDetectedOcrText)
      isResolving = false
      onProductScanned(resolved)
    }
  }

  // Automatically apply expiry date when detected in Step 2
  LaunchedEffect(liveDetectedExpiry, currentStep) {
    val exp = liveDetectedExpiry ?: return@LaunchedEffect
    if (currentStep == ScanStep.STEP_2_EXPIRY_OCR && !isResolving) {
      onExpiryDateScanned(exp)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Header + Step Switcher
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (currentStep == ScanStep.STEP_1_BARCODE_AND_BOX) {
                  Icons.Default.QrCodeScanner
                } else {
                  Icons.Default.DocumentScanner
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (currentStep == ScanStep.STEP_1_BARCODE_AND_BOX) {
                  "Adım 1: İTS Karekod & Kutu Tanıma"
                } else {
                  "Adım 2: Kutu Üzerinden SKT (OCR)"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
              )
            }
            IconButton(onClick = onDismiss) {
              Icon(Icons.Default.Close, contentDescription = "Kapat")
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Step selector tabs
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = currentStep == ScanStep.STEP_1_BARCODE_AND_BOX,
              onClick = {
                currentStep = ScanStep.STEP_1_BARCODE_AND_BOX
                statusMessage = "Kamerayı İTS Karekoda, Barkoda veya Kutu Ön Yüzüne tutun."
              },
              label = { Text("1. İTS Barkod / Kutu Adı", fontSize = 12.sp) },
              leadingIcon = {
                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = currentStep == ScanStep.STEP_2_EXPIRY_OCR,
              onClick = {
                currentStep = ScanStep.STEP_2_EXPIRY_OCR
                statusMessage = "Kamerayı kutu üzerindeki Son Kullanma Tarihine (SKT / EXP) tutun."
              },
              label = { Text("2. SKT Tarih Okut (OCR)", fontSize = 12.sp) },
              leadingIcon = {
                Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Camera Preview Area
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          if (hasCameraPermission) {
            CameraPreviewWithMLKit(
              onBarcodeDetected = { rawCode ->
                if (liveDetectedBarcode == null) {
                  liveDetectedBarcode = rawCode
                }
              },
              onOcrTextDetected = { text ->
                liveDetectedOcrText = text
                val exp = OCRParser.parseExpiryFromOCR(text)
                if (exp != null) {
                  liveDetectedExpiry = exp
                }
              }
            )

            // Viewfinder guide overlay
            Box(
              modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(170.dp)
                .border(2.dp, Color(0xFF10B981), RoundedCornerShape(14.dp))
            )

            // Live detection status pill at bottom of camera view
            Column(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = statusMessage,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
              if (liveDetectedExpiry != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Algılanan SKT: $liveDetectedExpiry",
                  color = Color(0xFF34D399),
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              } else if (liveDetectedOcrText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Okunan Kutu Metni: ${liveDetectedOcrText.lines().firstOrNull() ?: ""}",
                  color = Color(0xFF93C5FD),
                  fontSize = 11.sp,
                  maxLines = 1
                )
              }
            }
          } else {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Canlı barkod ve kutu taraması için kamera izni gereklidir.",
                color = Color.White,
                fontSize = 14.sp
              )
              Spacer(modifier = Modifier.height(12.dp))
              Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Kamera İzni Ver")
              }
            }
          }

          if (isResolving) {
            CircularProgressIndicator(color = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons for Capturing Current OCR / Gallery / Manual
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (currentStep == ScanStep.STEP_1_BARCODE_AND_BOX) {
            Button(
              onClick = {
                if (liveDetectedOcrText.isNotBlank()) {
                  scope.launch {
                    isResolving = true
                    statusMessage = "Kutu metni ve ürün açıklaması internetten aranıyor..."
                    val info = TurkishProductKnowledgeBase.analyzeBoxOcrTextWithInternet(liveDetectedOcrText)
                    isResolving = false
                    onProductScanned(info)
                  }
                } else {
                  statusMessage = "Henüz kutu üzerinde metin algılanmadı. Kamerayı kutunun ön yüzüne yaklaştırın veya galeriden fotoğraf seçin."
                }
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Kutu Ön Yüzünden Ürün Adı & Açıklamayı Al")
            }
          } else {
            Button(
              onClick = {
                val exp = liveDetectedExpiry ?: OCRParser.parseExpiryFromOCR(liveDetectedOcrText)
                if (exp != null) {
                  onExpiryDateScanned(exp)
                } else {
                  statusMessage = "Henüz geçerli bir tarih algılanmadı (Örn: SKT 08/2027, SON KUL TAR 10.2028 veya doğrudan tarih)."
                }
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (liveDetectedExpiry != null) Color(0xFF059669) else MaterialTheme.colorScheme.primary
              )
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                if (liveDetectedExpiry != null) {
                  "Algılanan Tarihi Kullan ($liveDetectedExpiry)"
                } else {
                  "Kutu Üzerindeki Tarihi Yakala"
                }
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Galeriden Seç", fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = { showManualEntry = !showManualEntry },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Kod / Metin Gir", fontSize = 12.sp)
            }
          }

          if (showManualEntry) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = manualCodeOrText,
                onValueChange = { manualCodeOrText = it },
                label = {
                  Text(
                    if (currentStep == ScanStep.STEP_1_BARCODE_AND_BOX) {
                      "İTS Karekod / Barkod veya Ürün İsmi"
                    } else {
                      "Tarih veya SKT Metni (Örn: SON KUL TAR 08/2027)"
                    },
                    fontSize = 12.sp
                  )
                },
                modifier = Modifier.weight(1f),
                singleLine = true
              )
              Button(
                onClick = {
                  val input = manualCodeOrText.trim()
                  if (input.isNotBlank()) {
                    scope.launch {
                      if (currentStep == ScanStep.STEP_2_EXPIRY_OCR) {
                        val date = OCRParser.parseExpiryFromOCR(input)
                          ?: TurkishProductKnowledgeBase.resolveBarcodeOrITS(input).expiryDate
                        if (date != null) {
                          onExpiryDateScanned(date)
                        } else {
                          statusMessage = "Metinden geçerli bir tarih çıkarılamadı."
                        }
                      } else {
                        isResolving = true
                        statusMessage = "İnternetten ürün bilgisi aranıyor..."
                        val hasLetters = input.any { it.isLetter() } && !input.startsWith("010869") && !input.startsWith("(01)")
                        val info = if (hasLetters) {
                          TurkishProductKnowledgeBase.analyzeBoxOcrTextWithInternet(input)
                        } else {
                          TurkishProductKnowledgeBase.resolveBarcodeOrITS(input)
                        }
                        isResolving = false
                        onProductScanned(info)
                      }
                    }
                  }
                }
              ) {
                Text("Çözümle")
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewWithMLKit(
  onBarcodeDetected: (String) -> Unit,
  onOcrTextDetected: (String) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
  val barcodeScanner = remember { BarcodeScanning.getClient() }
  val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

  DisposableEffect(Unit) {
    onDispose {
      try {
        barcodeScanner.close()
        textRecognizer.close()
        analysisExecutor.shutdown()
      } catch (_: Exception) {
      }
    }
  }

  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
      val previewView = PreviewView(ctx)
      val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
      cameraProviderFuture.addListener({
        try {
          val cameraProvider = cameraProviderFuture.get()
          val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
          }

          val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

          imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
              val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
              )

              barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                  val code = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                  if (!code.isNullOrBlank()) {
                    onBarcodeDetected(code)
                  }
                }
                .addOnCompleteListener {
                  textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                      if (visionText.text.isNotBlank()) {
                        onOcrTextDetected(visionText.text)
                      }
                    }
                    .addOnCompleteListener {
                      imageProxy.close()
                    }
                }
            } else {
              imageProxy.close()
            }
          }

          cameraProvider.unbindAll()
          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis
          )
        } catch (e: Exception) {
          Log.e("CameraPreview", "Camera bind failed", e)
        }
      }, ContextCompat.getMainExecutor(ctx))
      previewView
    }
  )
}
