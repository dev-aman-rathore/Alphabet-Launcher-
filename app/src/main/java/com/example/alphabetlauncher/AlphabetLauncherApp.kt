package com.example.alphabetlauncher

import android.app.Application
import com.example.alphabetlauncher.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AlphabetLauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AlphabetLauncherApp)
            modules(appModule)
        }
    }
}
