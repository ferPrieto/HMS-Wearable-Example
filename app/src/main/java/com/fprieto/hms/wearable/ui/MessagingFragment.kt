package com.fprieto.hms.wearable.ui

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.audio.AudioPlayer
import com.fprieto.hms.wearable.credentials.CredentialsProvider
import com.fprieto.hms.wearable.databinding.FragmentMessagingBinding
import com.fprieto.hms.wearable.extensions.await
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*

private val TAKE_PHOTO_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)

class MessagingFragment : Fragment(R.layout.fragment_messaging) {

    private lateinit var binding: FragmentMessagingBinding
    private val credentialsProvider: CredentialsProvider = CredentialsProvider()
    private var selectedDevice: Device? = null
        set(value) {
            field = value
            setDeviceIntoRadioButton(value)
            registerReceiver()
        }

    private fun setDeviceIntoRadioButton(value: Device?) {
        binding.connectedDevice.text = value?.name
        binding.connectedDevice.isChecked = true
    }

    private val requestTakePhotosPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.entries.all { true }) {
            takePicture.launch(null)
        }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        File.createTempFile("temp", ".jpg", requireActivity().cacheDir).let { file ->
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
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
        registerReceiver()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMessagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reSelectDevice()
        initButtons()
    }

    private fun reSelectDevice() {
        getDeviceUuId().let { uuId ->
            lifecycleScope.launch {
                try {
                    deviceClient.bondedDevices.await().let { devices ->
                        if (devices.isNullOrEmpty()) {
                            Toast.makeText(context, "The device selected is unavailable", Toast.LENGTH_SHORT).show()
                            Timber.e("The device selected is unavailable")
                            findNavController().navigateUp()
                            return@launch
                        }
                        devices.firstOrNull { device -> device.uuid == uuId }?.let { device ->
                            selectedDevice = device
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error getting available devices", Toast.LENGTH_SHORT).show()
                    Timber.e(e, "Error getting available devices: ${e.message}")
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun getDeviceUuId(): String {
        val args: MessagingFragmentArgs by navArgs()
        return args.deviceUuid
    }

    private fun initButtons() {
        binding.clearLogs.setOnClickListener {
            binding.logOutputTextView.text = ""
        }

        binding.pingDevice.setOnClickListener {
            pingBoundDevices()
        }

        binding.sendMessage.setOnClickListener {
            sendMessage("Hi from Huawei Phone! Time: ${System.currentTimeMillis()}ms")
        }

        binding.takePhoto.setOnClickListener {
            requestTakePhotosPermissions.launch(TAKE_PHOTO_PERMISSIONS)
        }
    }

    private fun printResultOnUIThread(string: String) = requireActivity().runOnUiThread {
        binding.logOutputTextView.append(string + System.lineSeparator())
        binding.logOutputTextView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun pingBoundDevices() {
        if (selectedDevice?.isConnected == false) {
            printResultOnUIThread("Click on get Devices button again")
            return
        }

        p2pClient.ping(selectedDevice) { result ->
            val text = "${Date()} Ping ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Result: $result"
            printResultOnUIThread(text)
        }.addOnSuccessListener {
            printResultOnUIThread("Pinged ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} succeed")
        }.addOnFailureListener {
            printResultOnUIThread("Ping ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} failed")
        }
    }

    private fun sendMessage(text: String) {
        if (selectedDevice?.isConnected == false) {
            printResultOnUIThread("Click on get Devices button again")
            return
        }

        val sendCallback: SendCallback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                printResultOnUIThread("Send message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Result: $resultCode")
            }

            override fun onSendProgress(progress: Long) {
                printResultOnUIThread("Send message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Progress: $progress")
            }
        }

        Message.Builder()
                .setPayload(text.toByteArray())
                .build().let { message ->
                    p2pClient.send(selectedDevice, message, sendCallback).addOnSuccessListener {
                        printResultOnUIThread("Sent message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()}")
                    }.addOnFailureListener {
                        printResultOnUIThread("Send message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} failed")
                    }
                }
    }

    private fun sendFile(file: File) {
        if (selectedDevice?.isConnected == false) {
            printResultOnUIThread("Click on get Devices button again")
            return
        }

        val sendCallback: SendCallback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                printResultOnUIThread("Send file to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Result: $resultCode")
            }

            override fun onSendProgress(progress: Long) {
                printResultOnUIThread("Send file to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Progress: $progress")
            }
        }

        initMessageBuilder(file, sendCallback)
    }

    private fun initMessageBuilder(file: File, sendCallback: SendCallback) {
        Message.Builder()
                .setPayload(file)
                .build().let { message ->
                    p2pClient.send(selectedDevice, message, sendCallback).addOnSuccessListener {
                        printResultOnUIThread("Sent file to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()}")
                    }.addOnFailureListener {
                        printResultOnUIThread("Send file to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} failed")
                    }
                }
    }

    private fun registerReceiver() {
        val receiver = Receiver { message ->
            message?.let {
                when (it.type) {
                    Message.MESSAGE_TYPE_DATA -> {
                        val data = String(message.data)
                        printResultOnUIThread("Received Text Message: $data")
                    }
                    Message.MESSAGE_TYPE_FILE -> {
                        printResultOnUIThread("Received file")
                    }
                    Message.MESSAGE_TYPE_DEFAULT -> printResultOnUIThread("Received default message")
                }
            }
        }
        p2pClient.registerReceiver(selectedDevice, receiver)
                .addOnSuccessListener { printResultOnUIThread("Register receiver listener succeed!") }
                .addOnFailureListener { printResultOnUIThread("Register receiver listener failed!") }
    }
}
