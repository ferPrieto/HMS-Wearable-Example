package com.fprieto.hms.wearable.presentation.vm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.fprieto.hms.wearable.model.local.LocalDataMessage
import com.fprieto.hms.wearable.model.local.LocalMessageType
import com.fprieto.hms.wearable.model.local.LocalPlayerCommand
import com.fprieto.hms.wearable.presentation.mapper.RemoteDataMessageToLocalMapper
import com.huawei.wearengine.p2p.Message
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.nio.charset.StandardCharsets

@RunWith(MockitoJUnitRunner::class)
class PlayerViewModelImplTest {

    private lateinit var cut: PlayerViewModel

    @Mock
    lateinit var remoteDataMessageToLocalMapper: RemoteDataMessageToLocalMapper


    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        cut = PlayerViewModelImpl(remoteDataMessageToLocalMapper)
    }

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Test
    fun `Given message When manageReceivedMessage then pauseVideo invoked with expected result`() {
        runBlocking {
            // Given
            val message =
                buildMessage("{\"messageType\":\"Player-Command\",\"playerCommand\":{\"command\":\"pause\" }}")
            val localDataMessage = LocalDataMessage(
                LocalMessageType.PlayerCommand,
                LocalPlayerCommand.Pause,
                null
            )
            whenever(remoteDataMessageToLocalMapper.toLocalDataMessage(message)).thenReturn(
                localDataMessage
            )

            val expected = Unit
            val pauseVideoObserver = mock<Observer<Event<Unit>>>()

            cut.pauseVideo.observeForever(pauseVideoObserver)

            // When
            cut.manageReceivedMessage(message)
            val actualValue = cut.pauseVideo.value?.peekContent()

            // Then
            assertEquals(expected, actualValue)
        }
    }

    @Test
    fun `Given message When manageReceivedMessage then fastForwardVideo invoked with expected result`() {
        runBlocking {
            // Given
            val message =
                buildMessage("{\"messageType\":\"Player-Command\",\"playerCommand\":{\"command\":\"fastForward\" }}")
            val localDataMessage = LocalDataMessage(
                LocalMessageType.PlayerCommand,
                LocalPlayerCommand.FastForward,
                null
            )
            whenever(remoteDataMessageToLocalMapper.toLocalDataMessage(message)).thenReturn(
                localDataMessage
            )

            val expected = Unit
            val fastForwardVideoObserver = mock<Observer<Event<Unit>>>()

            cut.fastForwardVideo.observeForever(fastForwardVideoObserver)

            // When
            cut.manageReceivedMessage(message)
            val actualValue = cut.fastForwardVideo.value?.peekContent()

            // Then
            assertEquals(expected, actualValue)
        }
    }

    private fun buildMessage(payLoad: String): Message =
        Message.Builder().setPayload(payLoad.toByteArray(StandardCharsets.UTF_8)).build()
}