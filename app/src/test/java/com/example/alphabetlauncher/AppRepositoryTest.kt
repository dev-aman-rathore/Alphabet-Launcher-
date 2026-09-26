package com.example.alphabetlauncher

import android.graphics.drawable.ColorDrawable
import com.example.alphabetlauncher.data.datasource.AppDataSource
import com.example.alphabetlauncher.data.model.AppModel
import com.example.alphabetlauncher.data.repository.AppRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRepositoryTest {

    private class FakeAppDataSource(private val apps: List<AppModel>) : AppDataSource {
        override suspend fun loadInstalledApps(): List<AppModel> = apps
    }

    private fun createApp(label: String, packageName: String): AppModel {
        val initial = label.firstOrNull()?.uppercaseChar() ?: '#'
        return AppModel(
            label = label,
            packageName = packageName,
            activityName = "MainActivity",
            icon = ColorDrawable(),
            iconBitmap = null,
            initial = if (initial in 'A'..'Z') initial else '#'
        )
    }

    @Test
    fun getAppsGroupedByLetter_groupsAppsCorrectlyAlphabetically() = runBlocking {
        val testApps = listOf(
            createApp("Chrome", "com.android.chrome"),
            createApp("Camera", "com.android.camera"),
            createApp("Gmail", "com.google.android.gm"),
            createApp("WhatsApp", "com.whatsapp"),
            createApp("1Password", "com.onepassword")
        )

        val repository = AppRepositoryImpl(FakeAppDataSource(testApps))
        val result = repository.getAppsGroupedByLetter()

        assertTrue(result.isSuccess)
        val map = result.getOrThrow()

        // Check 'C' has 2 apps, sorted case-insensitively
        val cApps = map['C'] ?: emptyList()
        assertEquals(2, cApps.size)
        assertEquals("Camera", cApps[0].label)
        assertEquals("Chrome", cApps[1].label)

        // Check 'G' has Gmail
        val gApps = map['G'] ?: emptyList()
        assertEquals(1, gApps.size)
        assertEquals("Gmail", gApps[0].label)

        // Check 'W' has WhatsApp
        val wApps = map['W'] ?: emptyList()
        assertEquals(1, wApps.size)
        assertEquals("WhatsApp", wApps[0].label)

        // Check non-letter initial grouped under '#'
        val numApps = map['#'] ?: emptyList()
        assertEquals(1, numApps.size)
        assertEquals("1Password", numApps[0].label)

        // Check empty letter has empty list
        val zApps = map['Z'] ?: emptyList()
        assertTrue(zApps.isEmpty())
    }

    @Test
    fun getFavoriteApps_prefersPopularPackagesAndLimitsCount() = runBlocking {
        val testApps = listOf(
            createApp("WhatsApp", "com.whatsapp"),
            createApp("Chrome", "com.android.chrome"),
            createApp("Gmail", "com.google.android.gm"),
            createApp("Calculator", "com.google.android.calculator"),
            createApp("App One", "com.example.one"),
            createApp("App Two", "com.example.two"),
            createApp("App Three", "com.example.three")
        )

        val repository = AppRepositoryImpl(FakeAppDataSource(testApps))
        val favorites = repository.getFavoriteApps().getOrThrow()

        assertTrue(favorites.size in 5..7)
        // Ensure preferred packages are first
        assertEquals("com.whatsapp", favorites[0].packageName)
        assertEquals("com.android.chrome", favorites[1].packageName)
        assertEquals("com.google.android.gm", favorites[2].packageName)
    }
}
