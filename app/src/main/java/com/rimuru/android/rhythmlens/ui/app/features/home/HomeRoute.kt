package com.rimuru.android.rhythmlens.ui.app.features.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

@Composable
fun HomeRoute(
    onNavigateToEcgDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val pendingCameraUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { isSaved ->
        val imageUri = pendingCameraUri.value
        if (isSaved && imageUri != null) {
            viewModel.onEvent(HomeEvent.ImageSelected(imageUri.toString()))
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { imageUri ->
        if (imageUri != null) {
            viewModel.onEvent(HomeEvent.ImageSelected(imageUri.toString()))
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { imageUri ->
        if (imageUri != null) {
            viewModel.onEvent(HomeEvent.ImageSelected(imageUri.toString()))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeEffect.OpenCamera -> {
                    val imageUri = createCameraImageUri(context)
                    pendingCameraUri.value = imageUri
                    cameraLauncher.launch(imageUri)
                }

                HomeEffect.OpenGalleryPicker -> {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                HomeEffect.OpenFilePicker -> {
                    fileLauncher.launch(arrayOf("image/png", "image/jpeg"))
                }

                is HomeEffect.NavigateToEcgDetail -> {
                    onNavigateToEcgDetail(effect.ecgId)
                }
            }
        }
    }

    HomeScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}

private fun createCameraImageUri(context: Context): Uri {
    val imageDir = File(context.cacheDir, "camera_images").apply {
        mkdirs()
    }
    val imageFile = File.createTempFile(
        "ecg_camera_",
        ".jpg",
        imageDir
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}
