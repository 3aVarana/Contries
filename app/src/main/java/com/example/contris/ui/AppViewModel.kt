package com.example.contris.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contris.domain.usecase.SyncCountriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** App-scoped ViewModel: triggers the initial sync once per process. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val syncCountries: SyncCountriesUseCase,
) : ViewModel() {

    private var started = false

    fun syncIfStale() {
        if (started) return
        started = true
        viewModelScope.launch { syncCountries(force = false) }
    }
}
