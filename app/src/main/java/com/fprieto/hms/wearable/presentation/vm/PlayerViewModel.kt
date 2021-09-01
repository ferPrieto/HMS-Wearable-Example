package com.fprieto.hms.wearable.presentation.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fprieto.hms.wearable.data.repository.DeviceRepository
import com.fprieto.hms.wearable.model.local.LocalMessageType
import com.fprieto.hms.wearable.model.local.LocalPlayerCommand
import com.fprieto.hms.wearable.presentation.mapper.RemoteDataMessageToLocalMapper
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

abstract class PlayerViewModel : ViewModel() {
    abstract fun getSelectedDevice()
    abstract fun manageReceivedMessage(message: Message)

    abstract val selectedDevice: LiveData<Event<Device>>
    abstract val playVideo: LiveData<Event<Unit>>
    abstract val pauseVideo: LiveData<Event<Unit>>
    abstract val rewindVideo: LiveData<Event<Unit>>
    abstract val fastForwardVideo: LiveData<Event<Unit>>
    abstract val previousVideo: LiveData<Event<Unit>>
    abstract val nextVideo: LiveData<Event<Unit>>
}

class PlayerViewModelImpl @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val remoteDataMessageMapper: RemoteDataMessageToLocalMapper
) : PlayerViewModel() {

    private val _selectedDevice = MediatorLiveData<Event<Device>>()
    private val _playVideo = MediatorLiveData<Event<Unit>>()
    private val _pauseVideo = MediatorLiveData<Event<Unit>>()
    private val _rewindVideo = MediatorLiveData<Event<Unit>>()
    private val _fastForwardVideo = MediatorLiveData<Event<Unit>>()
    private val _previousVideo = MediatorLiveData<Event<Unit>>()
    private val _nextVideo = MediatorLiveData<Event<Unit>>()

    override val selectedDevice: LiveData<Event<Device>>
        get() = _selectedDevice

    override val playVideo: LiveData<Event<Unit>>
        get() = _playVideo

    override val pauseVideo: LiveData<Event<Unit>>
        get() = _pauseVideo

    override val rewindVideo: LiveData<Event<Unit>>
        get() = _rewindVideo

    override val fastForwardVideo: LiveData<Event<Unit>>
        get() = _fastForwardVideo

    override val previousVideo: LiveData<Event<Unit>>
        get() = _previousVideo

    override val nextVideo: LiveData<Event<Unit>>
        get() = _nextVideo

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

    override fun manageReceivedMessage(message: Message) {
        remoteDataMessageMapper.toLocalDataMessage(message).let { localDataMessage ->
            if (localDataMessage.messageType == LocalMessageType.PlayerCommand) {
                when (localDataMessage.playerCommand) {
                    LocalPlayerCommand.Play -> {
                        _playVideo.postValue(eventOf(Unit))
                    }
                    LocalPlayerCommand.Pause -> {
                        _pauseVideo.postValue(eventOf(Unit))
                    }
                    LocalPlayerCommand.Rewind -> {
                        _rewindVideo.postValue(eventOf(Unit))
                    }
                    LocalPlayerCommand.Previous -> {
                        _previousVideo.postValue(eventOf(Unit))
                    }
                    LocalPlayerCommand.Next -> {
                        _nextVideo.postValue(eventOf(Unit))
                    }
                    else -> {
                        _fastForwardVideo.postValue(eventOf(Unit))
                    }
                }
            }
        }
    }
}