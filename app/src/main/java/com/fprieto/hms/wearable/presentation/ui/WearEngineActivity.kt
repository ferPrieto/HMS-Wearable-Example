package com.fprieto.hms.wearable.presentation.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.fprieto.hms.wearable.R
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.AuthClient
import com.huawei.wearengine.auth.Permission
import timber.log.Timber

private val hiWearPermissions = arrayOf(Permission.DEVICE_MANAGER, Permission.NOTIFY)

class WearEngineMainActivity : AppCompatActivity() {

    private val authClient: AuthClient by lazy {
        HiWear.getAuthClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
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