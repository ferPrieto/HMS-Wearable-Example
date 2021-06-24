package com.fprieto.hms.wearable.data

import com.huawei.wearengine.device.Device

interface DeviceLocalSource {
    var devices: List<Device>
    var selectedDevice: Device
    fun hasDevicesValidData(): Boolean
}

object DeviceLocalSourceImpl : DeviceLocalSource {

    override var devices: List<Device> = emptyList()
        set(value) {
            hasDevicesValidData = true
            field = value
        }

    override var selectedDevice: Device = getEmptyDevice()

    private var hasDevicesValidData = false

    override fun hasDevicesValidData(): Boolean = hasDevicesValidData

    private fun getEmptyDevice() = Device().apply {
        uuid = ""
        name = ""
    }
}
