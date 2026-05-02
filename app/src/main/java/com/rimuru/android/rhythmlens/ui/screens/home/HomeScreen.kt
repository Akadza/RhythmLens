package com.rimuru.android.rhythmlens.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit,           // для камеры
    onGalleryClick: () -> Unit,        // для галереи
    onImportClick: () -> Unit          // для импорта файла
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { scope.launch { sheetState.show() } },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_input_add),
                    contentDescription = stringResource(R.string.add_ecg)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = stringResource(R.string.slogan),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    // Modal Bottom Sheet
    if (sheetState.isVisible) {
        ModalBottomSheet(
            onDismissRequest = { scope.launch { sheetState.hide() } },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Как добавить ЭКГ?",
                    style = MaterialTheme.typography.headlineSmall
                )

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }
                        onScanClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📸 Сфотографировать ЭКГ")
                }

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }
                        onGalleryClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🖼 Из галереи")
                }

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }
                        onImportClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📁 Импорт оцифрованных данных")
                }
            }
        }
    }
}