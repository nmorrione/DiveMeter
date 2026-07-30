package com.nmorrione.divemeter.ui.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nmorrione.divemeter.BuildConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.ui.map.DiveMapView
import com.nmorrione.divemeter.ui.map.MapMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

private val DEFAULT_CENTER = LatLng(41.9028, 12.4964) // Rome — starting point until the user taps the map or a GPS fix arrives
private const val PICKER_ZOOM = 15f
private const val PICKER_SEARCH_ZOOM = 13f
private const val GEOCODE_LOG_TAG = "DiveMeter/Geocode"

// A key restricted to "Android apps" only accepts requests that carry these two headers
// matching the calling app's package name and signing certificate — the Maps SDK attaches
// them automatically, but a plain REST call like this one has to set them itself, or every
// request gets silently rejected (looks identical to "place not found" from the caller's side).
private fun signingCertSha1(context: Context): String? = try {
    val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()
    } else {
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES
        )
        @Suppress("DEPRECATION")
        packageInfo.signatures?.firstOrNull()
    }
    signature?.let {
        MessageDigest.getInstance("SHA1").digest(it.toByteArray())
            .joinToString("") { byte -> "%02X".format(byte) }
    }
} catch (e: Exception) {
    null
}

// The on-device Geocoder relies on a backend service some phones (notably several
// Xiaomi/Poco ROMs) don't ship, so it silently fails there. The Geocoding REST API
// works identically on every device since it's a plain network call; it reuses the
// same Maps API key, which needs the "Geocoding API" enabled in Google Cloud Console.
private suspend fun geocodePlace(context: Context, query: String): LatLng? = withContext(Dispatchers.IO) {
    try {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL(
            "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedQuery&key=${BuildConfig.MAPS_API_KEY}"
        )
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("X-Android-Package", context.packageName)
        signingCertSha1(context)?.let { connection.setRequestProperty("X-Android-Cert", it) }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val status = json.optString("status")
        if (status != "OK") {
            Log.w(GEOCODE_LOG_TAG, "geocode failed for \"$query\": status=$status error=${json.optString("error_message")}")
            return@withContext null
        }
        val location = json.getJSONArray("results")
            .getJSONObject(0)
            .getJSONObject("geometry")
            .getJSONObject("location")
        LatLng(location.getDouble("lat"), location.getDouble("lng"))
    } catch (e: Exception) {
        Log.w(GEOCODE_LOG_TAG, "geocode request failed for \"$query\"", e)
        null
    }
}

/**
 * Label + search + GPS button + tappable mini-map, reused by every "add a dive" flow that needs
 * to capture a spot's location. Prefills from the device's current location when permission is
 * already granted; the user can always override by searching a place, tapping the map, or the
 * GPS button again.
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
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isLocating by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchCenter by remember { mutableStateOf<LatLng?>(null) }
    var searchNotFound by remember { mutableStateOf(false) }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cancellationTokenSource = remember { CancellationTokenSource() }
    DisposableEffect(Unit) { onDispose { cancellationTokenSource.cancel() } }

    fun requestCurrentLocation() {
        isLocating = true
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    searchCenter = null
                    onLocationPicked(LatLng(location.latitude, location.longitude))
                    isLocating = false
                } else {
                    // No fresh fix yet (common right after opening the screen) — fall back to
                    // the last known fix rather than leaving the user staring at a dead button.
                    fusedClient.lastLocation
                        .addOnSuccessListener { last ->
                            isLocating = false
                            if (last != null) {
                                searchCenter = null
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

    fun runSearch() {
        val query = searchQuery.trim()
        focusManager.clearFocus()
        if (query.isEmpty()) return
        isSearching = true
        coroutineScope.launch {
            val result = geocodePlace(context, query)
            isSearching = false
            searchNotFound = result == null
            if (result != null) searchCenter = result
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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                searchNotFound = false
            },
            placeholder = { Text(stringResource(R.string.location_search_hint)) },
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        searchNotFound = false
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch() }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        if (searchNotFound) {
            Text(
                text = stringResource(R.string.location_search_not_found),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
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
                center = pickedLocation ?: searchCenter ?: DEFAULT_CENTER,
                zoom = if (pickedLocation == null && searchCenter != null) PICKER_SEARCH_ZOOM else PICKER_ZOOM,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                markers = pickedLocation?.let {
                    listOf(MapMarker(position = it, title = markerTitle))
                } ?: emptyList(),
                onMapTap = {
                    searchCenter = null
                    onLocationPicked(it)
                }
            )
        }
    }
}
