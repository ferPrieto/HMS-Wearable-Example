package com.fprieto.hms.wearable.mapper

import com.fprieto.hms.wearable.model.local.LocalDataMessage
import com.fprieto.hms.wearable.model.local.LocalMessageType
import com.fprieto.hms.wearable.model.local.LocalPlayerCommand
import com.fprieto.hms.wearable.model.remote.RemoteDataMessage
import com.google.gson.Gson
import com.huawei.wearengine.p2p.Message

class RemoteDataMessageToLocalMapper {
    fun toLocalDataMessage(message: Message): LocalDataMessage = String(message.data).let { textMessage ->
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
                when (remotePlayerCommand.command) {
                    "play" -> LocalPlayerCommand.Play
                    "stop" -> LocalPlayerCommand.Pause
                    "rewind" -> LocalPlayerCommand.Rewind
                    else -> LocalPlayerCommand.FastForward
                }
            }
}