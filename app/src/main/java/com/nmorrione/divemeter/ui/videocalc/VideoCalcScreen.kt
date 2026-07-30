package com.nmorrione.divemeter.ui.videocalc

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.ui.common.LocationPickerSection
import com.nmorrione.divemeter.ui.common.StarRating
import java.util.Locale

private const val GRAVITY = 9.80665

private fun estimateHeightMeters(apexMs: Long, entryMs: Long): Double {
    val fallSeconds = (entryMs - apexMs) / 1000.0
    return 0.5 * GRAVITY * fallSeconds * fallSeconds
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCalcScreen(
    onNavigateBack: () -> Unit,
    viewModel: VideoCalcViewModel = viewModel()
) {
    var videoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var apexMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var entryMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var spotName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableStateOf(0) }
    var pickedLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapTouchActive by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val pickVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            apexMs = null
            entryMs = null
        }
    }

    val apex = apexMs
    val entry = entryMs
    val heightMeters = if (apex != null && entry != null && entry > apex) {
        estimateHeightMeters(apex, entry)
    } else {
        null
    }
    val canSave = videoUri != null && heightMeters != null && spotName.isNotBlank() && pickedLocation != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.video_calc_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        val currentVideoUri = videoUri
        if (currentVideoUri == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.video_calc_choose_video_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(
                        onClick = {
                            pickVideoLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.VideoOnly
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.video_calc_choose_video))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState(), enabled = !mapTouchActive)
                    .padding(16.dp)
            ) {
                VideoTimelinePlayer(
                    videoUri = currentVideoUri,
                    apexMs = apexMs,
                    entryMs = entryMs,
                    onApexMark = { apexMs = it },
                    onEntryMark = { entryMs = it },
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = {
                        pickVideoLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.VideoOnly
                            )
                        )
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(stringResource(R.string.video_calc_change_video))
                }

                Text(
                    text = stringResource(R.string.video_calc_apex_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                if (apex != null && entry != null) {
                    if (entry > apex) {
                        Text(
                            text = stringResource(
                                R.string.video_calc_estimated_height,
                                String.format(Locale.US, "%.2f", heightMeters)
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.video_calc_entry_before_apex),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = spotName,
                    onValueChange = { spotName = it },
                    label = { Text(stringResource(R.string.manual_entry_spot_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )

                LocationPickerSection(
                    pickedLocation = pickedLocation,
                    onLocationPicked = { pickedLocation = it },
                    markerTitle = spotName.ifBlank { "New spot" },
                    modifier = Modifier.padding(top = 16.dp),
                    onMapTouchActive = { mapTouchActive = it }
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.dive_description)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )

                Text(
                    text = stringResource(R.string.dive_rating),
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                StarRating(rating = rating, onRatingChange = { rating = it })

                Button(
                    onClick = {
                        val location = pickedLocation
                        val height = heightMeters
                        if (height != null && spotName.isNotBlank() && location != null) {
                            viewModel.saveDive(
                                spotName,
                                height,
                                location.latitude,
                                location.longitude,
                                currentVideoUri.toString(),
                                description,
                                rating,
                                onNavigateBack
                            )
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp)
                ) {
                    Text(stringResource(R.string.manual_entry_save))
                }
            }
        }
    }
}
