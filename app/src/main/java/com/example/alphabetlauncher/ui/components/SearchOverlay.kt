package com.example.alphabetlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.alphabetlauncher.core.util.sdp
import com.example.alphabetlauncher.core.util.textSdp
import com.example.alphabetlauncher.data.model.AppModel

/**
 * Slide-up Search Overlay opened by swiping up on the home screen.
 * Displays search bar, initial "Favorites" section with heading, or real-time filtered "Results".
 * Strictly uses sdp for sizing.
 */
@Composable
fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    favorites: List<AppModel>,
    searchResults: List<AppModel>,
    onClose: () -> Unit,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.sdp, vertical = 14.sdp)
    ) {
        // Top search bar row with Cancel button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchBarView(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.sdp))

            Text(
                text = "Cancel",
                fontSize = 12.textSdp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(vertical = 6.sdp, horizontal = 4.sdp)
            )
        }

        Spacer(modifier = Modifier.height(18.sdp))

        // Content area based on search query
        val isSearching = query.trim().isNotEmpty()

        if (!isSearching) {
            // Initial state: Favorites heading and list
            Text(
                text = "Favorites",
                fontSize = 15.textSdp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.sdp)
            )

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.sdp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "No favorite apps yet. Long-press any app to add it.",
                        fontSize = 13.textSdp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = favorites,
                        key = { "fav_${it.packageName}/${it.activityName}" }
                    ) { app ->
                        AppItemRow(
                            app = app,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.sdp))
                    }
                }
            }
        } else {
            // Active typing state: Results heading and list or empty state
            Text(
                text = "Results",
                fontSize = 15.textSdp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.sdp)
            )

            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.sdp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "No apps found",
                        fontSize = 13.textSdp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = searchResults,
                        key = { "search_${it.packageName}/${it.activityName}" }
                    ) { app ->
                        AppItemRow(
                            app = app,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.sdp))
                    }
                }
            }
        }
    }
}
