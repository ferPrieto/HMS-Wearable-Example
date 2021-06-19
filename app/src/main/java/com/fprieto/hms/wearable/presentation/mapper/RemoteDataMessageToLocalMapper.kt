package com.fprieto.hms.wearable.presentation.mapper

import com.fprieto.hms.wearable.model.local.LocalDataMessage
import com.fprieto.hms.wearable.model.local.LocalMessageType
import com.fprieto.hms.wearable.model.local.LocalPlayerCommand
import com.fprieto.hms.wearable.model.remote.RemoteDataMessage
import com.google.gson.Gson
import com.huawei.wearengine.p2p.Message
import javax.inject.Inject

interface RemoteDataMessageToLocalMapper {
    fun toLocalDataMessage(message: Message): LocalDataMessage
}

class RemoteDataMessageToLocalMapperImpl @Inject constructor() :
    RemoteDataMessageToLocalMapper {
    override fun toLocalDataMessage(message: Message): LocalDataMessage =
        String(message.data).let { textMessage ->
            Gson().fromJson(textMessage, RemoteDataMessage::class.java).let { dataMessage ->
                LocalDataMessage(
                    messageType = getMessageType(dataMessage),
                    playerCommand = getPlayerCommand(dataMessage),
                    plainMessage = dataMessage.plainMessage
                )
            }
        }

    private fun getMessageType(dataMessage: RemoteDataMessage) =
        when (dataMessage.messageType) {
            "Player-Command" -> LocalMessageType.PlayerCommand
            else -> LocalMessageType.TextMessage
        }

    private fun getPlayerCommand(dataMessage: RemoteDataMessage): LocalPlayerCommand? =
        dataMessage.playerCommand?.let { remotePlayerCommand ->
            when (remotePlayerCommand.command.toLowerCase()) {
                "play" -> LocalPlayerCommand.Play
                "pause" -> LocalPlayerCommand.Pause
                "rewind" -> LocalPlayerCommand.Rewind
                else -> LocalPlayerCommand.FastForward
            }
        }
}