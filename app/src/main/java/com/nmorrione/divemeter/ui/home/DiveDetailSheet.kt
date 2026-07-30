package com.nmorrione.divemeter.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.data.Dive
import com.nmorrione.divemeter.data.DiveMethod
import com.nmorrione.divemeter.ui.common.StarRating
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiveDetailSheet(dive: Dive, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(text = dive.spotName, style = MaterialTheme.typography.headlineSmall)

            Text(
                text = stringResource(R.string.dive_detail_height, String.format(Locale.US, "%.2f", dive.heightMeters)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = stringResource(
                    when (dive.method) {
                        DiveMethod.MANUAL -> R.string.dive_detail_method_manual
                        DiveMethod.VIDEO -> R.string.dive_detail_method_video
                        DiveMethod.BAROMETER -> R.string.dive_detail_method_barometer
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (dive.rating > 0) {
                StarRating(
                    rating = dive.rating,
                    onRatingChange = null,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (dive.description.isNotBlank()) {
                Text(
                    text = dive.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            val ownerLabel = dive.ownerNickname.ifBlank { stringResource(R.string.dive_detail_unknown_user) }
            val dateLabel = remember(dive.timestampMillis) {
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(dive.timestampMillis))
            }
            Text(
                text = stringResource(R.string.dive_detail_added_by_on, ownerLabel, dateLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
        }
    }
}
