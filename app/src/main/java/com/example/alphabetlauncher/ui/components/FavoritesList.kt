package com.example.alphabetlauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.alphabetlauncher.data.model.AppModel

/**
 * Vertical list of favorite apps displayed beneath the clock during resting state.
 */
@Composable
fun FavoritesList(
    favorites: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        favorites.forEach { app ->
            AppItemRow(
                app = app,
                onClick = { onAppClick(app) }
            )
        }
    }
}
