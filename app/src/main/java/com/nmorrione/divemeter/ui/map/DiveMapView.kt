package com.nmorrione.divemeter.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

data class MapMarker(
    val position: LatLng,
    val title: String,
    val snippet: String? = null
)

@Composable
fun DiveMapView(
    center: LatLng,
    modifier: Modifier = Modifier,
    zoom: Float = 14f,
    markers: List<MapMarker> = emptyList(),
    onMapTap: ((LatLng) -> Unit)? = null,
    showMyLocation: Boolean = false,
    mapType: MapType = MapType.NORMAL
) {
    val context = LocalContext.current
    val hasLocationPermission = showMyLocation &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, zoom)
    }

    LaunchedEffect(center, zoom) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, zoom), 600)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = mapType, isMyLocationEnabled = hasLocationPermission),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = hasLocationPermission,
            zoomControlsEnabled = false,
            compassEnabled = true
        ),
        onMapClick = { latLng -> onMapTap?.invoke(latLng) }
    ) {
        markers.forEach { marker ->
            Marker(
                state = MarkerState(position = marker.position),
                title = marker.title,
                snippet = marker.snippet
            )
        }
    }
}
