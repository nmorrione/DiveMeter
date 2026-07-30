package com.nmorrione.divemeter.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.data.Dive
import com.nmorrione.divemeter.ui.map.DiveMapView
import com.nmorrione.divemeter.ui.map.MapMarker

private val DEFAULT_CENTER = LatLng(41.9028, 12.4964) // Rome — used only until the first dive is saved
private const val OVERVIEW_ZOOM = 12f
private const val FOCUSED_ZOOM = 16f

@SuppressLint("MissingPermission") // guarded by hasLocationPermission before every fused-location call
@Composable
fun HomeScreen(
    onNavigateToManualEntry: () -> Unit,
    onNavigateToVideoCalc: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val dives by viewModel.dives.collectAsState()
    // Re-fires every time this screen is (re)entered — including returning from Manual
    // Entry/Video Calc — since the ViewModel outlives navigation but this composable doesn't.
    LaunchedEffect(Unit) { viewModel.refresh() }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var centerOverride by remember { mutableStateOf<LatLng?>(null) }
    var mapZoomOverride by remember { mutableStateOf<Float?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedDiveId by remember { mutableStateOf<Long?>(null) }
    var mapType by remember { mutableStateOf(GoogleMap.MAP_TYPE_NORMAL) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cancellationTokenSource = remember { CancellationTokenSource() }
    DisposableEffect(Unit) { onDispose { cancellationTokenSource.cancel() } }

    // The app always opens on the user's current position, not the last saved dive.
    var currentDeviceLocation by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
                .addOnSuccessListener { location ->
                    if (location != null) currentDeviceLocation = LatLng(location.latitude, location.longitude)
                }
        }
    }

    val lastDive = dives.firstOrNull()
    val mapCenter = centerOverride
        ?: currentDeviceLocation
        ?: lastDive?.let { LatLng(it.latitude, it.longitude) }
        ?: DEFAULT_CENTER
    val mapZoom = mapZoomOverride ?: if (centerOverride != null) FOCUSED_ZOOM else OVERVIEW_ZOOM

    val searchResults = remember(searchQuery, dives) {
        if (searchQuery.isBlank()) emptyList() else dives.filter {
            it.spotName.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DiveMapView(
            center = mapCenter,
            zoom = mapZoom,
            modifier = Modifier.fillMaxSize(),
            markers = dives.map { dive ->
                MapMarker(
                    position = LatLng(dive.latitude, dive.longitude),
                    title = dive.spotName,
                    snippet = "${dive.heightMeters} m",
                    id = dive.id
                )
            },
            onMarkerClick = { id -> selectedDiveId = id },
            showMyLocation = hasLocationPermission,
            mapType = mapType
        )

        if (isSearchActive) {
            // A focusable Popup gets its own window-level input focus, which is what makes
            // typing actually work here — but a focusable Popup is also touch-modal for the
            // whole screen, so it must only exist while the user is actively searching.
            // Outside of that, this same area is a plain clickable row (see below) that lets
            // touches reach the map underneath, so panning/pinch-zoom and the FAB keep working.
            SearchPopup(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                results = searchResults,
                onResultClick = { dive ->
                    centerOverride = LatLng(dive.latitude, dive.longitude)
                    mapZoomOverride = FOCUSED_ZOOM
                    searchQuery = ""
                    isSearchActive = false
                },
                onDismiss = { isSearchActive = false }
            )
        } else {
            Surface(
                onClick = { isSearchActive = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(1f)
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 4.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Text(
                        text = searchQuery.ifBlank { stringResource(R.string.search_spots_hint) },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        Surface(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(1f)
                .padding(top = 84.dp, start = 16.dp)
                .size(48.dp),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 4.dp,
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title)
                )
            }
        }

        Surface(
            onClick = {
                mapType = if (mapType == GoogleMap.MAP_TYPE_NORMAL) {
                    GoogleMap.MAP_TYPE_HYBRID
                } else {
                    GoogleMap.MAP_TYPE_NORMAL
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(1f)
                .padding(top = 84.dp, end = 16.dp)
                .size(48.dp),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 4.dp,
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = stringResource(R.string.toggle_map_type)
                )
            }
        }

        if (dives.isEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 104.dp)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Text(
                    text = stringResource(R.string.no_spots_saved_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (hasLocationPermission) {
            Surface(
                onClick = {
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            centerOverride = LatLng(location.latitude, location.longitude)
                            mapZoomOverride = FOCUSED_ZOOM
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(1f)
                    .padding(end = 24.dp, bottom = 92.dp)
                    .size(48.dp),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp,
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.manual_entry_use_current_location)
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(1f)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_dive_fab_description))
        }
    }

    if (showAddSheet) {
        AddDiveSheet(
            onDismiss = { showAddSheet = false },
            onManualEntryClick = {
                showAddSheet = false
                onNavigateToManualEntry()
            },
            onVideoCalcClick = {
                showAddSheet = false
                onNavigateToVideoCalc()
            }
        )
    }

    val selectedDive = dives.firstOrNull { it.id == selectedDiveId }
    if (selectedDive != null) {
        DiveDetailSheet(
            dive = selectedDive,
            onDismiss = { selectedDiveId = null },
            onDelete = {
                viewModel.deleteDive(selectedDive.id)
                selectedDiveId = null
            }
        )
    }
}

@Composable
private fun SearchPopup(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Dive>,
    onResultClick: (Dive) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Popup(
        alignment = Alignment.TopCenter,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 4.dp,
                tonalElevation = 2.dp
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.search_spots_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp)
                )
            }

            if (query.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    if (results.isNotEmpty()) {
                        Column {
                            results.forEach { dive ->
                                ListItem(
                                    headlineContent = { Text(dive.spotName) },
                                    supportingContent = { Text("${dive.heightMeters} m") },
                                    modifier = Modifier.clickable { onResultClick(dive) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.search_no_results, query),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
