package com.nmorrione.divemeter.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.ui.map.DiveMapView
import com.nmorrione.divemeter.ui.map.MapMarker

private val DEFAULT_CENTER = LatLng(41.9028, 12.4964) // Rome — used only until the first dive is saved
private const val OVERVIEW_ZOOM = 12f
private const val FOCUSED_ZOOM = 16f

@Composable
fun HomeScreen(
    onNavigateToManualEntry: () -> Unit,
    onNavigateToVideoCalc: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val dives by viewModel.dives.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var centerOverride by remember { mutableStateOf<LatLng?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
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

    val lastDive = dives.firstOrNull()
    val mapCenter = centerOverride
        ?: lastDive?.let { LatLng(it.latitude, it.longitude) }
        ?: DEFAULT_CENTER
    val mapZoom = if (centerOverride != null) FOCUSED_ZOOM else OVERVIEW_ZOOM

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
                    snippet = "${dive.heightMeters} m"
                )
            },
            showMyLocation = hasLocationPermission,
            mapType = mapType
        )

        // Rendered in a real, separate Android window (not just a Compose overlay layer).
        // The full-screen GoogleMap AndroidView below competes for touch/IME focus with any
        // Compose content placed directly on top of it in the same window; a focusable Popup
        // sidesteps that entirely since it owns its own window-level input focus.
        Popup(
            alignment = Alignment.TopCenter,
            properties = PopupProperties(focusable = true, dismissOnClickOutside = false)
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
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.search_spots_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp)
                    )
                }

                if (searchQuery.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        if (searchResults.isNotEmpty()) {
                            Column {
                                searchResults.forEach { dive ->
                                    ListItem(
                                        headlineContent = { Text(dive.spotName) },
                                        supportingContent = { Text("${dive.heightMeters} m") },
                                        modifier = Modifier.clickable {
                                            centerOverride = LatLng(dive.latitude, dive.longitude)
                                            searchQuery = ""
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.search_no_results, searchQuery),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
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
}
