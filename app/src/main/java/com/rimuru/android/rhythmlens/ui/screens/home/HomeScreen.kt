package com.rimuru.android.rhythmlens.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rimuru.android.rhythmlens.R
import com.rimuru.android.rhythmlens.ui.theme.PrimaryBlue
import com.rimuru.android.rhythmlens.ui.theme.SecondaryTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onImportClick: () -> Unit
) {

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),

        contentWindowInsets = WindowInsets(0),

        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            painter = painterResource(id = R.drawable.ic_heart_ecg),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = SecondaryTeal
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = stringResource(R.string.app_name),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        sheetState.show()
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 12.dp),

                shape = CircleShape,

                containerColor = PrimaryBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_input_add),
                    contentDescription = stringResource(R.string.add_ecg),
                    modifier = Modifier.size(38.dp)
                )
            }
        },

        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Icon(
                painter = painterResource(id = R.drawable.ic_heart_ecg),
                contentDescription = null,

                modifier = Modifier.size(160.dp),

                tint = SecondaryTeal
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.app_name),

                style = MaterialTheme.typography.displayLarge,

                color = MaterialTheme.colorScheme.onBackground,

                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.slogan),

                style = MaterialTheme.typography.headlineMedium,

                color = PrimaryBlue,

                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(
                    WindowInsets.navigationBars
                )
            )
        }
    }

    if (sheetState.isVisible) {

        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                }
            },

            sheetState = sheetState,

            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),

                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = stringResource(R.string.how_to_add_ecg),
                    style = MaterialTheme.typography.headlineSmall
                )

                Button(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onScanClick()
                        }
                    },

                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_scan))
                }

                Button(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onGalleryClick()
                        }
                    },

                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_gallery))
                }

                Button(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onImportClick()
                        }
                    },

                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_import))
                }
            }
        }
    }
}