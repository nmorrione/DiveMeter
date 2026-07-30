package com.nmorrione.divemeter.ui.manualentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmorrione.divemeter.data.DiveMethod
import com.nmorrione.divemeter.data.DiveRepository
import kotlinx.coroutines.launch

class ManualEntryViewModel : ViewModel() {

    fun saveDive(
        spotName: String,
        heightMeters: Double,
        latitude: Double,
        longitude: Double,
        description: String,
        rating: Int,
        ownerNickname: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            DiveRepository.insertDive(
                spotName = spotName,
                heightMeters = heightMeters,
                latitude = latitude,
                longitude = longitude,
                method = DiveMethod.MANUAL,
                description = description,
                rating = rating,
                ownerNickname = ownerNickname
            )
            onSaved()
        }
    }
}
