package com.rimuru.android.rhythmlens.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EcgListItem(
    ecgId: String,
    date: String,
    patientName: String,
    mainDiagnosis: String,
    heartRate: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = date, style = MaterialTheme.typography.titleMedium)
            Text(text = patientName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = mainDiagnosis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$heartRate уд/мин",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}