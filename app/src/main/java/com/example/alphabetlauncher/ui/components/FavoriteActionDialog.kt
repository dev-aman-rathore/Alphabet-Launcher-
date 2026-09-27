package com.example.alphabetlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.example.alphabetlauncher.core.util.sdp
import com.example.alphabetlauncher.core.util.textSdp
import com.example.alphabetlauncher.data.model.AppModel

/**
 * Material 3 confirmation dialog displayed when a user long-presses an app.
 * Provides explicit Add or Remove CTA for managing custom favorites.
 * Strictly uses sdp for sizing.
 */
@Composable
fun FavoriteActionDialog(
    app: AppModel,
    isFavorite: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val titleText = if (isFavorite) "Remove from Favorites" else "Add to Favorites"
    val ctaText = if (isFavorite) "Remove" else "Add"
    val confirmationText = if (isFavorite) {
        "Remove \"${app.label}\" from your home favorites?"
    } else {
        "Add \"${app.label}\" to your home favorites?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            val iconSize = 38.sdp
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.initial.toString(),
                        fontSize = 16.textSdp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        title = {
            Text(
                text = titleText,
                fontSize = 15.textSdp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = confirmationText,
                fontSize = 13.textSdp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (isFavorite) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                }
            ) {
                Text(
                    text = ctaText,
                    fontSize = 13.textSdp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontSize = 13.textSdp
                )
            }
        }
    )
}
