package com.fprieto.hms.wearable.presentation.ui.di

import androidx.fragment.app.Fragment
import com.fprieto.hms.wearable.di.ActivityScope
import com.fprieto.hms.wearable.presentation.ui.DashboardFragment
import com.fprieto.hms.wearable.presentation.ui.MessagingFragment
import com.fprieto.hms.wearable.presentation.ui.PlayerFragment
import com.fprieto.hms.wearable.presentation.ui.WearEngineActivity
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
internal abstract class WearEngineMainActivityModule {
    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMainActivity(): WearEngineActivity

    @Binds
    @IntoMap
    @FragmentKey(DashboardFragment::class)
    abstract fun dashboardFragment(dashboardFragment: DashboardFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(MessagingFragment::class)
    abstract fun messagingFragment(messagingFragment: MessagingFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(PlayerFragment::class)
    abstract fun playerFragment(playerFragment: PlayerFragment): Fragment
}
