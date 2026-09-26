package com.example.alphabetlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos

class ScrubberMathTest {

    private val alphabetItems = buildList {
        add("★")
        for (c in 'A'..'Z') {
            add(c.toString())
        }
        add("•")
    }

    private fun calculateIndex(touchY: Float, totalHeight: Float): Int {
        val verticalPadding = totalHeight * 0.08f
        val usableHeight = totalHeight - 2 * verticalPadding
        val itemSlotHeight = usableHeight / alphabetItems.size
        val touchRelative = touchY - verticalPadding
        return (touchRelative / itemSlotHeight).toInt().coerceIn(0, alphabetItems.size - 1)
    }

    private fun computeFalloff(deltaY: Float, influenceRadius: Float): Float {
        if (deltaY >= influenceRadius) return 0f
        val normalizedDist = deltaY / influenceRadius
        return ((1.0 + cos(PI * normalizedDist)) * 0.5).toFloat()
    }

    @Test
    fun calculateIndex_clampsCorrectlyAtBoundaries() {
        val totalHeight = 1000f

        // Touch above the top boundary maps to index 0 (★)
        val topIndex = calculateIndex(-50f, totalHeight)
        assertEquals(0, topIndex)
        assertEquals("★", alphabetItems[topIndex])

        // Touch below the bottom boundary maps to last index (•)
        val bottomIndex = calculateIndex(1100f, totalHeight)
        assertEquals(alphabetItems.size - 1, bottomIndex)
        assertEquals("•", alphabetItems[bottomIndex])
    }

    @Test
    fun calculateIndex_mapsCenterToMiddleAlphabet() {
        val totalHeight = 1000f
        val centerIndex = calculateIndex(500f, totalHeight)

        // Center of 28 items should be near index 13 or 14 ('M' or 'N')
        assertTrue(centerIndex in 13..14)
        val letter = alphabetItems[centerIndex].first()
        assertTrue(letter in 'L'..'O')
    }

    @Test
    fun computeFalloff_producesSmoothHannWindow() {
        val radius = 100f

        // Center has maximum falloff factor of 1.0
        val centerFactor = computeFalloff(0f, radius)
        assertEquals(1.0f, centerFactor, 0.001f)

        // Boundary has factor 0.0
        val boundaryFactor = computeFalloff(radius, radius)
        assertEquals(0.0f, boundaryFactor, 0.001f)

        // Outside boundary has factor 0.0
        val outsideFactor = computeFalloff(150f, radius)
        assertEquals(0.0f, outsideFactor, 0.001f)

        // Halfway has factor 0.5
        val halfFactor = computeFalloff(radius / 2f, radius)
        assertEquals(0.5f, halfFactor, 0.001f)
    }
}
