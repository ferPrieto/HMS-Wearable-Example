package com.fprieto.hms.wearable.presentation.ui

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.audio.AudioPlayer
import com.fprieto.hms.wearable.credentials.CredentialsProvider
import com.fprieto.hms.wearable.databinding.FragmentMessagingBinding
import com.fprieto.hms.wearable.databinding.ViewLogsBinding
import com.fprieto.hms.wearable.extensions.await
import com.fprieto.hms.wearable.presentation.vm.MessagingViewModel
import com.fprieto.hms.wearable.presentation.vm.observeEvent
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.notify.Notification
import com.huawei.wearengine.notify.NotificationConstants
import com.huawei.wearengine.notify.NotificationTemplate
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

private val TAKE_PHOTO_PERMISSIONS =
    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)

class MessagingFragment @Inject constructor(
    viewModelFactory: ViewModelProvider.Factory
) : Fragment(R.layout.fragment_messaging) {

    private val viewModel by viewModels<MessagingViewModel> { viewModelFactory }

    private lateinit var binding: FragmentMessagingBinding
    private lateinit var viewLogsBinding: ViewLogsBinding

    private val credentialsProvider: CredentialsProvider = CredentialsProvider()

    private var sendCallback: SendCallback? = null
    private var selectedDevice: Device? = null
        set(value) {
            field = value
            setDeviceIntoRadioButton(value)
            registerReceiver(value)
        }

    private fun setDeviceIntoRadioButton(value: Device?) {
        binding.connectedDevice.text = value?.name
        binding.connectedDevice.isChecked = true
    }

    private val requestTakePhotosPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { true }) {
                takePicture.launch(null)
            }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            File.createTempFile("temp", ".jpg", requireActivity().cacheDir).let { file ->
                FileOutputStream(file).use { out ->
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        out
                    )
                }
                sendFile(file)
            }
        }

    private val p2pClient: P2pClient by lazy {
        HiWear.getP2pClient(context).apply {
            setPeerPkgName(credentialsProvider.getPeerPkgName())
            setPeerFingerPrint(credentialsProvider.getPeerFingerPrint())
        }
    }

    private val deviceClient: DeviceClient by lazy {
        HiWear.getDeviceClient(context)
    }

    private val audioPlayer: AudioPlayer by lazy {
        AudioPlayer(context)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getSelectedDevice()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMessagingBinding.inflate(inflater, container, false)
        viewLogsBinding = ViewLogsBinding.bind(binding.root)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setViewModelObservers()
    }

    private fun initViews() {
        binding.clearLogs.setOnClickListener {
            viewModel.clearLogs()
        }

        binding.pingDevice.setOnClickListener {
            viewModel.pingDevice()
        }

        binding.sendMessage.setOnClickListener {
            viewModel.sendMessage()
        }

        binding.takePhoto.setOnClickListener {
            viewModel.takePhoto()
        }

        binding.debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDebugMode(isChecked)
        }
    }

    private fun pingSelectedDevice() {
        if (selectedDevice?.isConnected == true) {
            pingDevice(selectedDevice)
        } else {
            logErrorAndGoToDashboard()
        }
    }

    private fun pingDevice(device: Device?) {
        p2pClient.ping(device) { result ->
            "${Date()} Ping ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Result: $result".logResult()
        }.addOnSuccessListener {
            "Pinged ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} succeed".logResult()
        }.addOnFailureListener { exception ->
            "Ping ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} failed".logError(
                exception
            )
        }
    }

    private fun setViewModelObservers() {
        viewModel.selectedDevice.observeEvent(this) { lastSelectedDevice ->
            selectDevice(lastSelectedDevice)
        }

        viewModel.clearLogs.observeEvent(this) {
            viewLogsBinding.logOutputTextView.text = ""
        }

        viewModel.pingDevice.observeEvent(this) {
            pingSelectedDevice()
        }

        viewModel.sendMessage.observeEvent(this) {
            sendMessage("Hi from Huawei Phone! Time: ${System.currentTimeMillis()}ms")
        }

        viewModel.takePhoto.observeEvent(this) {
            requestTakePhotosPermissions.launch(TAKE_PHOTO_PERMISSIONS)
        }

        viewModel.setDebugMode.observeEvent(this) { isChecked ->
            viewLogsBinding.scrollView.isVisible = isChecked
            binding.clearLogs.isVisible = isChecked
        }
    }

    private fun selectDevice(lastSelectedDevice: Device) {
        lifecycleScope.launch {
            deviceClient.bondedDevices.await().let { devices ->
                devices.firstOrNull { device -> device.uuid == lastSelectedDevice.uuid }
                    ?.let { selectedDevice = lastSelectedDevice }
            }
        }
    }

    private fun sendMessage(text: String) {
        if (selectedDevice?.isConnected == true) {
            buildMessageAndSend(text)
        } else {
            logErrorAndGoToDashboard()
        }
    }

    private fun initResultCallbackAndSender() {
        sendCallback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                "Send message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Result: $resultCode".logResult()
            }

            override fun onSendProgress(progress: Long) {
                "Send message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Progress: $progress".logResult()
            }
        }
        "Send Result Callback Registered!".logResult()
    }

    private fun buildMessageAndSend(text: String) {
        Message.Builder()
            .setPayload(text.toByteArray())
            .build().let { message ->
                p2pClient.send(selectedDevice, message, sendCallback).addOnSuccessListener {
                    "Sent message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()}".logResult()
                }.addOnFailureListener {
                    "Send message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} failed".logResult()
                }
            }
    }

    private fun sendFile(file: File) {
        if (selectedDevice?.isConnected == true) {
            buildFileMessageBuilderAndSend(file, sendCallback)
        } else {
            logErrorAndGoToDashboard()
        }
    }

    private fun buildFileMessageBuilderAndSend(file: File, sendCallback: SendCallback?) {
        Message.Builder()
            .setPayload(file)
            .build().let { message ->
                p2pClient.send(selectedDevice, message, sendCallback).addOnSuccessListener {
                    "Sent file to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()}".logResult()
                }.addOnFailureListener {
                    "Send file to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} failed".logResult()
                }
            }
    }

    private fun registerReceiver(selectedDevice: Device?) {
        val receiver = Receiver { message ->
            message?.let {
                when (it.type) {
                    Message.MESSAGE_TYPE_DATA -> "Received Text Message: ${String(message.data)}".logResult()
                    Message.MESSAGE_TYPE_FILE -> "Received file".logResult()
                    Message.MESSAGE_TYPE_DEFAULT -> "Received default message".logResult()
                }
            }
        }
        p2pClient.registerReceiver(selectedDevice, receiver)
            .addOnSuccessListener {
                "Register receiver listener succeed!".logResult()
                initResultCallbackAndSender()
            }
            .addOnFailureListener { "Register receiver listener failed!".logResult() }
    }

    private fun displayNotificationOnWearable(title: String, text: String) {
        if (selectedDevice?.isConnected == true) {
            val notification = Notification.Builder()
                .setPackageName(credentialsProvider.getPeerPkgName())
                .setTemplateId(NotificationTemplate.NOTIFICATION_TEMPLATE_ONE_BUTTON)
                .setButtonContents(
                    hashMapOf(
                        Pair(
                            NotificationConstants.BUTTON_ONE_CONTENT_KEY,
                            "Okay"
                        )
                    )
                )
                .setTitle(title)
                .setText(text)
                .build()

            HiWear.getNotifyClient(context).let { client ->
                client.notify(selectedDevice, notification).addOnSuccessListener {
                    "Send notification successfully!".logResult()
                }.addOnFailureListener { e ->
                    "Failed to send notification".logError(e)
                }
            }
        } else {
            logErrorAndGoToDashboard()
        }
    }

    private fun logErrorAndGoToDashboard() {
        "Lost connection with the device".logResult()
        findNavController().navigateUp()
    }

    private fun String.logResult() {
        appendOnOutputView(this)
        Timber.d(this)
    }

    private fun String.logError(exception: Exception) {
        appendOnOutputView(this)
        Timber.e(exception, this)
    }

    private fun appendOnOutputView(text: String) {
        requireActivity().runOnUiThread {
            viewLogsBinding.logOutputTextView.append(text + System.lineSeparator())
            viewLogsBinding.logOutputTextView.post {
                viewLogsBinding.scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}
