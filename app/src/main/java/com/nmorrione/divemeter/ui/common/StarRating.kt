package com.nmorrione.divemeter.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 1-5 star row. Tappable by default (tapping the currently selected star clears it back to 0);
 * pass [onRatingChange] as null for a plain read-only display (e.g. in a dive detail view).
 */
@Composable
fun StarRating(
    rating: Int,
    onRatingChange: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (star in 1..5) {
            val starModifier = Modifier.size(32.dp).let {
                if (onRatingChange != null) {
                    it.clickable { onRatingChange(if (star == rating) 0 else star) }
                } else {
                    it
                }
            }
            Icon(
                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = star.toString(),
                tint = MaterialTheme.colorScheme.primary,
                modifier = starModifier
            )
        }
    }
}
