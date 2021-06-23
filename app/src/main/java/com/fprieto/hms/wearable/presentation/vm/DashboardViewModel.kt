package com.fprieto.hms.wearable.presentation.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fprieto.hms.wearable.data.repository.DeviceRepository
import com.huawei.wearengine.device.Device
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

abstract class DashboardViewModel : ViewModel() {
    abstract fun selectDevice(device: Device)
    abstract fun selectDeviceBy(uuId: String)
    abstract fun setFoundDevices(devices: List<Device>)
    abstract fun getLastFoundDevices()

    abstract val selectedDevice: LiveData<Event<Device>>
    abstract val lastFoundDevices: LiveData<Event<List<Device>>>
}

class DashboardViewModelImpl @Inject constructor(
    private val deviceRepository: DeviceRepository
) : DashboardViewModel() {

    private val _selectedDevice = MediatorLiveData<Event<Device>>()
    private val _lastFoundDevices = MediatorLiveData<Event<List<Device>>>()

    override val selectedDevice: LiveData<Event<Device>>
        get() = _selectedDevice

    override val lastFoundDevices: LiveData<Event<List<Device>>>
        get() = _lastFoundDevices

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception)
    }

    override fun selectDevice(device: Device) {
        viewModelScope.launch {
            deviceRepository.setSelectedDevice(device)
        }
    }

    override fun selectDeviceBy(uuId: String) {
        viewModelScope.launch(errorHandler) {
            deviceRepository.setSelectedDeviceBy(uuId)
                .collectLatest { device ->
                    _selectedDevice.postValue(eventOf(device))
                }
        }
    }

    override fun setFoundDevices(devices: List<Device>) {
        viewModelScope.launch {
            deviceRepository.setFoundDevices(devices)
        }
    }

    override fun getLastFoundDevices() {
        viewModelScope.launch {
            deviceRepository.getLastFoundDevices().let { devices ->
                _lastFoundDevices.postValue(eventOf(devices))
            }
        }
    }
}