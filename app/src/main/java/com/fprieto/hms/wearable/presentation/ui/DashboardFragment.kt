package com.fprieto.hms.wearable.presentation.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.huawei.hihealth.error.HiHealthError
import com.huawei.hihealthkit.auth.HiHealthAuth.requestAuthorization
import com.huawei.hihealthkit.data.HiHealthExtendScope
import com.huawei.hihealthkit.data.HiHealthKitConstant
import com.huawei.hihealthkit.data.store.HiHealthDataStore
import com.huawei.hihealthkit.data.store.HiRealTimeListener
import com.huawei.hihealthkit.data.store.HiSportDataCallback
import com.huawei.hms.common.ApiException
import com.huawei.hms.hihealth.DataController
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.data.DataType.*
import com.huawei.hms.hihealth.data.HealthDataTypes
import com.huawei.hms.hihealth.data.HealthDataTypes.DT_INSTANTANEOUS_SPO2
import com.huawei.hms.hihealth.data.SamplePoint
import com.huawei.hms.hihealth.data.SampleSet
import com.huawei.hms.support.api.entity.auth.Scope
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

private val extendedScopes = arrayListOf(
    Scope(HiHealthExtendScope.HEALTHKIT_EXTEND_SPORT_READ)
)

class DashboardFragment @Inject constructor(
    viewModelFactory: ViewModelProvider.Factory
) : Fragment(R.layout.fragment_dashboard) {

    private val viewModel by viewModels<DashboardViewModel> { viewModelFactory }

    private lateinit var binding: FragmentDashboardBinding
    private lateinit var viewLogsBinding: ViewLogsBinding
    private val credentialsProvider: CredentialsProvider = CredentialsProvider()

    private var sendCallback: SendCallback? = null

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
        HuaweiHiHealth.getDataController(requireContext())
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
    }

    override fun onPause() {
        super.onPause()
        sendCallback = null
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
            device.uuid?.let { registerP2pClientReceiver(device) }
            readTodaySummation(device)
            readLatestData(device)
            getRealData()
        }
    }

    private fun getRealData() {
        HiHealthDataStore.startSport(
            context, HiHealthKitConstant.SPORT_TYPE_RUN
        ) { p0, p1 ->
            if (p0 == HiHealthError.SUCCESS) {
                "start sport success:$p0".logResult()
                registerSportData()
                startReadingHeartRate()
            }
        }
    }

    private fun registerSportData() {
        HiHealthDataStore.registerSportData(requireContext(), object : HiSportDataCallback {
            override fun onResult(resultCode: Int) {
                "registerSportData onResult resultCode:$resultCode".logResult()
            }

            override fun onDataChanged(state: Int, bundle: Bundle) {
                "registerSportData onChange state: $state".logResult()
                if (state == 2) {
                    "distance : " + bundle[HiHealthKitConstant.BUNDLE_KEY_DISTANCE].toString()
                        .logResult()
                    "duration : " + bundle[HiHealthKitConstant.BUNDLE_KEY_DURATION].toString()
                        .logResult()
                    "calorie : " + bundle[HiHealthKitConstant.BUNDLE_KEY_CALORIE].toString()
                        .logResult()
                    "totalSteps : " + bundle[HiHealthKitConstant.BUNDLE_KEY_TOTAL_STEPS].toString()
                        .logResult()
                    "speed : " + bundle[HiHealthKitConstant.BUNDLE_KEY_SPEED].toString().logResult()
                    "totalCreep : " + bundle[HiHealthKitConstant.BUNDLE_KEY_TOTAL_CREEP].toString()
                        .logResult()
                    "totalDescent : " + bundle[HiHealthKitConstant.BUNDLE_KEY_TOTAL_DESCENT].toString()
                        .logResult()
                }
            }
        })
    }

    private fun startReadingHeartRate() {
        HiHealthDataStore.startReadingHeartRate(context, object : HiRealTimeListener {
            override fun onResult(state: Int) {
                "ReadingHeartRate onResult state:$state".logResult()
            }

            override fun onChange(resultCode: Int, value: String) {
                "startReadingHeartRate onChange resultCode: $resultCode value: $value".logResult()
                if (resultCode == HiHealthError.SUCCESS) {
                    try {
                        val jsonObject = JSONObject(value)
                        "hri_info : ${jsonObject.getInt("hri_info")}"
                        "hr_info :  ${jsonObject.getInt("hr_info")}"
                        "hrsqi_info :   ${jsonObject.getInt("hrsqi_info")}"
                        "time_info :  ${jsonObject.getLong("time_info")}"
                    } catch (e: JSONException) {
                        "JSONException e ${e.message}"
                    }
                }
            }
        })
    }

    private fun registerP2pClientReceiver(device: Device) {
        if (binding.deviceRadioGroup.childCount > 0) {
            getRadioButtonBy(device.uuid).let { radioButton ->
                if (device.isConnected) {
                    p2pClient.registerReceiver(device, registerReceiver())
                        .addOnSuccessListener {
                            "Register receiver listener succeed!".logResult()
                            radioButton.isChecked = true
                            initResultCallbackAndSender(device)
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

    private fun readTodaySummation(device: Device) {
        listOf(
            DT_CONTINUOUS_STEPS_DELTA,
            DT_CONTINUOUS_CALORIES_BURNT
        ).map { dataType ->
            try {
                dataController.readTodaySummation(dataType)
                    .addOnSuccessListener { sampleSet ->
                        showTodaySummationData(dataType, sampleSet, device)
                    }
                    .addOnFailureListener { error ->
                        "There was an error retrieving Today's Summation ${error.message}".logResult()
                    }
            } catch (e: Exception) {
                "Failed reading Today's Summation: ${e.message}".logError(e)
            }
        }
    }

    private fun showTodaySummationData(
        dataType: DataType?,
        sampleSet: SampleSet,
        device: Device
    ) {
        if (sampleSet.isEmpty) {
            showZeroValue(dataType, device)
        } else {
            populateTodaySummationResults(sampleSet, device)
        }
    }

    private fun showZeroValue(dataType: DataType?, device: Device) {
        when (dataType) {
            DT_CONTINUOUS_STEPS_TOTAL -> {
                sendHealthDataToConnectedDevice(null, HealthData.Steps, device)
                populateSteps(null)
            }
            DT_CONTINUOUS_CALORIES_BURNT_TOTAL -> {
                sendHealthDataToConnectedDevice(null, HealthData.Calories, device)
                populateCalories(null)
            }
        }
    }

    private fun populateTodaySummationResults(sampleSet: SampleSet, device: Device) {
        sampleSet.samplePoints.map { samplePoint ->
            "Today's SampleSet for ${samplePoint.dataType} retrieved from HMS Core got".logResult()
            when (samplePoint.dataType) {
                DT_CONTINUOUS_STEPS_TOTAL -> {
                    sendHealthDataToConnectedDevice(samplePoint, HealthData.Steps, device)
                    populateSteps(samplePoint)
                }
                DT_CONTINUOUS_CALORIES_BURNT_TOTAL -> {
                    sendHealthDataToConnectedDevice(samplePoint, HealthData.Calories, device)
                    populateCalories(samplePoint)
                }
            }
            logTodaySummationResultsData(samplePoint)
        }
    }

    private fun readLatestData(device: Device) {
        lifecycleScope.launchWhenResumed {
            try {
                dataController.readLatestData(
                    listOf(
                        DT_INSTANTANEOUS_HEART_RATE,
                        DT_INSTANTANEOUS_SPO2
                    )
                ).await().let { results ->
                    results.keys.map { dataType ->
                        when (dataType) {
                            DT_INSTANTANEOUS_HEART_RATE -> {
                                sendHealthDataToConnectedDevice(
                                    results[DT_INSTANTANEOUS_HEART_RATE],
                                    HealthData.HeartRate,
                                    device
                                )
                                populateHeartRate(results[DT_INSTANTANEOUS_HEART_RATE])
                            }
                            DT_INSTANTANEOUS_SPO2 -> {
                                sendHealthDataToConnectedDevice(
                                    results[DT_INSTANTANEOUS_SPO2],
                                    HealthData.Oxygen,
                                    device
                                )
                                populateOxygenInBlood(results[DT_INSTANTANEOUS_SPO2])
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                "Failed reading Latest Data: ${e.message}".logError(e)
            }
        }
    }

    private fun populateSteps(samplePoint: SamplePoint?) {
        binding.stepsProgress.isVisible = false
        binding.stepsValue.isVisible = true
        samplePoint?.let {
            binding.stepsValue.text = it.fieldValues.values.first().toString()
        } ?: run {
            binding.stepsValue.text = "0"
        }
    }

    private fun populateCalories(samplePoint: SamplePoint?) {
        binding.caloriesProgress.isVisible = false
        binding.caloriesValue.isVisible = true
        samplePoint?.let {
            binding.caloriesValue.text = it.fieldValues.values.first().toString()
        } ?: run {
            binding.caloriesValue.text = "0"
        }
    }

    private fun populateHeartRate(samplePoint: SamplePoint?) {
        binding.heartRateProgress.isVisible = false
        binding.heartRateValue.isVisible = true
        binding.heartRateValue.text = samplePoint?.fieldValues?.values?.first().toString()
    }

    private fun populateOxygenInBlood(samplePoint: SamplePoint?) {
        binding.oxygenProgress.isVisible = false
        binding.oxygenValue.isVisible = true
        binding.oxygenValue.text = samplePoint?.fieldValues?.values?.last().toString()
    }

    private fun logTodaySummationResultsData(samplePoint: SamplePoint) {
        samplePoint.dataType.fields.map { field ->
            "Field: ${field.name}  Value: ${samplePoint.getFieldValue(field)}".logResult()
        }
    }

    private fun getPayload(samplePoint: SamplePoint?, healthData: HealthData): ByteArray =
        when (healthData) {
            HealthData.Calories -> "Calories"
            HealthData.HeartRate -> "HeartRate"
            HealthData.Steps -> "Steps"
            else -> "Oxygen"
        }.let { healthDataType ->
            val healthDataValue = if (healthData == HealthData.Oxygen) {
                samplePoint?.fieldValues?.values?.last().toString()
            } else {
                samplePoint?.fieldValues?.values?.first().toString()
            }
            "$healthDataType - $healthDataValue".toByteArray()
        }

    private fun sendHealthDataToConnectedDevice(
        samplePoint: SamplePoint?,
        healthData: HealthData,
        device: Device
    ) {
        device.uuid?.let {
            Message.Builder()
                .setPayload(getPayload(samplePoint, healthData))
                .build().let { message ->
                    p2pClient.send(device, message, sendCallback).addOnSuccessListener {
                        "Sent message to ${device.name}'s ${credentialsProvider.getPeerPkgName()}".logResult()
                    }.addOnFailureListener {
                        "Send message to ${device.name}'s ${credentialsProvider.getPeerPkgName()} failed".logResult()
                    }
                }
        }
    }

    private fun initResultCallbackAndSender(device: Device) {
        sendCallback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                "Send message to ${device.name}'s ${credentialsProvider.getPeerPkgName()} Result: $resultCode".logResult()
            }

            override fun onSendProgress(progress: Long) {
                "Send message to ${device.name}'s ${credentialsProvider.getPeerPkgName()} Progress: $progress".logResult()
            }
        }
        "Send HealthData Callback Registered!".logResult()
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

    enum class HealthData {
        Calories, Steps, HeartRate, Oxygen
    }
}