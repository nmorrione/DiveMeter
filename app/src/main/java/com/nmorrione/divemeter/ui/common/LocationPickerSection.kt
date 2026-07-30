package com.nmorrione.divemeter.ui.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.ui.map.DiveMapView
import com.nmorrione.divemeter.ui.map.MapMarker

private val DEFAULT_CENTER = LatLng(41.9028, 12.4964) // Rome — starting point until the user taps the map or a GPS fix arrives
private const val PICKER_ZOOM = 15f

/**
 * Label + GPS button + tappable mini-map, reused by every "add a dive" flow that needs to
 * capture a spot's location. Prefills from the device's current location when permission is
 * already granted; the user can always override by tapping the map or the GPS button again.
 */
@SuppressLint("MissingPermission") // guarded by hasLocationPermission before every fused-location call
@Composable
fun LocationPickerSection(
    pickedLocation: LatLng?,
    onLocationPicked: (LatLng) -> Unit,
    markerTitle: String,
    modifier: Modifier = Modifier,
    onMapTouchActive: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isLocating by remember { mutableStateOf(false) }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cancellationTokenSource = remember { CancellationTokenSource() }
    DisposableEffect(Unit) { onDispose { cancellationTokenSource.cancel() } }

    fun requestCurrentLocation() {
        isLocating = true
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationPicked(LatLng(location.latitude, location.longitude))
                    isLocating = false
                } else {
                    // No fresh fix yet (common right after opening the screen) — fall back to
                    // the last known fix rather than leaving the user staring at a dead button.
                    fusedClient.lastLocation
                        .addOnSuccessListener { last ->
                            isLocating = false
                            if (last != null) {
                                onLocationPicked(LatLng(last.latitude, last.longitude))
                            } else {
                                Toast.makeText(context, R.string.location_unavailable, Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            isLocating = false
                            Toast.makeText(context, R.string.location_unavailable, Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                isLocating = false
                Toast.makeText(context, R.string.location_unavailable, Toast.LENGTH_SHORT).show()
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) requestCurrentLocation()
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            requestCurrentLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.manual_entry_pick_location),
                style = MaterialTheme.typography.bodyLarge
            )
            if (isLocating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = {
                    if (hasLocationPermission) {
                        requestCurrentLocation()
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.manual_entry_use_current_location)
                    )
                }
            }
        }

        val currentOnMapTouchActive by rememberUpdatedState(onMapTouchActive)
        Surface(
            shape = RoundedCornerShape(12.dp),
            // Without this, a vertical drag on the map is stolen by the entry screen's own
            // scrollable Column instead of panning the map — this disables that scroll for
            // the duration of any touch that starts here, regardless of drag direction. Keyed
            // on Unit (not on the callback) so the gesture loop survives recomposition instead
            // of restarting mid-drag and leaving the "touch active" flag stuck on.
            modifier = Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    currentOnMapTouchActive(true)
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        currentOnMapTouchActive(false)
                    }
                }
            }
        ) {
            DiveMapView(
                center = pickedLocation ?: DEFAULT_CENTER,
                zoom = PICKER_ZOOM,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                markers = pickedLocation?.let {
                    listOf(MapMarker(position = it, title = markerTitle))
                } ?: emptyList(),
                onMapTap = onLocationPicked
            )
        }
    }
}
