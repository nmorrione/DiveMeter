package com.nmorrione.divemeter.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay

data class MapMarker(
    val position: GeoPoint,
    val title: String,
    val snippet: String? = null
)

@Composable
fun OsmMapView(
    center: GeoPoint,
    modifier: Modifier = Modifier,
    zoom: Double = 14.0,
    markers: List<MapMarker> = emptyList(),
    onMapTap: ((GeoPoint) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            controller.setCenter(center)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(center) {
        mapView.controller.animateTo(center)
    }

    LaunchedEffect(markers, onMapTap) {
        mapView.overlays.clear()

        if (onMapTap != null) {
            val receiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    onMapTap(p)
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            }
            mapView.overlays.add(MapEventsOverlay(receiver))
        }

        markers.forEach { marker ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = marker.position
                    title = marker.title
                    snippet = marker.snippet
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
        }
        mapView.invalidate()
    }

    AndroidView(modifier = modifier, factory = { mapView })
}
