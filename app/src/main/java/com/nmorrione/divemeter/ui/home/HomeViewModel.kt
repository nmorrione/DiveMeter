package com.nmorrione.divemeter.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nmorrione.divemeter.data.Dive
import com.nmorrione.divemeter.data.DiveMeterDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val diveDao = DiveMeterDatabase.getInstance(application).diveDao()

    val dives: StateFlow<List<Dive>> = diveDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
