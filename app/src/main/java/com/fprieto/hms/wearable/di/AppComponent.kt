package com.fprieto.hms.wearable.di

import com.fprieto.hms.wearable.App
import com.fprieto.hms.wearable.presentation.ui.di.WearEngineMainActivityModule
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class,
        WearEngineMainActivityModule::class
    ]
)
@Singleton
interface AppComponent : AndroidInjector<App>
