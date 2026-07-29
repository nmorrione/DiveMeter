package com.nmorrione.divemeter.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

data class MapMarker(
    val position: LatLng,
    val title: String,
    val snippet: String? = null
)

// The Maps SDK adds its own internal focusable child views (gesture/render surfaces) after
// the map finishes loading. Disabling focus only on the outer MapView isn't enough — one of
// those descendants still grabs input focus on tap, which stops the Compose search bar
// overlaid on top of the map from ever receiving it.
private fun disableFocusRecursively(view: View) {
    view.isFocusable = false
    view.isFocusableInTouchMode = false
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            disableFocusRecursively(view.getChildAt(i))
        }
    }
}

@SuppressLint("MissingPermission") // guarded by hasLocationPermission before enabling my-location
@Composable
fun DiveMapView(
    center: LatLng,
    modifier: Modifier = Modifier,
    zoom: Float = 14f,
    markers: List<MapMarker> = emptyList(),
    onMapTap: ((LatLng) -> Unit)? = null,
    showMyLocation: Boolean = false,
    mapType: Int = GoogleMap.MAP_TYPE_NORMAL
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasLocationPermission = showMyLocation &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    val mapView = remember {
        MapView(context).apply {
            // A MapView is a real Android View and grabs input focus on tap by default,
            // which stops overlapping Compose text fields (e.g. the home search bar) from
            // ever receiving it. Disabling focusability fixes that without losing gestures.
            isFocusable = false
            isFocusableInTouchMode = false
            onCreate(null)
        }
    }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = hasLocationPermission
            map.isMyLocationEnabled = hasLocationPermission
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoom))
            disableFocusRecursively(mapView)
            googleMap = map
        }
    }

    LaunchedEffect(googleMap, onMapTap) {
        googleMap?.setOnMapClickListener { latLng -> onMapTap?.invoke(latLng) }
    }

    LaunchedEffect(googleMap, mapType) {
        googleMap?.mapType = mapType
    }

    LaunchedEffect(googleMap, center, zoom) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(center, zoom), 600, null)
    }

    LaunchedEffect(googleMap, markers) {
        val map = googleMap ?: return@LaunchedEffect
        map.clear()
        markers.forEach { marker ->
            map.addMarker(
                MarkerOptions()
                    .position(marker.position)
                    .title(marker.title)
                    .snippet(marker.snippet)
            )
        }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}
