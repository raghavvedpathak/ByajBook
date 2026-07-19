package com.byajbook.ui

// [FIX-ARCH-STATE-1] Spec Requirement: Shared UI State Model
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}