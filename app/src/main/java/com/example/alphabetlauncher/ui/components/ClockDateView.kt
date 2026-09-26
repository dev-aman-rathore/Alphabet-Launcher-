package com.example.alphabetlauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.alphabetlauncher.core.util.sdp
import com.example.alphabetlauncher.core.util.textSdp
import com.example.alphabetlauncher.ui.state.ClockState

/**
 * Live updating clock and date view for the launcher's resting state.
 * Sizing strictly uses universal sdp.
 */
@Composable
fun ClockDateView(
    clockState: ClockState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = clockState.time,
            fontSize = 46.textSdp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.sdp))

        Text(
            text = clockState.date,
            fontSize = 13.textSdp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
        )
    }
}
