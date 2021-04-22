package com.fprieto.hms.wearable.ui

import android.os.Bundle
import android.widget.Toast
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
            val grantedPermissions = authClient.checkPermissions(hiWearPermissions)
            val allPermissionsGranted = grantedPermissions.isSuccessful
            Timber.d("All permission granted: $allPermissionsGranted")
            if (!allPermissionsGranted) {
                askForHiWearPermissions()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to ask for permissions")
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
            Toast.makeText(this, "Not all permission are granted for HiWear!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp() =
            Navigation.findNavController(this, R.id.mainNavigationFragment).navigateUp()


    /*
       private fun displayNotificationOnWearable(title: String, text: String) {
           val device = checkSelectedDevice()
           if (device == null) {
               printResultOnUIThread("No device is selected")
               return
           }

           val client = HiWear.getNotifyClient(this)
           val notification = Notification.Builder()
                   .setPackageName(PEER_PKG_NAME)
                   .setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_ONE_BUTTON)
                   .setButtonContents(hashMapOf(Pair(NotificationConstants.BUTTON_ONE_CONTENT_KEY, "Okay")))
                   .setTitle(title)
                   .setText(text)
                   .build()

           client.notify(device, notification).addOnSuccessListener {
               printResultOnUIThread("Send notification successfully!")
           }.addOnFailureListener { e ->
               Timber.e(e, "Failed to send notification")
               printResultOnUIThread("On notification error: ${e.message}")
           }
       }*/


}