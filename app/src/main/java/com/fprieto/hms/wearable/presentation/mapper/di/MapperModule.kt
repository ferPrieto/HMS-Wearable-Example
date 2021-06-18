package com.fprieto.hms.wearable.presentation.mapper.di

import com.fprieto.hms.wearable.presentation.mapper.RemoteDataMessageToLocalMapper
import com.fprieto.hms.wearable.presentation.mapper.RemoteDataMessageToLocalMapperImpl
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
class MapperModule {
    @Provides
    @Reusable
    fun provideCompanyInfoDomainToUiModelMapper(): RemoteDataMessageToLocalMapper =
        RemoteDataMessageToLocalMapperImpl()
}
