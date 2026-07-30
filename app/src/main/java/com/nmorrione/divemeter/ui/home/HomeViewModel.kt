package com.nmorrione.divemeter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nmorrione.divemeter.data.Dive
import com.nmorrione.divemeter.data.DiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _dives = MutableStateFlow<List<Dive>>(emptyList())
    val dives: StateFlow<List<Dive>> = _dives.asStateFlow()

    // Not called from init: this ViewModel survives navigating away to add a dive (it's scoped
    // to the "home" back-stack entry), but HomeScreen's composition is torn down and rebuilt on
    // each visit — so refresh() is triggered from there instead, to pick up newly saved dives.
    fun refresh() {
        viewModelScope.launch {
            _dives.value = DiveRepository.fetchDives()
        }
    }

    fun deleteDive(id: Long) {
        viewModelScope.launch {
            DiveRepository.deleteDive(id)
            refresh()
        }
    }
}
