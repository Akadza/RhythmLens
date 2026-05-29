package com.rimuru.android.rhythmlens.ui.app.features.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ScanUiState,
    onEvent: (ScanEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.screen_scan))
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ScanEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(RhythmSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
        ) {
            Text(
                text = stringResource(R.string.scan_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { onEvent(ScanEvent.CreateTestEcgClicked) },
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (state.isProcessing) {
                        stringResource(R.string.processing)
                    } else {
                        stringResource(R.string.create_test_ecg)
                    }
                )
            }

            FilledTonalButton(
                onClick = { onEvent(ScanEvent.TakePhotoClicked) },
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.take_photo))
            }

            FilledTonalButton(
                onClick = { onEvent(ScanEvent.ChooseFromGalleryClicked) },
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.choose_from_gallery))
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
