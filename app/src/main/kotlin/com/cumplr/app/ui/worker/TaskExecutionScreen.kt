package com.cumplr.app.ui.worker

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cumplr.core.ui.component.CumplrAppBar
import com.cumplr.core.ui.component.CumplrButton
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
    val step           by viewModel.step.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val observations   by viewModel.observations.collectAsStateWithLifecycle()
    val endPhotoBytes  by viewModel.endPhotoBytes.collectAsStateWithLifecycle()
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()

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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            CumplrAppBar(
                title = STEP_LABELS[step - 1],
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

            StepIndicator(currentStep = step, totalSteps = 4)

            when (step) {
                1 -> CameraStep(
                    enabled       = cameraGranted,
                    buttonLabel   = "Tomar foto de inicio",
                    onPhotoCaptured = { viewModel.onStartPhotoTaken(it) },
                )
                2 -> TimerStep(
                    elapsedSeconds = elapsedSeconds,
                    onContinue     = { viewModel.goToStep3() },
                )
                3 -> ObservationsStep(
                    observations   = observations,
                    onObservationsChange = { viewModel.setObservations(it) },
                    onContinue     = { viewModel.goToStep4() },
                )
                4 -> CameraStep(
                    enabled       = cameraGranted,
                    buttonLabel   = if (endPhotoBytes == null) "Tomar foto de finalización" else "Foto tomada ✓",
                    onPhotoCaptured = { viewModel.onEndPhotoTaken(it) },
                    extraContent  = {
                        if (endPhotoBytes != null) {
                            Spacer(Modifier.height(Spacing.lg))
                            val isUploading = uiState is ExecutionUiState.Uploading
                            if (isUploading) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CumplrAccent, modifier = Modifier.size(32.dp))
                                }
                            } else {
                                CumplrButton(
                                    text     = "Enviar tarea",
                                    onClick  = { viewModel.submit() },
                                    enabled  = !isUploading,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            val err = (uiState as? ExecutionUiState.Error)?.message
                            if (err != null) {
                                Spacer(Modifier.height(Spacing.sm))
                                Text(
                                    text     = err,
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = CumplrStatusOverdueFg,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

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
            val color = if (completed || active) CumplrAccent else CumplrSurface3

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

@Composable
private fun CameraStep(
    enabled: Boolean,
    buttonLabel: String,
    onPhotoCaptured: (ByteArray) -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { ContextCompat.getMainExecutor(context) }
    var imageCapture   by remember { mutableStateOf<ImageCapture?>(null) }

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
                        PreviewView(ctx).also { pv ->
                            ProcessCameraProvider.getInstance(ctx).addListener({
                                val provider = ProcessCameraProvider.getInstance(ctx).get()
                                val preview  = Preview.Builder().build().also {
                                    it.setSurfaceProvider(pv.surfaceProvider)
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture
                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        capture,
                                    )
                                } catch (_: Exception) {}
                            }, executor)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            CumplrButton(
                text    = buttonLabel,
                onClick = {
                    val capture = imageCapture ?: return@CumplrButton
                    val tmp = File.createTempFile("cumplr_", ".jpg", context.cacheDir)
                    val opts = ImageCapture.OutputFileOptions.Builder(tmp).build()
                    capture.takePicture(opts, executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onPhotoCaptured(tmp.readBytes().also { tmp.delete() })
                            }
                            override fun onError(e: ImageCaptureException) { tmp.delete() }
                        }
                    )
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

        extraContent?.invoke()
    }
}

@Composable
private fun TimerStep(elapsedSeconds: Long, onContinue: () -> Unit) {
    val h  = elapsedSeconds / 3600
    val m  = (elapsedSeconds % 3600) / 60
    val s  = elapsedSeconds % 60
    val ts = "%02d:%02d:%02d".format(h, m, s)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
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
        Spacer(Modifier.height(Spacing.xxxl))
        CumplrButton(
            text     = "Agregar observaciones",
            onClick  = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

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
            value          = observations,
            onValueChange  = { if (it.length <= 500) onObservationsChange(it) },
            placeholder    = { Text("Describe lo que encontraste durante la tarea...") },
            minLines       = 5,
            maxLines       = 8,
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
