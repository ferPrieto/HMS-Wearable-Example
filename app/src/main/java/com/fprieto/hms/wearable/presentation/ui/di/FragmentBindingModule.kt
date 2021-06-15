package com.fprieto.hms.wearable.presentation.ui.di

import androidx.fragment.app.FragmentFactory
import com.fprieto.hms.wearable.android.InjectingFragmentFactory
import dagger.Binds
import dagger.Module

@Module
abstract class FragmentBindingModule {
    @Binds
    abstract fun bindFragmentFactory(factory: InjectingFragmentFactory): FragmentFactory
}
