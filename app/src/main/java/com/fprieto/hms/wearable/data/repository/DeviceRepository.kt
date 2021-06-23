package com.fprieto.hms.wearable.data.repository

import com.fprieto.hms.wearable.data.DeviceLocalSource
import com.huawei.wearengine.device.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

interface DeviceRepository {
    suspend fun setSelectedDeviceBy(uuId: String): Flow<Device>
    suspend fun setSelectedDevice(device: Device)
    suspend fun setFoundDevices(devices: List<Device>)
    suspend fun getSelectedDevice(): Flow<Device>
    suspend fun getLastFoundDevices(): List<Device>
}

class DeviceRepositoryImpl @Inject constructor(
    private val deviceLocalSource: DeviceLocalSource
) : DeviceRepository {
    private val _selectedDeviceSharedFlow = MutableStateFlow(getEmptyDevice())
    private val selectedDeviceSharedFlow = _selectedDeviceSharedFlow.asSharedFlow()
    override suspend fun setSelectedDeviceBy(uuId: String): Flow<Device> {
        deviceLocalSource.devices.firstOrNull { device ->
            device.uuid == uuId
        }?.let { device ->
            deviceLocalSource.selectedDevice = device
            _selectedDeviceSharedFlow.emit(device)
        } ?: IllegalStateException("The device has not been found")
        return selectedDeviceSharedFlow.distinctUntilChanged()
    }

    private fun getEmptyDevice() = Device().apply {
        name = ""
        uuid = ""
    }

    override suspend fun setSelectedDevice(device: Device) {
        deviceLocalSource.selectedDevice = device
    }

    override suspend fun setFoundDevices(devices: List<Device>) {
        deviceLocalSource.devices = devices
    }

    override suspend fun getSelectedDevice(): Flow<Device> {
        _selectedDeviceSharedFlow.emit(deviceLocalSource.selectedDevice)
        return selectedDeviceSharedFlow.distinctUntilChanged()
    }

    override suspend fun getLastFoundDevices(): List<Device> = deviceLocalSource.devices
}
