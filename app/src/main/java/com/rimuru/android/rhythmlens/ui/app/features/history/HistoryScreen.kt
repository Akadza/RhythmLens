package com.rimuru.android.rhythmlens.ui.app.features.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.RhythmSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.history))
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                CenterMessage(
                    modifier = Modifier.padding(innerPadding),
                    title = stringResource(R.string.history),
                    body = "Загрузка истории ЭКГ"
                )
            }

            state.errorMessage != null -> {
                CenterMessage(
                    modifier = Modifier.padding(innerPadding),
                    title = stringResource(R.string.history_error_title),
                    body = state.errorMessage
                )
            }

            state.items.isEmpty() -> {
                CenterMessage(
                    modifier = Modifier.padding(innerPadding),
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(RhythmSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(RhythmSpacing.Medium)
                ) {
                    items(
                        items = state.items,
                        key = { item -> item.id }
                    ) { item ->
                        EcgListItem(
                            item = item,
                            onClick = {
                                onEvent(HistoryEvent.EcgClicked(item.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(RhythmSpacing.XXLarge),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.two_line_message_template, title, body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
