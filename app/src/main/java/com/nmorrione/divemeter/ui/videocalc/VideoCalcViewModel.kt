package com.nmorrione.divemeter.ui.videocalc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nmorrione.divemeter.data.Dive
import com.nmorrione.divemeter.data.DiveMeterDatabase
import com.nmorrione.divemeter.data.DiveMethod
import kotlinx.coroutines.launch

class VideoCalcViewModel(application: Application) : AndroidViewModel(application) {

    private val diveDao = DiveMeterDatabase.getInstance(application).diveDao()

    fun saveDive(
        spotName: String,
        heightMeters: Double,
        latitude: Double,
        longitude: Double,
        videoUri: String,
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
                    method = DiveMethod.VIDEO,
                    videoUri = videoUri
                )
            )
            onSaved()
        }
    }
}
