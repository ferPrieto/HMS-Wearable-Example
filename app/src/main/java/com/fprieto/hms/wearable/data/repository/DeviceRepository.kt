package com.fprieto.hms.wearable.data.repository

import com.fprieto.hms.wearable.data.DeviceLocalSource
import com.huawei.wearengine.device.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

private const val DEVICE_CONNECTED = 2

interface DeviceRepository {
    suspend fun setSelectedDeviceBy(uuId: String): Flow<Device>
    suspend fun setFoundDevices(devices: List<Device>): Flow<List<Device>>
    suspend fun getSelectedDevice(): Flow<Device>
    suspend fun getLastFoundDevices(): Flow<List<Device>>
}

class DeviceRepositoryImpl @Inject constructor(
    private val deviceLocalSource: DeviceLocalSource
) : DeviceRepository {
    private val _devicesSharedFlow = MutableStateFlow(emptyList<Device>())
    private val devicesSharedFlow = _devicesSharedFlow.asSharedFlow()

    private val _selectedDeviceSharedFlow = MutableStateFlow(getEmptyDevice())
    private val selectedDeviceSharedFlow = _selectedDeviceSharedFlow.asSharedFlow()
    override suspend fun setSelectedDeviceBy(uuId: String): Flow<Device> {
        deviceLocalSource.devices.firstOrNull { device ->
            device.uuid == uuId
        }?.let { device ->
            device.setConnectState(DEVICE_CONNECTED)
            deviceLocalSource.selectedDevice = device
            _selectedDeviceSharedFlow.emit(device)
        } ?: IllegalStateException("The device has not been found")
        return selectedDeviceSharedFlow.distinctUntilChanged()
    }

    private fun getEmptyDevice() = Device().apply {
        name = ""
        uuid = ""
    }

    override suspend fun setFoundDevices(devices: List<Device>): Flow<List<Device>> {
        deviceLocalSource.devices = devices
        _devicesSharedFlow.emit(devices)
        return devicesSharedFlow.distinctUntilChanged()
    }

    override suspend fun getSelectedDevice(): Flow<Device> {
        _selectedDeviceSharedFlow.emit(deviceLocalSource.selectedDevice)
        return selectedDeviceSharedFlow.distinctUntilChanged()
    }

    override suspend fun getLastFoundDevices(): Flow<List<Device>> {
        _devicesSharedFlow.emit(deviceLocalSource.devices)
        return devicesSharedFlow.distinctUntilChanged()
    }
}
