package com.example.alphabetlauncher.ui.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import com.example.alphabetlauncher.core.util.sdp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val ALPHABET_ITEMS = buildList {
    add("★")
    for (c in 'A'..'Z') {
        add(c.toString())
    }
    add("•")
}

/**
 * High-performance single Canvas component for the interactive A-Z scrubber bar.
 *
 * Implements:
 * 1. Straight vertical alignment at rest.
 * 2. Natural cosine-falloff horizontal displacement pointing directly at the touch point.
 * 3. Enlarged circular bubble showing the currently selected letter adjacent to the finger.
 * 4. Spring-physics return animation on release.
 * 5. Pre-allocated Paint instances to guarantee 60 fps tracking with zero GC allocations.
 */
@Composable
fun AlphabetScrubberCanvas(
    modifier: Modifier = Modifier,
    onLetterSelected: (Char) -> Unit,
    onScrubStart: (Char) -> Unit,
    onScrubEnd: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Sizing strictly derived from sdp
    val barWidthSdp = 40.sdp
    val maxDisplacementSdp = 38.sdp
    val bubbleRadiusSdp = 24.sdp
    val letterTextSizeSdp = 10.sdp
    val bubbleTextSizeSdp = 22.sdp
    val barRightPaddingSdp = 12.sdp
    val bubbleOffsetSdp = 48.sdp

    val maxDisplacementPx = with(density) { maxDisplacementSdp.toPx() }
    val bubbleRadiusPx = with(density) { bubbleRadiusSdp.toPx() }
    val letterTextSizePx = with(density) { letterTextSizeSdp.toPx() }
    val bubbleTextSizePx = with(density) { bubbleTextSizeSdp.toPx() }
    val barRightPaddingPx = with(density) { barRightPaddingSdp.toPx() }
    val bubbleOffsetPx = with(density) { bubbleOffsetSdp.toPx() }

    // Colors
    val letterColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f).toArgb()
    val activeLetterColor = MaterialTheme.colorScheme.primary.toArgb()
    val bubbleBgColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val bubbleTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val bubbleBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f).toArgb()

    // Animation progress for smooth curve expansion and spring return (0f = resting, 1f = fully curved)
    val curveProgress = remember { Animatable(0f) }

    var isTouching by remember { mutableStateOf(false) }
    var currentTouchY by remember { mutableFloatStateOf(0f) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Pre-allocated Paint instances to avoid GC allocations during draw
    val letterPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    val bubbleBgPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    val bubbleBorderPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
    }

    val bubbleTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val textBounds = remember { Rect() }

    Canvas(
        modifier = modifier
            .width(barWidthSdp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialY = down.position.y
                    currentTouchY = initialY
                    isTouching = true

                    coroutineScope.launch {
                        curveProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }

                    // Process initial touch down
                    val itemCount = ALPHABET_ITEMS.size
                    val totalHeight = size.height.toFloat()
                    val verticalPadding = totalHeight * 0.08f
                    val usableHeight = totalHeight - 2 * verticalPadding
                    val itemSlotHeight = usableHeight / itemCount

                    val touchRelative = initialY - verticalPadding
                    val rawIndex = if (itemSlotHeight > 0f) {
                        kotlin.math.floor(touchRelative / itemSlotHeight).toInt().coerceIn(0, itemCount - 1)
                    } else {
                        0
                    }
                    selectedIndex = rawIndex

                    val selectedChar = when (val item = ALPHABET_ITEMS[rawIndex]) {
                        "★" -> 'A'
                        "•" -> 'Z'
                        else -> item.first()
                    }
                    onScrubStart(selectedChar)

                    var pointer = down
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointer.id } ?: break

                        if (!change.pressed) {
                            // Touch released
                            break
                        }

                        if (change.positionChange() != Offset.Zero) {
                            currentTouchY = change.position.y
                            val relY = currentTouchY - verticalPadding
                            val newIndex = if (itemSlotHeight > 0f) {
                                kotlin.math.floor(relY / itemSlotHeight).toInt().coerceIn(0, itemCount - 1)
                            } else {
                                0
                            }

                            if (newIndex != selectedIndex) {
                                selectedIndex = newIndex
                                val newChar = when (val item = ALPHABET_ITEMS[newIndex]) {
                                    "★" -> 'A'
                                    "•" -> 'Z'
                                    else -> item.first()
                                }
                                onLetterSelected(newChar)
                            }
                            change.consume()
                        }
                    }

                    // Release handling
                    isTouching = false
                    selectedIndex = -1
                    onScrubEnd()

                    coroutineScope.launch {
                        // Spring physics return animation
                        curveProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            }
    ) {
        val totalWidth = size.width
        val totalHeight = size.height

        val itemCount = ALPHABET_ITEMS.size
        val verticalPadding = totalHeight * 0.08f
        val usableHeight = totalHeight - 2 * verticalPadding
        val itemSlotHeight = usableHeight / itemCount

        val baseX = totalWidth - barRightPaddingPx
        val animValue = curveProgress.value

        // Influence radius for the cosine curve (around 5 letters on either side)
        val influenceRadius = itemSlotHeight * 5.5f

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // Configure paints
            letterPaint.textSize = letterTextSizePx
            bubbleBgPaint.color = bubbleBgColor
            bubbleBorderPaint.color = bubbleBorderColor
            bubbleTextPaint.color = bubbleTextColor
            bubbleTextPaint.textSize = bubbleTextSizePx

            var activeBubbleChar: String? = null
            var activeBubbleY = currentTouchY

            for (i in 0 until itemCount) {
                val item = ALPHABET_ITEMS[i]
                val defaultY = verticalPadding + (i + 0.5f) * itemSlotHeight

                // Calculate displacement using raised cosine bell curve
                val deltaY = kotlin.math.abs(defaultY - currentTouchY)
                val displacement = if (animValue > 0.001f && deltaY < influenceRadius) {
                    val normalizedDist = deltaY / influenceRadius
                    // Hann window / raised cosine: smooth 0 to 1 with 0 slope at boundaries
                    val factor = ((1.0 + cos(PI * normalizedDist)) * 0.5).toFloat()
                    maxDisplacementPx * factor * animValue
                } else {
                    0f
                }

                val letterX = baseX - displacement
                val isCurrentSelected = (i == selectedIndex && animValue > 0.3f)

                if (isCurrentSelected) {
                    activeBubbleChar = when (item) {
                        "★" -> "A"
                        "•" -> "Z"
                        else -> item
                    }
                    activeBubbleY = defaultY
                    letterPaint.color = activeLetterColor
                    letterPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                } else {
                    letterPaint.color = letterColor
                    letterPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }

                // Vertical centering for letter text
                letterPaint.getTextBounds(item, 0, item.length, textBounds)
                val textCenterY = defaultY - textBounds.exactCenterY()

                nativeCanvas.drawText(item, letterX, textCenterY, letterPaint)
            }

            // Draw letter bubble if active
            if (activeBubbleChar != null && animValue > 0.2f) {
                val bubbleAnimAlpha = ((animValue - 0.2f) / 0.8f).coerceIn(0f, 1f)
                val bubbleScale = 0.6f + 0.4f * bubbleAnimAlpha

                val currentBubbleRadius = bubbleRadiusPx * bubbleScale
                val bubbleCenterX = baseX - maxDisplacementPx * animValue - bubbleOffsetPx * animValue
                val minBubbleY = verticalPadding + currentBubbleRadius
                val maxBubbleY = (totalHeight - verticalPadding - currentBubbleRadius).coerceAtLeast(minBubbleY)
                val bubbleCenterY = activeBubbleY.coerceIn(minBubbleY, maxBubbleY)

                // Background circle
                nativeCanvas.drawCircle(bubbleCenterX, bubbleCenterY, currentBubbleRadius, bubbleBgPaint)
                // Border circle
                nativeCanvas.drawCircle(bubbleCenterX, bubbleCenterY, currentBubbleRadius, bubbleBorderPaint)

                // Bubble text
                bubbleTextPaint.textSize = bubbleTextSizePx * bubbleScale
                bubbleTextPaint.getTextBounds(activeBubbleChar, 0, activeBubbleChar.length, textBounds)
                val bubbleTextY = bubbleCenterY - textBounds.exactCenterY()
                nativeCanvas.drawText(activeBubbleChar, bubbleCenterX, bubbleTextY, bubbleTextPaint)
            }
        }
    }
}
