package com.fprieto.hms.wearable.presentation.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.fprieto.hms.wearable.presentation.mapper.RemoteDataMessageToLocalMapper
import com.fprieto.hms.wearable.model.local.LocalMessageType
import com.fprieto.hms.wearable.model.local.LocalPlayerCommand
import com.huawei.wearengine.p2p.Message
import javax.inject.Inject

abstract class PlayerViewModel : ViewModel() {
    abstract fun manageReceivedMessage(message: Message)

    abstract val playVideo: LiveData<Event<Unit>>
    abstract val pauseVideo: LiveData<Event<Unit>>
    abstract val rewindVideo: LiveData<Event<Unit>>
    abstract val fastForwardVideo: LiveData<Event<Unit>>
}

class PlayerViewModelImpl @Inject constructor(
    private val remoteDataMessageMapper: RemoteDataMessageToLocalMapper
) : PlayerViewModel() {

    private val _playVideo = MediatorLiveData<Event<Unit>>()
    private val _pauseVideo = MediatorLiveData<Event<Unit>>()
    private val _rewindVideo = MediatorLiveData<Event<Unit>>()
    private val _fastForwardVideo = MediatorLiveData<Event<Unit>>()


    override val playVideo: LiveData<Event<Unit>>
        get() = _playVideo

    override val pauseVideo: LiveData<Event<Unit>>
        get() = _pauseVideo

    override val rewindVideo: LiveData<Event<Unit>>
        get() = _rewindVideo

    override val fastForwardVideo: LiveData<Event<Unit>>
        get() = _fastForwardVideo

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
                    else -> {
                        _fastForwardVideo.postValue(eventOf(Unit))
                    }
                }
            }
        }
    }
}