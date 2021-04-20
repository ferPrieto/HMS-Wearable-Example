package com.fprieto.hms.wearable.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.credentials.CredentialsProvider
import com.fprieto.hms.wearable.databinding.FragmentDashboardBinding
import com.fprieto.hms.wearable.extensions.await
import com.fprieto.hms.wearable.mapper.RemoteDataMessageToLocalMapper
import com.fprieto.hms.wearable.model.local.LocalMessageType
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
import java.util.*

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private lateinit var binding: FragmentDashboardBinding
    private val connectedDevices: MutableList<Device> = mutableListOf()
    private val credentialsProvider: CredentialsProvider = CredentialsProvider()

    private val p2pClient: P2pClient by lazy {
        HiWear.getP2pClient(context).apply {
            setPeerPkgName(credentialsProvider.getPeerPkgName())
            setPeerFingerPrint(credentialsProvider.getPeerFingerPrint())
        }
    }

    private val deviceClient: DeviceClient by lazy {
        HiWear.getDeviceClient(context)
    }

    private fun tryNavigateTo(destination: Destination) {
        val device = checkSelectedDevice()
        device?.let {
            val action = when (destination) {
                Destination.Messaging -> DashboardFragmentDirections.actionDashboardFragmentToMessagingFragment(device.uuid)
                else -> DashboardFragmentDirections.actionDashboardFragmentToPlayerFragment(device.uuid)
            }
            findNavController().navigate(action)
        } ?: run {
            printResultOnUIThread("No device selected, get available devices again")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.getDevices.setOnClickListener {
            getAvailableDevices()
        }

        binding.clearLogs.setOnClickListener {
            binding.logOutputTextView.text = ""
        }

        binding.messaging.setOnClickListener {
            tryNavigateTo(Destination.Messaging)
        }

        binding.mediaPlayer.setOnClickListener {
            tryNavigateTo(Destination.MediaPlayer)
        }
        binding.deviceRadioGroup.setOnCheckedChangeListener { radioGroup: RadioGroup, childIndex: Int ->
            if (radioGroup.childCount > 0 && childIndex != -1) {
                tryToRegisterP2pClientReceiver(registerReceiver(), radioGroup.getChildAt(childIndex - 1) as RadioButton)
            }
        }
    }

    private fun getAvailableDevices() {
        lifecycleScope.launch {
            try {
                deviceClient.bondedDevices.await().let { devices ->
                    if (devices.isNullOrEmpty()) {
                        printResultOnUIThread("Devices list is null or empty")
                        return@launch
                    }
                    printResultOnUIThread("Bonded Devices onSuccess! devices list size = " + devices.size)
                    updateDeviceList(devices)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get of bounded devices: ${e.message}")
                printResultOnUIThread("Bonded Devices task submission error: ${e.message}")
            }
        }
    }

    private fun updateDeviceList(devices: List<Device>) {
        binding.deviceRadioGroup.clearCheck()
        binding.deviceRadioGroup.removeAllViews()

        devices.map { device ->
            printResultOnUIThread("device Name: " + device.name)
            printResultOnUIThread("device connect status:" + device.isConnected)
            getRadioButton(device).let { radioButton ->
                radioButton.isChecked = device.isConnected
                binding.deviceRadioGroup.addView(radioButton)
            }
            connectedDevices.add(device)
        }
    }

    private fun getRadioButton(device: Device) = RadioButton(context).apply {
        id = View.generateViewId()
        text = device.name
        setTag(R.id.device_tag, device.uuid)
    }

    private fun printResultOnUIThread(string: String) = requireActivity().runOnUiThread {
        binding.logOutputTextView.append(string + System.lineSeparator())
        binding.logOutputTextView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun sendFile(file: File) {
        checkSelectedDevice().let { device ->
            if (device == null || !device.isConnected) {
                printResultOnUIThread("Click on get Devices button again")
                return
            }

            val sendCallback: SendCallback = object : SendCallback {
                override fun onSendResult(resultCode: Int) {
                    printResultOnUIThread("Send file to ${device.name}'s ${credentialsProvider.getPeerPkgName()} Result: $resultCode")
                }

                override fun onSendProgress(progress: Long) {
                    printResultOnUIThread("Send file to ${device.name}'s ${credentialsProvider.getPeerPkgName()} Progress: $progress")
                }
            }

            Message.Builder()
                    .setPayload(file)
                    .build().let { message ->
                        p2pClient.send(device, message, sendCallback).addOnSuccessListener {
                            printResultOnUIThread("Sent file to ${device.name}'s ${credentialsProvider.getPeerPkgName()}")
                        }.addOnFailureListener {
                            printResultOnUIThread("Send file to ${device.name}'s ${credentialsProvider.getPeerPkgName()} failed")
                        }
                    }
        }
    }

    private fun checkSelectedDevice(): Device? {
        binding.deviceRadioGroup.checkedRadioButtonId.let { buttonId ->
            if (buttonId == -1) {
                return null
            }

            val deviceUUID = binding.deviceRadioGroup.findViewById<RadioButton>(buttonId)?.getTag(R.id.device_tag)
                    as? String? ?: return null
            return getDeviceFromUDID(deviceUUID)
        }
    }

    private fun getDeviceFromUDID(uuid: String): Device? =
            connectedDevices.firstOrNull { device -> device.uuid == uuid }

    private fun registerReceiver() = Receiver { message ->
        message?.let { msg ->
            when (msg.type) {
                Message.MESSAGE_TYPE_DATA -> {
                    printResultOnUIThread("Received Text Message: $msg.data")
                    manageNavigation(msg)
                }
                Message.MESSAGE_TYPE_FILE -> {
                    printResultOnUIThread("Received file")
                    tryNavigateTo(Destination.MediaPlayer) //todo: remote control ready for recorded file
                }
                Message.MESSAGE_TYPE_DEFAULT -> printResultOnUIThread("Received default message")
            }

        } ?: run {
            printResultOnUIThread("Failed to manage the message ")
            Timber.d("Failed to manage the message on Dashboard")
        }
    }

    private fun tryToRegisterP2pClientReceiver(receiver: Receiver, radioButton: RadioButton) {
        checkSelectedDevice().let { device ->
            device?.let {
                if (device.isConnected) {
                    p2pClient.registerReceiver(device, receiver)
                            .addOnSuccessListener {
                                printResultOnUIThread("Register receiver listener succeed!")
                                radioButton.isChecked = true
                            }
                            .addOnFailureListener {
                                printResultOnUIThread("Register receiver listener failed!")
                                radioButton.isChecked = false
                                printResultOnUIThread("Click on get Devices button again")
                            }
                } else {
                    radioButton.isChecked = false
                    printResultOnUIThread("The device seems to be disconnected")
                }
            } ?: run {
                printResultOnUIThread("Click on get Devices button again")
            }
        }
    }

    private fun manageNavigation(msg: Message) {
        RemoteDataMessageToLocalMapper().toLocalDataMessage(msg).let { localDataMessage ->
            when (localDataMessage.messageType) {
                LocalMessageType.PlayerCommand -> tryNavigateTo(Destination.MediaPlayer)
                else -> tryNavigateTo(Destination.Messaging)
            }
        }
    }
}

enum class Destination {
    Messaging,
    MediaPlayer
}
