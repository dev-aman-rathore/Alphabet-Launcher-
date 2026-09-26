package com.example.alphabetlauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * Filtered app list view displayed when the user is scrubbing the A-Z bar.
 * Displays the active letter header and the alphabetical list of matching apps,
 * or a clear empty state when no apps exist for the letter.
 */
@Composable
fun FilteredAppList(
    letter: Char,
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Prominent letter header
        Text(
            text = letter.toString(),
            fontSize = 38.textSdp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.sdp)
        )

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.sdp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "No apps",
                    fontSize = 14.textSdp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Normal
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    AppItemRow(
                        app = app,
                        onClick = { onAppClick(app) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.sdp))
                }
            }
        }
    }
}
