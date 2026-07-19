package com.byajbook.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byajbook.domain.model.Settings
import com.byajbook.domain.repository.BackupRepository
import com.byajbook.domain.repository.SettingsRepository
import com.byajbook.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Settings>>(UiState.Loading)
    val uiState: StateFlow<UiState<Settings>> = _uiState.asStateFlow()

    private val _importResult = MutableSharedFlow<Result<Unit>>()
    val importResult = _importResult.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getSettings()
                .onEach { _uiState.value = UiState.Success(it) }
                .catch { _uiState.value = UiState.Error(it.message ?: "Unknown Error") }
                .collect()
        }
    }

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings)
        }
    }

    suspend fun getBackupJson(): String {
        return backupRepository.exportBackup()
    }

    fun importBackup(json: String) {
        viewModelScope.launch {
            val result = backupRepository.importBackup(json)
            _importResult.emit(result)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            settingsRepository.clearTransactionData()
        }
    }
}
