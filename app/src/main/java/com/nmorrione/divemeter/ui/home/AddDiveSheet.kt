package com.nmorrione.divemeter.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nmorrione.divemeter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDiveSheet(
    onDismiss: () -> Unit,
    onManualEntryClick: () -> Unit,
    onVideoCalcClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.add_dive_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AddDiveOptionRow(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.method_manual_title),
                description = stringResource(R.string.method_manual_description),
                enabled = true,
                onClick = onManualEntryClick
            )
            AddDiveOptionRow(
                icon = Icons.Default.Videocam,
                title = stringResource(R.string.method_video_title),
                description = stringResource(R.string.method_video_description),
                enabled = true,
                onClick = onVideoCalcClick
            )
            AddDiveOptionRow(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.method_barometer_title),
                description = stringResource(R.string.method_barometer_description),
                enabled = false,
                onClick = {}
            )
        }
    }
}

@Composable
private fun AddDiveOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp).alpha(alpha)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.alpha(alpha)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
