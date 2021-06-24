package com.fprieto.hms.wearable.presentation.ui

import android.os.Bundle
import androidx.fragment.app.FragmentFactory
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.databinding.ActivityWearEngineBinding
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.AuthClient
import com.huawei.wearengine.auth.Permission
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import javax.inject.Inject

private val hiWearPermissions = arrayOf(Permission.DEVICE_MANAGER, Permission.NOTIFY)

class WearEngineActivity : DaggerAppCompatActivity() {

    private val authClient: AuthClient by lazy {
        HiWear.getAuthClient(this)
    }

    private lateinit var binding: ActivityWearEngineBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var fragmentFactory: FragmentFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.fragmentFactory = fragmentFactory
        binding = ActivityWearEngineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setBottomNavigation()
        checkPermissions()
    }

    private fun setBottomNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.mainNavigationFragment
        ) as NavHostFragment

        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun checkPermissions() {
        try {
            authClient.checkPermissions(hiWearPermissions).isSuccessful.let { allGranted ->
                "All permission granted: $allGranted".logResult()
                if (!allGranted) {
                    askForHiWearPermissions()
                }
            }

        } catch (e: Exception) {
            "Failed to ask for permissions".logResult()
            onNotAllPermissionGranted()
        }
    }

    private fun askForHiWearPermissions() {
        authClient.requestPermission(object : AuthCallback {
            override fun onOk(permissions: Array<Permission>) {
                if (!permissions.map { it.name }.containsAll(hiWearPermissions.map { it.name })) {
                    onNotAllPermissionGranted()
                }
            }

            override fun onCancel() {
                onNotAllPermissionGranted()
            }
        }, *hiWearPermissions)
    }

    private fun onNotAllPermissionGranted() {
        runOnUiThread {
            "Not all permission are granted for HiWear!".logResult()
            finish()
        }
    }

    override fun onSupportNavigateUp() =
        Navigation.findNavController(this, R.id.mainNavigationFragment).navigateUp()
}

private fun String.logResult() {
    Timber.d(this)
}