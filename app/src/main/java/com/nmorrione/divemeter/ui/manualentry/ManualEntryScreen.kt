package com.nmorrione.divemeter.ui.manualentry

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.model.LatLng
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.ui.map.DiveMapView
import com.nmorrione.divemeter.ui.map.MapMarker

private val DEFAULT_CENTER = LatLng(41.9028, 12.4964) // Rome — starting point until the user taps the map or a GPS fix arrives

@SuppressLint("MissingPermission") // guarded by hasLocationPermission before every fused-location call
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManualEntryViewModel = viewModel()
) {
    val context = LocalContext.current
    var spotName by rememberSaveable { mutableStateOf("") }
    var heightText by rememberSaveable { mutableStateOf("") }
    var pickedLocation by remember { mutableStateOf<LatLng?>(null) }
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
                    pickedLocation = LatLng(location.latitude, location.longitude)
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

    val height = heightText.toDoubleOrNull()
    val canSave = spotName.isNotBlank() && height != null && height > 0 && pickedLocation != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_entry_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = spotName,
                onValueChange = { spotName = it },
                label = { Text(stringResource(R.string.manual_entry_spot_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it },
                label = { Text(stringResource(R.string.manual_entry_height)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
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
                        listOf(MapMarker(position = it, title = spotName.ifBlank { "New spot" }))
                    } ?: emptyList(),
                    onMapTap = { tapped -> pickedLocation = tapped }
                )
            }

            Button(
                onClick = {
                    val location = pickedLocation
                    val parsedHeight = heightText.toDoubleOrNull()
                    if (spotName.isNotBlank() && parsedHeight != null && parsedHeight > 0 && location != null) {
                        viewModel.saveDive(spotName, parsedHeight, location.latitude, location.longitude, onNavigateBack)
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                Text(stringResource(R.string.manual_entry_save))
            }
        }
    }
}
