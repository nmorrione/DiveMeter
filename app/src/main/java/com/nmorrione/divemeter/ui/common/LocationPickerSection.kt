package com.nmorrione.divemeter.ui.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cancellationTokenSource = remember { CancellationTokenSource() }
    DisposableEffect(Unit) { onDispose { cancellationTokenSource.cancel() } }

    val requestCurrentLocation = rememberUpdatedState {
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationPicked(LatLng(location.latitude, location.longitude))
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) requestCurrentLocation.value()
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            requestCurrentLocation.value()
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
            IconButton(onClick = {
                if (hasLocationPermission) {
                    requestCurrentLocation.value()
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

        Surface(shape = RoundedCornerShape(12.dp)) {
            DiveMapView(
                center = pickedLocation ?: DEFAULT_CENTER,
                zoom = 15f,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                markers = pickedLocation?.let {
                    listOf(MapMarker(position = it, title = markerTitle))
                } ?: emptyList(),
                onMapTap = onLocationPicked
            )
        }
    }
}
