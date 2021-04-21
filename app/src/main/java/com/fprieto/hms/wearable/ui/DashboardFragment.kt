package com.fprieto.hms.wearable.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.credentials.CredentialsProvider
import com.fprieto.hms.wearable.databinding.FragmentDashboardBinding
import com.fprieto.hms.wearable.extensions.await
import com.fprieto.hms.wearable.mapper.RemoteDataMessageToLocalMapper
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
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
            binding.logOutputTextView.text = ""
        }

        binding.messaging.setOnClickListener {
            navigateTo(Destination.Messaging)
        }

        binding.mediaPlayer.setOnClickListener {
            navigateTo(Destination.MediaPlayer)
        }
        binding.deviceRadioGroup.setOnCheckedChangeListener { radioGroup: RadioGroup, checkedId: Int ->
            if (radioGroup.childCount > 0 && checkedId != -1) {
                radioGroup.children.first { childrenView ->
                    childrenView.id == checkedId
                }.let { childrenViewFound ->
                    registerP2pClientReceiver(registerReceiver(), childrenViewFound as RadioButton)
                }
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
        devices.map { device ->
            printResultOnUIThread("device Name: " + device.name)
            printResultOnUIThread("device connect status:" + device.isConnected)
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

    private fun printResultOnUIThread(string: String) = requireActivity().runOnUiThread {
        binding.logOutputTextView.append(string + System.lineSeparator())
        binding.logOutputTextView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
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
                    navigateTo(Destination.MediaPlayer)
                }
                Message.MESSAGE_TYPE_DEFAULT -> printResultOnUIThread("Received default message")
            }

        } ?: run {
            printResultOnUIThread("Failed to manage the message ")
            Timber.d("Failed to manage the message on Dashboard")
        }
    }

    private fun registerP2pClientReceiver(receiver: Receiver, radioButton: RadioButton) {
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
                radioButton.isChecked = false
                printResultOnUIThread("Click on get Devices button again")
            }
        }
    }

    private fun manageNavigation(msg: Message) {
        RemoteDataMessageToLocalMapper().toLocalDataMessage(msg).let { localDataMessage ->
            navigateTo(localDataMessage)
        }
    }

    private fun navigateTo(localDataMessage: LocalDataMessage) {
        checkSelectedDevice()?.let { device ->
            val action = when (getDestination(localDataMessage)) {
                Destination.Messaging -> DashboardFragmentDirections.actionDashboardFragmentToMessagingFragment(device.uuid)
                else -> DashboardFragmentDirections.actionDashboardFragmentToPlayerFragment(device.uuid, localDataMessage.playerCommand
                        ?: LocalPlayerCommand.Play)
            }
            findNavController().navigate(action)
        } ?: run {
            printResultOnUIThread("No device selected, get available devices again")
        }
    }

    private fun getDestination(dataMessage: LocalDataMessage) = when (dataMessage.messageType) {
        LocalMessageType.PlayerCommand -> Destination.MediaPlayer
        else -> Destination.Messaging
    }
}

enum class Destination {
    Messaging,
    MediaPlayer
}
