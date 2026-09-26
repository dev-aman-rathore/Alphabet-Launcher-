package com.example.alphabetlauncher.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alphabetlauncher.core.util.sdp
import com.example.alphabetlauncher.core.util.textSdp
import com.example.alphabetlauncher.ui.components.AlphabetScrubberCanvas
import com.example.alphabetlauncher.ui.components.ClockDateView
import com.example.alphabetlauncher.ui.components.FavoritesList
import com.example.alphabetlauncher.ui.components.FilteredAppList
import com.example.alphabetlauncher.ui.state.LauncherUiState
import com.example.alphabetlauncher.ui.viewmodel.LauncherViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Root HomeScreen of the Alphabet Launcher.
 * Harmonizes the live resting view, filtered app drawer, and single-canvas A-Z scrubber.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: LauncherViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clockState by viewModel.clockState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main content area (left side & center)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 22.sdp,
                    end = 42.sdp,
                    top = 36.sdp,
                    bottom = 12.sdp
                )
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
                                }
                            )
                        } else {
                            // Resting state: clock, date, and favorites list
                            Column(modifier = Modifier.fillMaxSize()) {
                                ClockDateView(clockState = clockState)

                                Spacer(modifier = Modifier.height(24.sdp))

                                FavoritesList(
                                    favorites = state.favorites,
                                    onAppClick = { app ->
                                        viewModel.launchApp(context, app)
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
    }
}
