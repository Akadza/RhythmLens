package com.rimuru.android.rhythmlens.ui.app.features.ecgdetail

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rimuru.android.rhythmlens.R

@Composable
fun EcgDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToComparison: (String) -> Unit,
    onNavigateToSyntheticImage: (String) -> Unit,
    viewModel: EcgDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                EcgDetailEffect.NavigateBack -> {
                    onNavigateBack()
                }

                is EcgDetailEffect.NavigateToComparison -> {
                    onNavigateToComparison(effect.ecgId)
                }

                is EcgDetailEffect.NavigateToSyntheticImage -> {
                    onNavigateToSyntheticImage(effect.ecgId)
                }

                is EcgDetailEffect.NavigateToExport -> {
                    // no-op: export is handled as ShareReport for the MVP
                }

                is EcgDetailEffect.ShareReport -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            context.getString(R.string.share_report)
                        )
                    )
                }
            }
        }
    }

    EcgDetailScreen(
        state = state.value,
        onEvent = viewModel::onEvent
    )
}
