package com.rimuru.android.rhythmlens.ui.app.features.syntheticimage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimuru.android.rhythmlens.R
import java.io.File
import java.io.InputStream

@Composable
fun SyntheticImageRoute(
    onNavigateBack: () -> Unit,
    viewModel: SyntheticImageViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SyntheticImageEffect.NavigateBack -> {
                    onNavigateBack()
                }

                is SyntheticImageEffect.SaveImage -> {
                    runCatching {
                        saveImageToGallery(context, effect.imageUri)
                    }.onSuccess {
                        Toast.makeText(context, R.string.synthetic_ecg_saved, Toast.LENGTH_SHORT).show()
                    }.onFailure { throwable ->
                        Toast.makeText(
                            context,
                            throwable.message ?: context.getString(R.string.synthetic_ecg_save_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                is SyntheticImageEffect.ShareImage -> {
                    runCatching {
                        val uri = saveImageToGallery(context, effect.imageUri)
                        shareImage(context, uri)
                    }.onFailure { throwable ->
                        Toast.makeText(
                            context,
                            throwable.message ?: context.getString(R.string.synthetic_ecg_share_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    SyntheticImageScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}

private fun saveImageToGallery(context: Context, imageUri: String): Uri {
    val resolver = context.contentResolver
    val fileName = "RhythmLens_${System.currentTimeMillis()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RhythmLens")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val targetUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IllegalStateException(context.getString(R.string.synthetic_ecg_save_failed))

    try {
        openImageInputStream(context, Uri.parse(imageUri)).use { input ->
            requireNotNull(input) { context.getString(R.string.synthetic_ecg_save_failed) }
            resolver.openOutputStream(targetUri).use { output ->
                requireNotNull(output) { context.getString(R.string.synthetic_ecg_save_failed) }
                input.copyTo(output)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val readyValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(targetUri, readyValues, null, null)
        }

        return targetUri
    } catch (throwable: Throwable) {
        resolver.delete(targetUri, null, null)
        throw throwable
    }
}

private fun openImageInputStream(context: Context, uri: Uri): InputStream? {
    return if (uri.scheme == "file") {
        uri.path?.let { path -> File(path).inputStream() }
    } else {
        context.contentResolver.openInputStream(uri)
    }
}

private fun shareImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.synthetic_ecg_share)
        )
    )
}
