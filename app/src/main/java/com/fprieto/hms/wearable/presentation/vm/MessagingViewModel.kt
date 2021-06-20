package com.fprieto.hms.wearable.presentation.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel

abstract class MessagingViewModel : ViewModel() {
    abstract fun clearLogs()
    abstract fun pingDevice()
    abstract fun sendMessage()
    abstract fun takePhoto()
    abstract fun setDebugMode(isChecked: Boolean)

    abstract val clearLogs: LiveData<Event<Unit>>
    abstract val pingDevice: LiveData<Event<Unit>>
    abstract val sendMessage: LiveData<Event<Unit>>
    abstract val takePhoto: LiveData<Event<Unit>>
    abstract val setDebugMode: LiveData<Event<Boolean>>
}

class MessagingViewModelImpl : MessagingViewModel() {

    private val _clearLogs = MediatorLiveData<Event<Unit>>()
    private val _pingDevice = MediatorLiveData<Event<Unit>>()
    private val _sendMessage = MediatorLiveData<Event<Unit>>()
    private val _takePhoto = MediatorLiveData<Event<Unit>>()
    private val _setDebugMode = MediatorLiveData<Event<Boolean>>()

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