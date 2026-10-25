package com.rimon.pdfmastersuite.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimon.pdfmastersuite.audio.SoundManager
import com.rimon.pdfmastersuite.data.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)
    private val soundManager = SoundManager.getInstance(application)

    val soundEnabled: StateFlow<Boolean> = preferencesRepository.isSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hapticsEnabled: StateFlow<Boolean> = preferencesRepository.isHapticsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            soundEnabled.collect {
                soundManager.soundEnabled = it
            }
        }
        viewModelScope.launch {
            hapticsEnabled.collect {
                soundManager.hapticsEnabled = it
            }
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSoundEnabled(enabled)
            soundManager.soundEnabled = enabled
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHapticsEnabled(enabled)
            soundManager.hapticsEnabled = enabled
        }
    }
}
