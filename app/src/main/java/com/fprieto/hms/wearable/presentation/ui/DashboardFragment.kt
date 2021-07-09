package com.fprieto.hms.wearable.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.credentials.CredentialsProvider
import com.fprieto.hms.wearable.databinding.FragmentDashboardBinding
import com.fprieto.hms.wearable.databinding.ViewLogsBinding
import com.fprieto.hms.wearable.extensions.await
import com.fprieto.hms.wearable.presentation.vm.DashboardViewModel
import com.fprieto.hms.wearable.presentation.vm.observeEvent
import com.huawei.hms.hihealth.DataController
import com.huawei.hms.hihealth.HiHealthOptions
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.data.SamplePoint
import com.huawei.hms.hihealth.data.SampleSet
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.result.AuthHuaweiId
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class DashboardFragment @Inject constructor(
    viewModelFactory: ViewModelProvider.Factory
) : Fragment(R.layout.fragment_dashboard) {

    private val viewModel by viewModels<DashboardViewModel> { viewModelFactory }

    private lateinit var binding: FragmentDashboardBinding
    private lateinit var viewLogsBinding: ViewLogsBinding
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

    private val dataController: DataController by lazy {
        HiHealthOptions.builder()
            .addDataType(DataType.DT_CONTINUOUS_STEPS_DELTA, HiHealthOptions.ACCESS_READ)
            .addDataType(DataType.DT_CONTINUOUS_CALORIES_BURNT_TOTAL, HiHealthOptions.ACCESS_READ)
            .addDataType(
                DataType.POLYMERIZE_CONTINUOUS_ACTIVITY_STATISTICS,
                HiHealthOptions.ACCESS_READ
            )
            .addDataType(DataType.DT_INSTANTANEOUS_LOCATION_TRACE, HiHealthOptions.ACCESS_READ)
            .addDataType(DataType.DT_STATISTICS_SLEEP, HiHealthOptions.ACCESS_READ)
            .addDataType(
                DataType.POLYMERIZE_CONTINUOUS_HEART_RATE_STATISTICS,
                HiHealthOptions.ACCESS_READ
            )
            .addDataType(
                DataType.POLYMERIZE_CONTINUOUS_HEART_RATE_STATISTICS,
                HiHealthOptions.ACCESS_READ
            )
            .build().let { hiHealthOptions ->
                val signInHuaweiId: AuthHuaweiId =
                    HuaweiIdAuthManager.getExtendedAuthResult(hiHealthOptions)
                HuaweiHiHealth.getDataController(requireContext(), signInHuaweiId)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        viewLogsBinding = ViewLogsBinding.bind(binding.root)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.getLastFoundDevices()
        viewModel.getSelectedDevice()
        getHiHealthData()
        readToday()
    }

    private fun getHiHealthData() {
        val hiHealthOptions = HiHealthOptions.builder()
            .addDataType(DataType.DT_CONTINUOUS_STEPS_DELTA, HiHealthOptions.ACCESS_READ)
            .addDataType(DataType.DT_INSTANTANEOUS_BODY_WEIGHT, HiHealthOptions.ACCESS_READ)
            .build()
        val signInHuaweiId = HuaweiIdAuthManager.getExtendedAuthResult(hiHealthOptions)
        HuaweiHiHealth.getDataController(requireContext(), signInHuaweiId)
    }

    private fun readToday() {
        listOf(
            DataType.DT_CONTINUOUS_STEPS_DELTA,
            DataType.DT_CONTINUOUS_CALORIES_BURNT,
            DataType.DT_CONTINUOUS_DISTANCE_DELTA,
            DataType.DT_CONTINUOUS_SLEEP,
            DataType.DT_INSTANTANEOUS_HEART_RATE
        ).map { dataType -> getLatestData(dataType) }

        //TODO: oxygen in blood only possible through dataController.readLatestData(dataType)
    }

    private fun getLatestData(dataType: DataType) {
        dataController.readTodaySummation(dataType)
            .addOnSuccessListener { sampleSet ->
                "Today's SampleSet retrieved from HMS Core got".logResult()
                showSampleSet(sampleSet)
            }
            .addOnFailureListener { error ->
                "There was an error retrieving today's data ${error.message}".logResult()
            }
    }

    private fun showSampleSet(sampleSet: SampleSet) {
        sampleSet.samplePoints.map { samplePoint ->
            when (samplePoint.dataType) {
                DataType.DT_CONTINUOUS_STEPS_DELTA -> "populate Steps Data in UI".logResult()
                DataType.DT_CONTINUOUS_CALORIES_BURNT_TOTAL -> "populate Calories Data in UI".logResult()
                DataType.POLYMERIZE_CONTINUOUS_ACTIVITY_STATISTICS -> "populate Activity Data in UI".logResult()
                DataType.DT_INSTANTANEOUS_LOCATION_TRACE -> "populate Location Data in UI".logResult()
                DataType.DT_STATISTICS_SLEEP -> "populate Sleep Data in UI".logResult()
                DataType.POLYMERIZE_CONTINUOUS_HEART_RATE_STATISTICS -> "populate HeartRate Data in UI".logResult()
            }
            logSamplePointsData(samplePoint)
        }
    }

    private fun logSamplePointsData(samplePoint: SamplePoint) {
        samplePoint.dataType.fields.map { field ->
            "Field: ${field.name}  Value: ${samplePoint.getFieldValue(field)}".logResult()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setViewModelObservers()
    }

    private fun initViews() {
        binding.getDevices.setOnClickListener {
            getAvailableDevices()
        }

        binding.clearLogs.setOnClickListener {
            viewLogsBinding.logOutputTextView.text = ""
        }

        binding.deviceRadioGroup.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            selectRadioButton(checkedId)
        }

        binding.debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewLogsBinding.scrollView.isVisible = isChecked
            binding.clearLogs.isVisible = isChecked
        }
    }

    private fun selectRadioButton(checkedId: Int) {
        if (binding.deviceRadioGroup.childCount > 0 && checkedId != -1) {
            binding.deviceRadioGroup.children.first { childrenView ->
                childrenView.id == checkedId
            }.let { childrenViewFound ->
                viewModel.selectDeviceBy((childrenViewFound as RadioButton).tag as String)
            }
        }
    }

    private fun getAvailableDevices() {
        lifecycleScope.launch {
            try {
                deviceClient.bondedDevices.await().let { devices ->
                    if (devices.isNotEmpty()) {
                        "Bonded Devices onSuccess! devices list size = ${devices.size}".logResult()
                        viewModel.setFoundDevices(devices)
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
        }
    }

    private fun getRadioButton(device: Device) = RadioButton(context).apply {
        id = View.generateViewId()
        text = device.name
        tag = device.uuid
    }

    private fun registerReceiver() = Receiver { message ->
        Timber.d(message.data.toString())
    }

    private fun setViewModelObservers() {
        viewModel.lastFoundDevices.observeEvent(this) { devices ->
            updateDeviceList(devices)
        }

        viewModel.selectedDevice.observeEvent(this) { device ->
            device?.let {
                registerP2pClientReceiver(device)
            }
        }
    }

    private fun registerP2pClientReceiver(device: Device) {
        if (binding.deviceRadioGroup.childCount > 0) {
            getRadioButtonBy(device.uuid).let { radioButton ->
                if (device.isConnected) {
                    p2pClient.registerReceiver(device, registerReceiver())
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
            }
        }
    }

    private fun getRadioButtonBy(uuId: String): RadioButton =
        binding.deviceRadioGroup.findViewWithTag(uuId)

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