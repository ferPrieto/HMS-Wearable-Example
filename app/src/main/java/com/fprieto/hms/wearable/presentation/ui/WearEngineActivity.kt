package com.fprieto.hms.wearable.presentation.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.FragmentFactory
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.databinding.ActivityWearEngineBinding
import com.huawei.hihealthkit.data.HiHealthExtendScope.HEALTHKIT_EXTEND_HEALTHBEHAVIOR_READ
import com.huawei.hihealthkit.data.HiHealthExtendScope.HEALTHKIT_EXTEND_SPORT_READ
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.SettingController
import com.huawei.hms.hihealth.data.Scopes.*
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.AuthClient
import com.huawei.wearengine.auth.Permission
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import javax.inject.Inject

private val hiWearPermissions = arrayOf(Permission.DEVICE_MANAGER, Permission.NOTIFY)
private val scopes = arrayOf(
    HEALTHKIT_STEP_READ,
    HEALTHKIT_CALORIES_READ,
    HEALTHKIT_ACTIVITY_READ,
    HEALTHKIT_LOCATION_READ,
    HEALTHKIT_SLEEP_READ,
    HEALTHKIT_HEARTRATE_READ,
    HEALTHKIT_OXYGENSTATURATION_READ,
    HEALTHKIT_BLOODGLUCOSE_READ,
    HEALTHKIT_ACTIVITY_RECORD_READ,
    HEALTHKIT_EXTEND_SPORT_READ,
    HEALTHKIT_EXTEND_HEALTHBEHAVIOR_READ
)

class WearEngineActivity : DaggerAppCompatActivity() {

    private val authClient: AuthClient by lazy {
        HiWear.getAuthClient(this)
    }

    private val settingController: SettingController by lazy {
        HuaweiHiHealth.getSettingController(this)
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
        requestHealthKitAuth()
        checkPermissions()
    }

    private fun setBottomNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.mainNavigationFragment
        ) as NavHostFragment

        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun requestHealthKitAuth() {
        val requestAuthorizationIntent: Intent =
            settingController.requestAuthorizationIntent(scopes, true)

        "Start HealthKit authorization activity".logResult()
        registerForActivityResult(
            StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                "HealthKit Result OK".logResult()
            } else {
                "HealthKit Result NOK".logResult()
            }
        }.launch(requestAuthorizationIntent)
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