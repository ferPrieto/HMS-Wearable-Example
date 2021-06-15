package com.fprieto.hms.wearable.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides

@Module
open class AppModule {
    @Provides
    fun provideContext(app: Application): Context = app.applicationContext
}
