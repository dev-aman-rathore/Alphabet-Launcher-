package com.example.alphabetlauncher.data.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Domain model representing an installed, launchable application.
 *
 * @property label User-facing app name.
 * @property packageName Unique application package identifier.
 * @property activityName Fully qualified main activity component name for launching.
 * @property icon Application icon drawable.
 * @property iconBitmap Pre-converted ImageBitmap for zero-allocation rendering in Compose.
 * @property initial The uppercase first character for alphabet categorization (A-Z or # for other).
 */
data class AppModel(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val iconBitmap: ImageBitmap? = null,
    val initial: Char
)

