package com.fprieto.hms.wearable.data.di

import com.fprieto.hms.wearable.data.DeviceLocalSource
import com.fprieto.hms.wearable.data.DeviceLocalSourceImpl
import com.fprieto.hms.wearable.data.repository.DeviceRepository
import com.fprieto.hms.wearable.data.repository.DeviceRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.Reusable
import javax.inject.Singleton

@Module
class DataModule {
    @Provides
    @Reusable
    fun provideDeviceRepository(
        deviceLocalSource: DeviceLocalSource
    ): DeviceRepository = DeviceRepositoryImpl(deviceLocalSource)

    @Provides
    @Singleton
    fun provideDeviceLocalSource(): DeviceLocalSource = DeviceLocalSourceImpl
}
