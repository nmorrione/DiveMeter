package com.nmorrione.divemeter.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.ui.map.MapMarker
import com.nmorrione.divemeter.ui.map.OsmMapView
import org.osmdroid.util.GeoPoint

private val DEFAULT_CENTER = GeoPoint(41.9028, 12.4964) // Rome — used only until the first dive is saved

@Composable
fun HomeScreen(
    onNavigateToManualEntry: () -> Unit,
    onNavigateToVideoCalc: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val dives by viewModel.dives.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var centerOverride by remember { mutableStateOf<GeoPoint?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val lastDive = dives.firstOrNull()
    val mapCenter = centerOverride
        ?: lastDive?.let { GeoPoint(it.latitude, it.longitude) }
        ?: DEFAULT_CENTER

    val searchResults = remember(searchQuery, dives) {
        if (searchQuery.isBlank()) emptyList() else dives.filter {
            it.spotName.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            center = mapCenter,
            modifier = Modifier.fillMaxSize(),
            markers = dives.map { dive ->
                MapMarker(
                    position = GeoPoint(dive.latitude, dive.longitude),
                    title = dive.spotName,
                    snippet = "${dive.heightMeters} m"
                )
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
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

            if (searchResults.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Column {
                        searchResults.forEach { dive ->
                            ListItem(
                                headlineContent = { Text(dive.spotName) },
                                supportingContent = { Text("${dive.heightMeters} m") },
                                modifier = Modifier.clickable {
                                    centerOverride = GeoPoint(dive.latitude, dive.longitude)
                                    searchQuery = ""
                                }
                            )
                        }
                    }
                }
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
