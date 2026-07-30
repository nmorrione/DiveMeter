package com.nmorrione.divemeter.ui.manualentry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nmorrione.divemeter.data.Dive
import com.nmorrione.divemeter.data.DiveMeterDatabase
import com.nmorrione.divemeter.data.DiveMethod
import com.nmorrione.divemeter.data.UserPreferences
import kotlinx.coroutines.launch

class ManualEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val diveDao = DiveMeterDatabase.getInstance(application).diveDao()

    fun saveDive(
        spotName: String,
        heightMeters: Double,
        latitude: Double,
        longitude: Double,
        description: String,
        rating: Int,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            diveDao.insert(
                Dive(
                    spotName = spotName,
                    heightMeters = heightMeters,
                    latitude = latitude,
                    longitude = longitude,
                    timestampMillis = System.currentTimeMillis(),
                    method = DiveMethod.MANUAL,
                    description = description,
                    rating = rating,
                    ownerNickname = UserPreferences.getNickname(getApplication<Application>()) ?: ""
                )
            )
            onSaved()
        }
    }
}
