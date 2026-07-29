package com.nmorrione.divemeter.ui.manualentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.nmorrione.divemeter.R
import com.nmorrione.divemeter.ui.common.LocationPickerSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManualEntryViewModel = viewModel()
) {
    var spotName by rememberSaveable { mutableStateOf("") }
    var heightText by rememberSaveable { mutableStateOf("") }
    var pickedLocation by remember { mutableStateOf<LatLng?>(null) }

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

            LocationPickerSection(
                pickedLocation = pickedLocation,
                onLocationPicked = { pickedLocation = it },
                markerTitle = spotName.ifBlank { "New spot" },
                modifier = Modifier.padding(top = 16.dp)
            )

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
