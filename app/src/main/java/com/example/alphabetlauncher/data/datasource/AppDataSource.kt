package com.example.alphabetlauncher.data.datasource

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import com.example.alphabetlauncher.data.model.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data source interface responsible for querying installed launchable applications.
 */
interface AppDataSource {
    suspend fun loadInstalledApps(): List<AppModel>
}

/**
 * Android PackageManager implementation of [AppDataSource].
 * Filters out the launcher itself and provides fallback handling for labels and icons.
 */
class PackageManagerAppDataSource(
    private val context: Context
) : AppDataSource {

    override suspend fun loadInstalledApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(mainIntent, 0)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val ownPackageName = context.packageName

        resolveInfos
            .filter { resolveInfo ->
                val pkgName = resolveInfo.activityInfo?.packageName
                pkgName != null && pkgName != ownPackageName
            }
            .mapNotNull { resolveInfo ->
                try {
                    val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                    val packageName = activityInfo.packageName
                    val activityName = activityInfo.name

                    val rawLabel = resolveInfo.loadLabel(packageManager)?.toString()
                    val label = if (!rawLabel.isNullOrBlank()) {
                        rawLabel.trim()
                    } else {
                        packageName
                    }

                    val icon = resolveInfo.loadIcon(packageManager)
                        ?: packageManager.defaultActivityIcon

                    val iconBitmap = try {
                        drawableToBitmap(icon).asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }

                    val firstChar = label.firstOrNull()?.uppercaseChar() ?: '#'
                    val initial = if (firstChar in 'A'..'Z') firstChar else '#'

                    AppModel(
                        label = label,
                        packageName = packageName,
                        activityName = activityName,
                        icon = icon,
                        iconBitmap = iconBitmap,
                        initial = initial
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
