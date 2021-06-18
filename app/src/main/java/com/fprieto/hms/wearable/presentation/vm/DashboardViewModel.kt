package com.fprieto.hms.wearable.presentation.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.fprieto.hms.wearable.model.local.LocalMessageType
import com.fprieto.hms.wearable.model.local.LocalPlayerCommand
import com.fprieto.hms.wearable.presentation.mapper.RemoteDataMessageToLocalMapper
import com.fprieto.hms.wearable.presentation.ui.Destination
import com.huawei.wearengine.p2p.Message
import javax.inject.Inject

abstract class DashboardViewModel : ViewModel() {
    abstract fun getNavigationDestination(message: Message?, localMessageType: LocalMessageType?)

    abstract val navigateToMessaging: LiveData<Event<Unit>>
    abstract val navigateToPlayer: LiveData<Event<LocalPlayerCommand>>
}

class DashboardViewModelImpl @Inject constructor(
    private val remoteDataMessageMapper: RemoteDataMessageToLocalMapper
) : DashboardViewModel() {
    private val _navigateToMessaging = MediatorLiveData<Event<Unit>>()
    private val _navigateToPlayer = MediatorLiveData<Event<LocalPlayerCommand>>()

    override val navigateToMessaging: LiveData<Event<Unit>>
        get() = _navigateToMessaging

    override val navigateToPlayer: LiveData<Event<LocalPlayerCommand>>
        get() = _navigateToPlayer

    override fun getNavigationDestination(message: Message?, localMessageType: LocalMessageType?) {
        message?.let { msg ->
            remoteDataMessageMapper.toLocalDataMessage(msg).let { localDataMessage ->
                when (getDestination(localDataMessage.messageType)) {
                    Destination.Messaging -> _navigateToMessaging.postValue(eventOf(Unit))
                    else -> _navigateToPlayer.postValue(
                        eventOf(localDataMessage.playerCommand ?: LocalPlayerCommand.Play)
                    )
                }
            }
        } ?: run {
            localMessageType?.let { messageType ->
                when (getDestination(messageType)) {
                    Destination.Messaging -> _navigateToMessaging.postValue(eventOf(Unit))
                    else -> _navigateToPlayer.postValue(eventOf(LocalPlayerCommand.Play))
                }
            }
        }
    }

    private fun getDestination(messageType: LocalMessageType) = when (messageType) {
        LocalMessageType.PlayerCommand -> Destination.MediaPlayer
        else -> Destination.Messaging
    }
}