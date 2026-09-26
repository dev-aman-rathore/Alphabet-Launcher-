package com.example.alphabetlauncher.di

import com.example.alphabetlauncher.data.datasource.AppDataSource
import com.example.alphabetlauncher.data.datasource.PackageManagerAppDataSource
import com.example.alphabetlauncher.data.repository.AppRepository
import com.example.alphabetlauncher.data.repository.AppRepositoryImpl
import com.example.alphabetlauncher.ui.viewmodel.LauncherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<AppDataSource> { PackageManagerAppDataSource(get()) }
    single<AppRepository> { AppRepositoryImpl(get()) }
    viewModel { LauncherViewModel(get()) }
}
