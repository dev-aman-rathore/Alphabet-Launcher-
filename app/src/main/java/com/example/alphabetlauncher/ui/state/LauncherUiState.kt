package com.example.alphabetlauncher.ui.state

import com.example.alphabetlauncher.data.model.AppModel

/**
 * Sealed hierarchy defining the UI state of the Alphabet Launcher.
 */
sealed interface LauncherUiState {
    data object Loading : LauncherUiState

    data class Success(
        val favorites: List<AppModel>,
        val appsByLetter: Map<Char, List<AppModel>>,
        val isDragging: Boolean = false,
        val selectedLetter: Char? = null,
        val filteredApps: List<AppModel> = emptyList()
    ) : LauncherUiState

    data class Error(val message: String) : LauncherUiState
}

/**
 * Live time and date representation for the resting home view.
 */
data class ClockState(
    val time: String = "",
    val date: String = ""
)
