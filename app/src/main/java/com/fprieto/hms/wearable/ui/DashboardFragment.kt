package com.fprieto.hms.wearable.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.credentials.CredentialsProvider
import com.fprieto.hms.wearable.databinding.FragmentDashboardBinding
import com.fprieto.hms.wearable.databinding.ViewLogsBinding
import com.fprieto.hms.wearable.extensions.await
import com.fprieto.hms.wearable.extensions.toLocalData
import com.fprieto.hms.wearable.model.local.LocalDataMessage
import com.fprieto.hms.wearable.model.local.LocalMessageType
import com.fprieto.hms.wearable.model.local.LocalPlayerCommand
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import kotlinx.coroutines.launch
import timber.log.Timber

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private lateinit var binding: FragmentDashboardBinding
    private lateinit var viewLogsBinding: ViewLogsBinding
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        viewLogsBinding = ViewLogsBinding.bind(binding.root)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.deviceRadioGroup.clearCheck()
        binding.deviceRadioGroup.removeAllViews()
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
            viewLogsBinding.logOutputTextView.text = ""
        }

        binding.messaging.setOnClickListener {
            navigateTo(LocalMessageType.TextMessage)
        }

        binding.mediaPlayer.setOnClickListener {
            navigateTo(LocalMessageType.PlayerCommand)
        }
        binding.deviceRadioGroup.setOnCheckedChangeListener { radioGroup: RadioGroup, checkedId: Int ->
            selectRadioButton(radioGroup, checkedId)
        }
        binding.debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewLogsBinding.scrollView.isVisible = isChecked
            binding.clearLogs.isVisible = isChecked
        }
    }

    private fun selectRadioButton(radioGroup: RadioGroup, checkedId: Int) {
        if (radioGroup.childCount > 0 && checkedId != -1) {
            radioGroup.children.first { childrenView ->
                childrenView.id == checkedId
            }.let { childrenViewFound ->
                registerP2pClientReceiver(registerReceiver(), childrenViewFound as RadioButton)
            }
        }
    }

    private fun getAvailableDevices() {
        lifecycleScope.launch {
            try {
                deviceClient.bondedDevices.await().let { devices ->
                    if (devices.isNotEmpty()) {
                        "Bonded Devices onSuccess! devices list size = ${devices.size}".logResult()
                        updateDeviceList(devices)
                    } else {
                        "Devices list is null or empty".logResult()
                    }
                }
            } catch (e: Exception) {
                "Failed to get of bounded devices: ${e.message}".logError(e)
            }
        }
    }

    private fun updateDeviceList(devices: List<Device>) {
        binding.deviceRadioGroup.removeAllViews()
        devices.map { device ->
            "device Name: ${device.name}".logResult()
            "device connect status: ${device.isConnected}".logResult()
            getRadioButton(device).let { radioButton ->
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

    private fun checkSelectedDevice(): Device? =
            binding.deviceRadioGroup.checkedRadioButtonId.let { buttonId ->
                if (buttonId != 1) {
                    (binding.deviceRadioGroup.findViewById<RadioButton>(buttonId)?.getTag(R.id.device_tag) as? String?)?.let { deviceUUId ->
                        getDeviceFromUDID(deviceUUId)
                    } ?: run { null }
                } else null
            }

    private fun getDeviceFromUDID(uuid: String): Device? =
            connectedDevices.firstOrNull { device -> device.uuid == uuid }

    private fun registerReceiver() = Receiver { message ->
        message?.let { msg ->
            when (msg.type) {
                Message.MESSAGE_TYPE_DATA -> {
                    "Received Text Message: $msg.data".logResult()
                    navigateTo(msg.toLocalData())
                }
                Message.MESSAGE_TYPE_FILE -> {
                    "Received file".logResult()
                    navigateTo(msg.toLocalData())
                }
                Message.MESSAGE_TYPE_DEFAULT -> "Received default message".logResult()
            }
        } ?: run { "Failed to manage the message on Dashboard".logResult() }
    }

    private fun registerP2pClientReceiver(receiver: Receiver, radioButton: RadioButton) {
        checkSelectedDevice().let { device ->
            device?.let {
                if (device.isConnected) {
                    p2pClient.registerReceiver(device, receiver)
                            .addOnSuccessListener {
                                "Register receiver listener succeed!".logResult()
                                radioButton.isChecked = true
                            }
                            .addOnFailureListener {
                                "Register receiver listener failed!".logError(it)
                                radioButton.isChecked = false
                            }
                } else {
                    radioButton.isChecked = false
                    "The device seems to be disconnected".logResult()
                }
            } ?: run {
                radioButton.isChecked = false
                "Click on get Devices button again".logResult()
            }
        }
    }

    private fun navigateTo(localDataMessage: LocalDataMessage) {
        checkSelectedDevice()?.let { device ->
            when (getDestination(localDataMessage.messageType)) {
                Destination.Messaging -> DashboardFragmentDirections.actionDashboardFragmentToMessagingFragment(device.uuid)
                else -> DashboardFragmentDirections.actionDashboardFragmentToPlayerFragment(device.uuid, localDataMessage.playerCommand
                        ?: LocalPlayerCommand.Play)
            }.let { findNavController().navigate(it) }
        } ?: run { "No device selected, get available devices again".logResult() }
    }

    private fun navigateTo(messageType: LocalMessageType) {
        checkSelectedDevice()?.let { device ->
            when (getDestination(messageType)) {
                Destination.Messaging -> DashboardFragmentDirections.actionDashboardFragmentToMessagingFragment(device.uuid)
                else -> DashboardFragmentDirections.actionDashboardFragmentToPlayerFragment(device.uuid, LocalPlayerCommand.Play)
            }.let { findNavController().navigate(it) }
        } ?: run { "No device selected, get available devices again".logResult() }
    }

    private fun getDestination(messageType: LocalMessageType) = when (messageType) {
        LocalMessageType.PlayerCommand -> Destination.MediaPlayer
        else -> Destination.Messaging
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

enum class Destination {
    Messaging,
    MediaPlayer
}
