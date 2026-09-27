package com.example.alphabetlauncher.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphabetlauncher.data.model.AppModel
import com.example.alphabetlauncher.data.repository.AppRepository
import com.example.alphabetlauncher.ui.state.ClockState
import com.example.alphabetlauncher.ui.state.LauncherUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class LauncherViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LauncherUiState>(LauncherUiState.Loading)
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _clockState = MutableStateFlow(ClockState())
    val clockState: StateFlow<ClockState> = _clockState.asStateFlow()

    // Search overlay state
    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dialog state for managing custom favorites
    private val _selectedAppForAction = MutableStateFlow<AppModel?>(null)
    val selectedAppForAction: StateFlow<AppModel?> = _selectedAppForAction.asStateFlow()

    private val _isSelectedAppFavorite = MutableStateFlow(false)
    val isSelectedAppFavorite: StateFlow<Boolean> = _isSelectedAppFavorite.asStateFlow()

    val searchResults: StateFlow<List<AppModel>> = combine(
        _uiState,
        _searchQuery
    ) { state, query ->
        if (state !is LauncherUiState.Success) {
            emptyList()
        } else {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                emptyList()
            } else {
                state.allApps.filter { app ->
                    app.label.contains(trimmed, ignoreCase = true)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

    init {
        loadApps()
        startClockUpdates()
        observeFavorites()
    }

    fun loadApps(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = LauncherUiState.Loading
            try {
                val appsResult = appRepository.getInstalledApps(forceRefresh)
                val groupedResult = appRepository.getAppsGroupedByLetter(forceRefresh)

                if (appsResult.isSuccess && groupedResult.isSuccess) {
                    val apps = appsResult.getOrDefault(emptyList())
                    val grouped = groupedResult.getOrDefault(emptyMap())
                    val initialFavorites = appRepository.getFavoriteAppsFlow().first()

                    _uiState.value = LauncherUiState.Success(
                        allApps = apps,
                        favorites = initialFavorites,
                        appsByLetter = grouped,
                        isDragging = false,
                        selectedLetter = null,
                        filteredApps = emptyList()
                    )
                } else {
                    val errorMsg = appsResult.exceptionOrNull()?.localizedMessage
                        ?: "Failed to load installed apps"
                    _uiState.value = LauncherUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = LauncherUiState.Error(e.localizedMessage ?: "Unexpected error")
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            appRepository.getFavoriteAppsFlow().collect { updatedFavorites ->
                val current = _uiState.value
                if (current is LauncherUiState.Success) {
                    _uiState.value = current.copy(favorites = updatedFavorites)
                }
            }
        }
    }

    private fun startClockUpdates() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = LocalDateTime.now()
                val time = now.format(timeFormatter)
                val date = now.format(dateFormatter)

                if (_clockState.value.time != time || _clockState.value.date != date) {
                    _clockState.value = ClockState(time = time, date = date)
                }

                delay(1000L)
            }
        }
    }

    fun onScrubStart(letter: Char) {
        val current = _uiState.value
        if (current is LauncherUiState.Success) {
            val matchingApps = current.appsByLetter[letter] ?: emptyList()
            _uiState.value = current.copy(
                isDragging = true,
                selectedLetter = letter,
                filteredApps = matchingApps
            )
        }
    }

    fun onScrubMove(letter: Char) {
        val current = _uiState.value
        if (current is LauncherUiState.Success) {
            if (current.selectedLetter != letter || !current.isDragging) {
                val matchingApps = current.appsByLetter[letter] ?: emptyList()
                _uiState.value = current.copy(
                    isDragging = true,
                    selectedLetter = letter,
                    filteredApps = matchingApps
                )
            }
        }
    }

    fun onScrubEnd() {
        val current = _uiState.value
        if (current is LauncherUiState.Success) {
            _uiState.value = current.copy(
                isDragging = false,
                selectedLetter = null,
                filteredApps = emptyList()
            )
        }
    }

    // Search management
    fun openSearch() {
        _searchQuery.value = ""
        _isSearchOpen.value = true
    }

    fun closeSearch() {
        _isSearchOpen.value = false
        _searchQuery.value = ""
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // Favorite Action Dialog management
    fun onAppLongClick(app: AppModel) {
        val current = _uiState.value
        if (current is LauncherUiState.Success) {
            val isFav = current.favorites.any { it.packageName == app.packageName }
            _isSelectedAppFavorite.value = isFav
            _selectedAppForAction.value = app
        }
    }

    fun dismissActionDialog() {
        _selectedAppForAction.value = null
    }

    fun addFavorite(app: AppModel) {
        viewModelScope.launch {
            appRepository.addFavorite(app.packageName)
            dismissActionDialog()
        }
    }

    fun removeFavorite(app: AppModel) {
        viewModelScope.launch {
            appRepository.removeFavorite(app.packageName)
            dismissActionDialog()
        }
    }

    fun launchApp(context: Context, app: AppModel) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    ?: Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        component = ComponentName(app.packageName, app.activityName)
                    }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Could not launch ${app.label}: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
