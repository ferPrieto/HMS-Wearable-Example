package com.fprieto.hms.wearable.presentation.mapper

import com.fprieto.hms.wearable.model.local.LocalDataMessage
import com.fprieto.hms.wearable.model.local.LocalMessageType
import com.fprieto.hms.wearable.model.local.LocalPlayerCommand
import com.huawei.wearengine.p2p.Message
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class RemoteDataMessageToLocalMapperImplTest(
    private val hiWearMessage: Message,
    private val expected: LocalDataMessage
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                buildMessage("{\"messageType\":\"Player-Command\",\"playerCommand\":{\"command\":\"fastForward\"}}"),
                LocalDataMessage(
                    LocalMessageType.PlayerCommand,
                    LocalPlayerCommand.FastForward,
                    null
                )
            ),
            arrayOf(
                buildMessage("{\"messageType\":\"Player-Command\",\"playerCommand\":{\"command\":\"rewind\" }}"),
                LocalDataMessage(
                    LocalMessageType.PlayerCommand,
                    LocalPlayerCommand.Rewind,
                    null
                )
            ),
            arrayOf(
                buildMessage("{\"messageType\":\"Player-Command\",\"playerCommand\":{\"command\":\"play\" }}"),
                LocalDataMessage(
                    LocalMessageType.PlayerCommand,
                    LocalPlayerCommand.Play,
                    null
                )
            ),
            arrayOf(
                buildMessage("{\"messageType\":\"Player-Command\",\"playerCommand\":{\"command\":\"pause\" }}"),
                LocalDataMessage(
                    LocalMessageType.PlayerCommand,
                    LocalPlayerCommand.Pause,
                    null
                )
            ),
            arrayOf(
                buildMessage("{\"messageType\":\"Text-Message\",\"plainMessage\":\"Some text message\" }"),
                LocalDataMessage(
                    LocalMessageType.TextMessage,
                    null,
                    "Some text message"
                )
            )
        )

        private fun buildMessage(payLoad: String): Message =
            Message.Builder().setPayload(payLoad.toByteArray(StandardCharsets.UTF_8)).build()
    }

    private lateinit var cut: RemoteDataMessageToLocalMapperImpl

    @Before
    fun setUp() {
        cut = RemoteDataMessageToLocalMapperImpl()
    }

    @Test
    fun `Given hiWearMessage when toLocalDataMessage then returns expected result`() {
        // When
        val actualValue = cut.toLocalDataMessage(hiWearMessage)

        // Then
        assertEquals(expected, actualValue)
    }
}