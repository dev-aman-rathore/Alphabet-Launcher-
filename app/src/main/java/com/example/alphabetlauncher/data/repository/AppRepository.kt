package com.example.alphabetlauncher.data.repository

import com.example.alphabetlauncher.data.datasource.AppDataSource
import com.example.alphabetlauncher.data.datasource.FavoritesPreferencesDataSource
import com.example.alphabetlauncher.data.model.AppModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository interface for managing app discovery, in-memory caching,
 * alphabet categorization, and favorite app persistence.
 */
interface AppRepository {
    suspend fun getInstalledApps(forceRefresh: Boolean = false): Result<List<AppModel>>
    suspend fun getAppsGroupedByLetter(forceRefresh: Boolean = false): Result<Map<Char, List<AppModel>>>
    fun getFavoriteAppsFlow(): Flow<List<AppModel>>
    suspend fun addFavorite(packageName: String)
    suspend fun removeFavorite(packageName: String)
}

/**
 * Implementation of [AppRepository] with in-memory caching to guarantee
 * zero PackageManager queries on the touch/animation path, backed by DataStore for custom favorites.
 */
class AppRepositoryImpl(
    private val appDataSource: AppDataSource,
    private val favoritesPreferencesDataSource: FavoritesPreferencesDataSource
) : AppRepository {

    private val mutex = Mutex()
    private var cachedApps: List<AppModel>? = null
    private var cachedGroupedApps: Map<Char, List<AppModel>>? = null

    private val targetFavoriteCount = 6

    override suspend fun getInstalledApps(forceRefresh: Boolean): Result<List<AppModel>> =
        mutex.withLock {
            runCatching {
                if (cachedApps == null || forceRefresh) {
                    val apps = appDataSource.loadInstalledApps()
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
                    cachedApps = apps
                    cachedGroupedApps = buildGroupedMap(apps)
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
                }
                cachedGroupedApps ?: emptyMap()
            }
        }

    override fun getFavoriteAppsFlow(): Flow<List<AppModel>> {
        return favoritesPreferencesDataSource.favoritePackagesFlow.map { favoritePackages ->
            val allApps = cachedApps ?: appDataSource.loadInstalledApps().also {
                cachedApps = it.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { app -> app.label })
                cachedGroupedApps = buildGroupedMap(cachedApps!!)
            }

            val appByPackage = allApps.associateBy { it.packageName }
            val favorites = mutableListOf<AppModel>()

            // 1. Map user-saved favorite packages
            for (pkg in favoritePackages) {
                val app = appByPackage[pkg]
                if (app != null && !favorites.contains(app)) {
                    favorites.add(app)
                }
            }

            // 2. If favorites count is less than targetFavoriteCount on initial launch, backfill from allApps
            if (favorites.size < targetFavoriteCount && allApps.isNotEmpty()) {
                for (app in allApps) {
                    if (!favorites.contains(app)) {
                        favorites.add(app)
                        if (favorites.size >= targetFavoriteCount) break
                    }
                }
            }

            favorites
        }
    }

    override suspend fun addFavorite(packageName: String) {
        favoritesPreferencesDataSource.addFavorite(packageName)
    }

    override suspend fun removeFavorite(packageName: String) {
        favoritesPreferencesDataSource.removeFavorite(packageName)
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
}
