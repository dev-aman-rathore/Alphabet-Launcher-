package com.example.alphabetlauncher.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal SDP (Scalable DP) dimension utility for Jetpack Compose.
 *
 * Implements standard SDP scaling based on a 300dp baseline width:
 * scaled_dp = (screenWidthDp / 300.0) * value
 *
 * Strictly adheres to project rules:
 * - Single unit convention: sdp for layout and text.
 * - No direct dp, sp, or ssp literals used in UI components.
 */

private const val SDP_BASE_WIDTH_DP = 300f

/**
 * Returns scaled Dp for layout sizing (padding, width, height, etc.).
 */
val Int.sdp: Dp
    @Composable
    get() {
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
        val scale = screenWidthDp / SDP_BASE_WIDTH_DP
        return (this * scale).dp
    }

/**
 * Returns scaled Dp for Float values.
 */
val Float.sdp: Dp
    @Composable
    get() {
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
        val scale = screenWidthDp / SDP_BASE_WIDTH_DP
        return (this * scale).dp
    }

/**
 * Returns scaled TextUnit for text sizing derived strictly from the sdp scale.
 * Avoids any direct sp or ssp definitions.
 */
val Int.textSdp: TextUnit
    @Composable
    get() {
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
        val scale = screenWidthDp / SDP_BASE_WIDTH_DP
        return (this * scale).sp
    }

/**
 * Returns scaled TextUnit for Float values derived strictly from sdp.
 */
val Float.textSdp: TextUnit
    @Composable
    get() {
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
        val scale = screenWidthDp / SDP_BASE_WIDTH_DP
        return (this * scale).sp
    }
