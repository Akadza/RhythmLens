package com.rimuru.android.rhythmlens.ui.screens.ecg

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.screens.ecg.components.AiAnalysisCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcgDetailScreen(
    ecgId: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ЭКГ • 02.05.2026 14:52") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Фиксированная нижняя панель действий
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Сравнить */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.compare))
                    }
                    Button(
                        onClick = { /* TODO: Сгенерировать синтетику */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.generate_synthetic))
                    }
                    Button(
                        onClick = { /* TODO: Экспорт */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.export))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. AI Анализ
            AiAnalysisCard()

            // 2. 12 отведений
            Text(
                text = stringResource(R.string.leads_12),
                style = MaterialTheme.typography.headlineMedium
            )

            // Заглушка для графиков (позже заменим на Vico)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Здесь будут 12 интерактивных графиков отведений\n(Vico Compose)",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // 3. Заключение врача
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.doctor_conclusion),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Заключение врача будет отображаться здесь.\n" +
                                "Врач сможет добавить/отредактировать текст.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}