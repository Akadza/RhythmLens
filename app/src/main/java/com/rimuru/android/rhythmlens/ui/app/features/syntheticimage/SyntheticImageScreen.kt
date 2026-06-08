package com.rimuru.android.rhythmlens.ui.app.features.syntheticimage

import android.net.Uri
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyntheticImageScreen(
    state: SyntheticImageUiState,
    onEvent: (SyntheticImageEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.screen_synthetic_ecg))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onEvent(SyntheticImageEvent.BackClicked)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(RhythmSpacing.Large)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Large)
        ) {
            SyntheticDescriptionCard()

            SyntheticPreviewCard(
                state = state,
                onRetryClick = {
                    onEvent(SyntheticImageEvent.RetryClicked)
                },
                onSaveClick = {
                    onEvent(SyntheticImageEvent.SaveClicked)
                },
                onShareClick = {
                    onEvent(SyntheticImageEvent.ShareClicked)
                }
            )
        }
    }
}

@Composable
private fun SyntheticDescriptionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(RhythmSpacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
        ) {
            Text(
                text = stringResource(R.string.synthetic_ecg_title),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.synthetic_ecg_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyntheticPreviewCard(
    state: SyntheticImageUiState,
    onRetryClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .padding(RhythmSpacing.Medium),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.synthetic_ecg_generating),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
                    ) {
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        Button(onClick = onRetryClick) {
                            Text(text = stringResource(R.string.synthetic_ecg_retry))
                        }
                    }
                }

                state.imageUri != null -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            factory = { context ->
                                ImageView(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    adjustViewBounds = true
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    setBackgroundColor(android.graphics.Color.rgb(255, 252, 250))
                                }
                            },
                            update = { imageView ->
                                imageView.setImageURI(Uri.parse(state.imageUri))
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RhythmSpacing.Small)
                        ) {
                            Button(
                                onClick = onShareClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.synthetic_ecg_share))
                            }

                            FilledTonalButton(
                                onClick = onSaveClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(R.string.synthetic_ecg_save))
                            }
                        }

                        OutlinedButton(
                            onClick = onRetryClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.synthetic_ecg_regenerate))
                        }
                    }
                }
            }
        }
    }
}
