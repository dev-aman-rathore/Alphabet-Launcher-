package com.example.alphabetlauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alphabetlauncher.core.util.sdp
import com.example.alphabetlauncher.core.util.textSdp
import com.example.alphabetlauncher.ui.components.AlphabetScrubberCanvas
import com.example.alphabetlauncher.ui.components.ClockDateView
import com.example.alphabetlauncher.ui.components.FavoriteActionDialog
import com.example.alphabetlauncher.ui.components.FavoritesList
import com.example.alphabetlauncher.ui.components.FilteredAppList
import com.example.alphabetlauncher.ui.components.SearchOverlay
import com.example.alphabetlauncher.ui.state.LauncherUiState
import com.example.alphabetlauncher.ui.viewmodel.LauncherViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Root HomeScreen of the Alphabet Launcher.
 * Harmonizes the live resting view (capped at 7 favorites), filtered app drawer on scrubbing,
 * swipe-up search overlay, and custom favorites management dialog.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clockState by viewModel.clockState.collectAsStateWithLifecycle()

    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val selectedAppForAction by viewModel.selectedAppForAction.collectAsStateWithLifecycle()
    val isSelectedAppFavorite by viewModel.isSelectedAppFavorite.collectAsStateWithLifecycle()

    // Dismiss search overlay on device back press
    BackHandler(enabled = isSearchOpen) {
        viewModel.closeSearch()
    }

    var swipeUpDragTotal by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main content area (left side & center) with swipe-up search gesture detector
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 22.sdp,
                    end = 42.sdp,
                    top = 36.sdp,
                    bottom = 12.sdp
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            swipeUpDragTotal = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            swipeUpDragTotal += dragAmount
                        },
                        onDragEnd = {
                            // Negative dragAmount corresponds to upward vertical motion
                            if (swipeUpDragTotal < -50f && !isSearchOpen) {
                                viewModel.openSearch()
                            }
                        }
                    )
                }
        ) {
            when (val state = uiState) {
                is LauncherUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                is LauncherUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                fontSize = 13.textSdp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(10.sdp))
                            TextButton(onClick = { viewModel.loadApps(forceRefresh = true) }) {
                                Text(
                                    text = "Retry",
                                    fontSize = 12.textSdp
                                )
                            }
                        }
                    }
                }

                is LauncherUiState.Success -> {
                    AnimatedContent(
                        targetState = state.isDragging,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "LauncherContentTransition"
                    ) { isDragging ->
                        if (isDragging && state.selectedLetter != null) {
                            // Active scrubbing state: filtered app list
                            FilteredAppList(
                                letter = state.selectedLetter,
                                apps = state.filteredApps,
                                onAppClick = { app ->
                                    viewModel.launchApp(context, app)
                                },
                                onAppLongClick = { app ->
                                    viewModel.onAppLongClick(app)
                                }
                            )
                        } else {
                            // Resting state: clock, date, and favorites list (capped at 7)
                            Column(modifier = Modifier.fillMaxSize()) {
                                ClockDateView(clockState = clockState)

                                Spacer(modifier = Modifier.height(24.sdp))

                                FavoritesList(
                                    favorites = state.favorites.take(7),
                                    onAppClick = { app ->
                                        viewModel.launchApp(context, app)
                                    },
                                    onAppLongClick = { app ->
                                        viewModel.onAppLongClick(app)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Vertical A-Z scrubber bar pinned to the right edge
        AlphabetScrubberCanvas(
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetterSelected = { letter ->
                viewModel.onScrubMove(letter)
            },
            onScrubStart = { letter ->
                viewModel.onScrubStart(letter)
            },
            onScrubEnd = {
                viewModel.onScrubEnd()
            }
        )

        // Slide-up Search Overlay
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            SearchOverlay(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                favorites = (uiState as? LauncherUiState.Success)?.favorites ?: emptyList(),
                searchResults = searchResults,
                onClose = { viewModel.closeSearch() },
                onAppClick = { app ->
                    viewModel.closeSearch()
                    viewModel.launchApp(context, app)
                },
                onAppLongClick = { app ->
                    viewModel.onAppLongClick(app)
                }
            )
        }

        // Custom Favorites Confirmation Dialog on Long Press
        selectedAppForAction?.let { app ->
            FavoriteActionDialog(
                app = app,
                isFavorite = isSelectedAppFavorite,
                onConfirm = {
                    if (isSelectedAppFavorite) {
                        viewModel.removeFavorite(app)
                    } else {
                        viewModel.addFavorite(app)
                    }
                },
                onDismiss = {
                    viewModel.dismissActionDialog()
                }
            )
        }
    }
}
