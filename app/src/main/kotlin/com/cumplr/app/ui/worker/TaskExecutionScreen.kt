package com.cumplr.app.ui.worker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.CumplrButton
import com.cumplr.core.ui.component.CumplrButtonVariant
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrAccentInk
import com.cumplr.core.ui.theme.CumplrBorder
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface3
import com.cumplr.core.ui.theme.Spacing
import java.io.File

private val STEP_LABELS = listOf("Foto inicio", "Temporizador", "Observaciones", "Foto final")

@Composable
fun TaskExecutionScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: TaskExecutionViewModel = hiltViewModel(),
) {
    val step              by viewModel.step.collectAsStateWithLifecycle()
    val elapsedSeconds    by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val observations      by viewModel.observations.collectAsStateWithLifecycle()
    val startPhotoBytes   by viewModel.startPhotoBytes.collectAsStateWithLifecycle()
    val endPhotoBytes     by viewModel.endPhotoBytes.collectAsStateWithLifecycle()
    val startPendingBytes by viewModel.startPendingBytes.collectAsStateWithLifecycle()
    val endPendingBytes   by viewModel.endPendingBytes.collectAsStateWithLifecycle()
    val isRestored        by viewModel.isRestored.collectAsStateWithLifecycle()
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is ExecutionUiState.Success) onSuccess()
    }

    val context = LocalContext.current
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { cameraGranted = it }

    LaunchedEffect(Unit) {
        if (!cameraGranted) permLauncher.launch(Manifest.permission.CAMERA)
    }

    // Determine app bar title based on active sub-state
    val appBarTitle = when {
        startPendingBytes != null -> "Confirmar foto de inicio"
        endPendingBytes   != null -> "Confirmar foto final"
        else -> STEP_LABELS[step - 1]
    }

    // Step indicator shows logical step (confirmation is part of step 1 or 4)
    val indicatorStep = when {
        startPendingBytes != null -> 1
        endPendingBytes   != null -> 4
        else -> step
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

        if (uiState is ExecutionUiState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CumplrAccent)
            }
            return@Surface
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            CumplrAppBar(
                title = appBarTitle,
                leadingIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver",
                            tint = CumplrFgMuted,
                        )
                    }
                },
            )

            StepIndicator(currentStep = indicatorStep, totalSteps = 4)

            when {
                // ── Start photo: pending confirmation ────────────────────────
                startPendingBytes != null -> PhotoConfirmStep(
                    photoBytes  = startPendingBytes!!,
                    description = "Asegúrate de que la foto sea clara y muestre el área de trabajo.",
                    onConfirm   = { viewModel.confirmStartPhoto() },
                    onRetake    = { viewModel.retakeStartPhoto() },
                )

                // ── Step 1: take start photo ─────────────────────────────────
                step == 1 -> CameraStep(
                    enabled       = cameraGranted,
                    buttonLabel   = "Tomar foto de inicio",
                    onPhotoCaptured = { viewModel.onStartPhotoTaken(it) },
                )

                // ── Step 2: timer ────────────────────────────────────────────
                step == 2 -> TimerStep(
                    elapsedSeconds    = elapsedSeconds,
                    startPhotoBytes   = startPhotoBytes,
                    isRestored        = isRestored,
                    onContinue        = { viewModel.goToStep3() },
                    onRetakeStartPhoto = { viewModel.retakeStartPhotoFromTimer() },
                )

                // ── Step 3: observations ─────────────────────────────────────
                step == 3 -> ObservationsStep(
                    observations         = observations,
                    onObservationsChange = { viewModel.setObservations(it) },
                    onContinue           = { viewModel.goToStep4() },
                )

                // ── End photo: pending confirmation ──────────────────────────
                endPendingBytes != null -> PhotoConfirmStep(
                    photoBytes  = endPendingBytes!!,
                    description = "Verifica que la foto muestre el trabajo completado.",
                    onConfirm   = { viewModel.confirmEndPhoto() },
                    onRetake    = { viewModel.retakeEndPhoto() },
                )

                // ── Step 4 with confirmed photo: show preview + submit ────────
                step == 4 && endPhotoBytes != null -> SubmitStep(
                    endPhotoBytes = endPhotoBytes!!,
                    uiState       = uiState,
                    onSubmit      = { viewModel.submit() },
                    onRetake      = { viewModel.retakeEndPhoto() },
                )

                // ── Step 4: take end photo ───────────────────────────────────
                else -> CameraStep(
                    enabled       = cameraGranted,
                    buttonLabel   = "Tomar foto de finalización",
                    onPhotoCaptured = { viewModel.onEndPhotoTaken(it) },
                )
            }
        }
    }
}

