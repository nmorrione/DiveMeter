package com.nmorrione.divemeter.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.view.MotionEvent
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
    val snippet: String? = null,
    val id: Long? = null
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
    onMarkerClick: ((Long) -> Unit)? = null,
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
            // Mini-maps are often embedded inside a scrollable Column (e.g. the entry forms).
            // Without this, a vertical drag that starts on the map is intercepted by the
            // parent scroll container instead of panning the map.
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
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
            // The SDK's own my-location button is placed by the SDK itself and can't be
            // repositioned declaratively; the app provides its own button instead, so this
            // only enables the blue "you are here" dot on the map.
            map.uiSettings.isMyLocationButtonEnabled = false
            map.isMyLocationEnabled = hasLocationPermission
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoom))
            disableFocusRecursively(mapView)
            googleMap = map
        }
    }

    LaunchedEffect(googleMap, onMapTap) {
        googleMap?.setOnMapClickListener { latLng -> onMapTap?.invoke(latLng) }
    }

    LaunchedEffect(googleMap, onMarkerClick) {
        googleMap?.setOnMarkerClickListener { marker ->
            val id = marker.tag as? Long
            if (id != null && onMarkerClick != null) {
                onMarkerClick(id)
                true // consumed: skip the default info window and the SDK's own re-center/zoom
            } else {
                false
            }
        }
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
            )?.tag = marker.id
        }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}
