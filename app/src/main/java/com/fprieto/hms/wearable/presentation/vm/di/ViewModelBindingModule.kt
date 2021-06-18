package com.fprieto.hms.wearable.presentation.vm.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fprieto.hms.wearable.android.InjectingViewModelFactory
import com.fprieto.hms.wearable.presentation.vm.DashboardViewModel
import com.fprieto.hms.wearable.presentation.vm.DashboardViewModelImpl
import com.fprieto.hms.wearable.presentation.vm.PlayerViewModel
import com.fprieto.hms.wearable.presentation.vm.PlayerViewModelImpl
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelBindingModule {
    @Binds
    internal abstract fun bindViewModelFactory(factory: InjectingViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(PlayerViewModel::class)
    abstract fun playerViewModel(viewModel: PlayerViewModelImpl): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DashboardViewModel::class)
    abstract fun dashboardViewModel(viewModel: DashboardViewModelImpl): ViewModel
}