// ── Step indicator ────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..totalSteps).forEach { i ->
            val completed = i < currentStep
            val active    = i == currentStep
            val color     = if (completed || active) CumplrAccent else CumplrSurface3

            Box(
                modifier = Modifier
                    .size(if (active) 28.dp else 22.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = if (completed) "✓" else "$i",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (completed || active) CumplrAccentInk else CumplrFgMuted,
                )
            }

            if (i < totalSteps) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (completed) CumplrAccent else CumplrSurface3)
                )
            }
        }
    }
}

// ── Camera ────────────────────────────────────────────────────────────────────

@Composable
private fun CameraStep(
    enabled: Boolean,
    buttonLabel: String,
    onPhotoCaptured: (ByteArray) -> Unit,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { ContextCompat.getMainExecutor(context) }
    val scope          = rememberCoroutineScope()
    var imageCapture   by remember { mutableStateOf<ImageCapture?>(null) }
    var isTakingPhoto  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (enabled) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CumplrSurface),
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraFuture.addListener({
                            try {
                                val provider = cameraFuture.get()
                                val preview  = Preview.Builder().build()
                                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                val capture  = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture,
                                )
                            } catch (_: Exception) {}
                        }, executor)
                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            CumplrButton(
                text     = buttonLabel,
                enabled  = !isTakingPhoto,
                onClick  = {
                    val capture = imageCapture ?: return@CumplrButton
                    if (isTakingPhoto) return@CumplrButton
                    isTakingPhoto = true
                    scope.launch {
                        val tmp = try {
                            withContext(Dispatchers.IO) {
                                File.createTempFile("cumplr_", ".jpg", context.cacheDir)
                            }
                        } catch (_: Exception) {
                            isTakingPhoto = false
                            return@launch
                        }
                        capture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(tmp).build(),
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    scope.launch(Dispatchers.IO) {
                                        val bytes = try {
                                            tmp.readBytes().also { tmp.delete() }
                                        } catch (_: Exception) {
                                            tmp.delete()
                                            withContext(Dispatchers.Main) { isTakingPhoto = false }
                                            return@launch
                                        }
                                        withContext(Dispatchers.Main) {
                                            isTakingPhoto = false
                                            onPhotoCaptured(bytes)
                                        }
                                    }
                                }
                                override fun onError(e: ImageCaptureException) {
                                    tmp.delete()
                                    isTakingPhoto = false
                                }
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text      = "Permiso de cámara requerido.\nActívalo en Ajustes del dispositivo.",
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = CumplrFgMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Photo confirmation ────────────────────────────────────────────────────────

@Composable
private fun PhotoConfirmStep(
    photoBytes: ByteArray,
    description: String,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
) {
    val bitmap = remember(photoBytes) {
        BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)?.asImageBitmap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text  = description,
            style = MaterialTheme.typography.bodyMedium,
            color = CumplrFgMuted,
        )

        if (bitmap != null) {
            Image(
                bitmap            = bitmap,
                contentDescription = null,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        CumplrButton(
            text     = "Usar esta foto",
            onClick  = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
        CumplrButton(
            text     = "Retomar foto",
            onClick  = onRetake,
            variant  = CumplrButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Timer ─────────────────────────────────────────────────────────────────────

@Composable
private fun TimerStep(
    elapsedSeconds: Long,
    startPhotoBytes: ByteArray?,
    isRestored: Boolean,
    onContinue: () -> Unit,
    onRetakeStartPhoto: () -> Unit,
) {
    val h  = elapsedSeconds / 3600
    val m  = (elapsedSeconds % 3600) / 60
    val s  = elapsedSeconds % 60
    val ts = "%02d:%02d:%02d".format(h, m, s)

    val startBitmap = remember(startPhotoBytes) {
        startPhotoBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
    ) {
        // Start photo thumbnail + restored banner
        if (startBitmap != null || isRestored) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (startBitmap != null) {
                    Image(
                        bitmap             = startBitmap,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
                Column {
                    Text(
                        text  = "Foto de inicio",
                        style = MaterialTheme.typography.labelSmall,
                        color = CumplrFgMuted,
                    )
                    Text(
                        text  = if (isRestored) "✓ Restaurada" else "✓ Registrada",
                        style = MaterialTheme.typography.labelMedium,
                        color = CumplrAccent,
                    )
                }

                if (isRestored) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text  = "↩ Tarea retomada",
                        style = MaterialTheme.typography.labelSmall,
                        color = CumplrFgMuted,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
        }

        Spacer(Modifier.weight(1f))

        // Timer display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text  = "Tiempo transcurrido",
                style = MaterialTheme.typography.bodyLarge,
                color = CumplrFgMuted,
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text  = ts,
                style = MaterialTheme.typography.displayLarge,
                color = CumplrAccent,
            )
        }

        Spacer(Modifier.weight(1f))

        CumplrButton(
            text     = "Agregar observaciones",
            onClick  = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
        CumplrButton(
            text     = "Cambiar foto de inicio",
            onClick  = onRetakeStartPhoto,
            variant  = CumplrButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Observations ─────────────────────────────────────────────────────────────

@Composable
private fun ObservationsStep(
    observations: String,
    onObservationsChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text  = "Observaciones (opcional)",
            style = MaterialTheme.typography.bodyMedium,
            color = CumplrFgMuted,
        )

        OutlinedTextField(
            value         = observations,
            onValueChange = { if (it.length <= 500) onObservationsChange(it) },
            placeholder   = { Text("Describe lo que encontraste durante la tarea...") },
            minLines      = 5,
            maxLines      = 8,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = CumplrAccent,
                unfocusedBorderColor    = CumplrBorder,
                focusedLabelColor       = CumplrAccent,
                unfocusedLabelColor     = CumplrFgMuted,
                cursorColor             = CumplrAccent,
                focusedTextColor        = CumplrFg,
                unfocusedTextColor      = CumplrFg,
                focusedContainerColor   = CumplrSurface,
                unfocusedContainerColor = CumplrSurface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text  = "${observations.length}/500",
            style = MaterialTheme.typography.bodySmall,
            color = CumplrFgMuted,
        )

        Spacer(Modifier.weight(1f))

        CumplrButton(
            text     = "Tomar foto final",
            onClick  = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Submit (step 4, end photo confirmed) ─────────────────────────────────────

@Composable
private fun SubmitStep(
    endPhotoBytes: ByteArray,
    uiState: ExecutionUiState,
    onSubmit: () -> Unit,
    onRetake: () -> Unit,
) {
    val bitmap = remember(endPhotoBytes) {
        BitmapFactory.decodeByteArray(endPhotoBytes, 0, endPhotoBytes.size)?.asImageBitmap()
    }
    val isUploading = uiState is ExecutionUiState.Uploading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (bitmap != null) {
            Image(
                bitmap             = bitmap,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (isUploading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    CircularProgressIndicator(color = CumplrAccent, modifier = Modifier.size(24.dp))
                    Text(
                        text  = "Subiendo fotos…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CumplrFgMuted,
                    )
                }
            }
        } else {
            CumplrButton(
                text     = "Enviar tarea",
                onClick  = onSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
            CumplrButton(
                text     = "Retomar foto final",
                onClick  = onRetake,
                variant  = CumplrButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val err = (uiState as? ExecutionUiState.Error)?.message
        if (err != null) {
            Text(
                text     = err,
                style    = MaterialTheme.typography.bodySmall,
                color    = CumplrStatusOverdueFg,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
