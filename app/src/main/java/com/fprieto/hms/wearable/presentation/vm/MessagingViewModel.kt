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

abstract class MessagingViewModel : ViewModel() {
    abstract fun getSelectedDevice()
    abstract fun clearLogs()
    abstract fun pingDevice()
    abstract fun sendMessage()
    abstract fun takePhoto()
    abstract fun setDebugMode(isChecked: Boolean)

    abstract val selectedDevice: LiveData<Event<Device>>
    abstract val clearLogs: LiveData<Event<Unit>>
    abstract val pingDevice: LiveData<Event<Unit>>
    abstract val sendMessage: LiveData<Event<Unit>>
    abstract val takePhoto: LiveData<Event<Unit>>
    abstract val setDebugMode: LiveData<Event<Boolean>>
}

class MessagingViewModelImpl @Inject constructor(
    private val deviceRepository: DeviceRepository
) : MessagingViewModel() {

    private val _selectedDevice = MediatorLiveData<Event<Device>>()
    private val _clearLogs = MediatorLiveData<Event<Unit>>()
    private val _pingDevice = MediatorLiveData<Event<Unit>>()
    private val _sendMessage = MediatorLiveData<Event<Unit>>()
    private val _takePhoto = MediatorLiveData<Event<Unit>>()
    private val _setDebugMode = MediatorLiveData<Event<Boolean>>()

    override val selectedDevice: LiveData<Event<Device>>
        get() = _selectedDevice

    override val clearLogs: LiveData<Event<Unit>>
        get() = _clearLogs

    override val pingDevice: LiveData<Event<Unit>>
        get() = _pingDevice

    override val sendMessage: LiveData<Event<Unit>>
        get() = _sendMessage

    override val takePhoto: LiveData<Event<Unit>>
        get() = _takePhoto

    override val setDebugMode: LiveData<Event<Boolean>>
        get() = _setDebugMode

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception)
    }

    override fun getSelectedDevice() {
        viewModelScope.launch(errorHandler) {
            deviceRepository.getSelectedDevice()
                .collectLatest { device ->
                    _selectedDevice.postValue(eventOf(device))
                }
        }
    }

    override fun clearLogs() {
        _clearLogs.postValue(eventOf(Unit))
    }

    override fun pingDevice() {
        _pingDevice.postValue(eventOf(Unit))
    }

    override fun sendMessage() {
        _sendMessage.postValue(eventOf(Unit))
    }

    override fun takePhoto() {
        _takePhoto.postValue(eventOf(Unit))
    }

    override fun setDebugMode(isChecked: Boolean) {
        _setDebugMode.postValue(eventOf(isChecked))
    }
}