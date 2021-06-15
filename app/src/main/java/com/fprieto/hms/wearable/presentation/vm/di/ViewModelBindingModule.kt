package com.fprieto.hms.wearable.presentation.vm.di

import androidx.lifecycle.ViewModelProvider
import com.fprieto.hms.wearable.android.InjectingViewModelFactory
import dagger.Binds
import dagger.Module

@Module
abstract class ViewModelBindingModule {
    @Binds
    internal abstract fun bindViewModelFactory(factory: InjectingViewModelFactory): ViewModelProvider.Factory
}
