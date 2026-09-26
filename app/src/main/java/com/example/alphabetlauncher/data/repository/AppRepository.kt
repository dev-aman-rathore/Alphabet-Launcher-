package com.example.alphabetlauncher.data.repository

import com.example.alphabetlauncher.data.datasource.AppDataSource
import com.example.alphabetlauncher.data.model.AppModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository interface for managing app discovery, in-memory caching,
 * alphabet categorization, and favorite app selection.
 */
interface AppRepository {
    suspend fun getInstalledApps(forceRefresh: Boolean = false): Result<List<AppModel>>
    suspend fun getAppsGroupedByLetter(forceRefresh: Boolean = false): Result<Map<Char, List<AppModel>>>
    suspend fun getFavoriteApps(forceRefresh: Boolean = false): Result<List<AppModel>>
}

/**
 * Implementation of [AppRepository] with in-memory caching to guarantee
 * zero PackageManager queries on the touch/animation path.
 */
class AppRepositoryImpl(
    private val appDataSource: AppDataSource
) : AppRepository {

    private val mutex = Mutex()
    private var cachedApps: List<AppModel>? = null
    private var cachedGroupedApps: Map<Char, List<AppModel>>? = null
    private var cachedFavorites: List<AppModel>? = null

    // Well-known popular packages to prefer when populating default favorites
    private val preferredFavoritePackages = listOf(
        "com.whatsapp",
        "com.android.chrome",
        "com.google.android.youtube",
        "com.google.android.gm",
        "com.google.android.apps.photos",
        "com.google.android.calculator",
        "com.google.android.dialer",
        "com.android.camera",
        "com.google.android.GoogleCamera",
        "com.android.settings"
    )

    private val targetFavoriteCount = 6

    override suspend fun getInstalledApps(forceRefresh: Boolean): Result<List<AppModel>> =
        mutex.withLock {
            runCatching {
                if (cachedApps == null || forceRefresh) {
                    val apps = appDataSource.loadInstalledApps()
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
                    cachedApps = apps
                    cachedGroupedApps = buildGroupedMap(apps)
                    cachedFavorites = buildFavorites(apps)
                }
                cachedApps ?: emptyList()
            }
        }

    override suspend fun getAppsGroupedByLetter(forceRefresh: Boolean): Result<Map<Char, List<AppModel>>> =
        mutex.withLock {
            runCatching {
                if (cachedGroupedApps == null || forceRefresh) {
                    val apps = appDataSource.loadInstalledApps()
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
                    cachedApps = apps
                    cachedGroupedApps = buildGroupedMap(apps)
                    cachedFavorites = buildFavorites(apps)
                }
                cachedGroupedApps ?: emptyMap()
            }
        }

    override suspend fun getFavoriteApps(forceRefresh: Boolean): Result<List<AppModel>> =
        mutex.withLock {
            runCatching {
                if (cachedFavorites == null || forceRefresh) {
                    val apps = appDataSource.loadInstalledApps()
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
                    cachedApps = apps
                    cachedGroupedApps = buildGroupedMap(apps)
                    cachedFavorites = buildFavorites(apps)
                }
                cachedFavorites ?: emptyList()
            }
        }

    private fun buildGroupedMap(apps: List<AppModel>): Map<Char, List<AppModel>> {
        val map = ('A'..'Z').associateWith { mutableListOf<AppModel>() }.toMutableMap()
        val otherList = mutableListOf<AppModel>()

        for (app in apps) {
            val initial = app.initial
            if (initial in 'A'..'Z') {
                map[initial]?.add(app)
            } else {
                otherList.add(app)
            }
        }

        if (otherList.isNotEmpty()) {
            map['#'] = otherList
        }

        return map
    }

    private fun buildFavorites(allApps: List<AppModel>): List<AppModel> {
        if (allApps.isEmpty()) return emptyList()

        val appByPackage = allApps.associateBy { it.packageName }
        val favorites = mutableListOf<AppModel>()

        // 1. Pick preferred popular apps if installed
        for (pkg in preferredFavoritePackages) {
            val app = appByPackage[pkg]
            if (app != null && !favorites.contains(app)) {
                favorites.add(app)
                if (favorites.size >= targetFavoriteCount) break
            }
        }

        // 2. Fill remainder up to targetFavoriteCount from the remaining installed apps
        if (favorites.size < targetFavoriteCount) {
            for (app in allApps) {
                if (!favorites.contains(app)) {
                    favorites.add(app)
                    if (favorites.size >= targetFavoriteCount) break
                }
            }
        }

        return favorites
    }
}
