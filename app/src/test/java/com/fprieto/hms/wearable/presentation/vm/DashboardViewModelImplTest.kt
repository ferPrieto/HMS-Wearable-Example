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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class DashboardViewModelImplTest {

    private lateinit var cut: DashboardViewModel

    @Mock
    lateinit var remoteDataMessageToLocalMapper: RemoteDataMessageToLocalMapper

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        cut = DashboardViewModelImpl(remoteDataMessageToLocalMapper)
    }

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Test
    fun `Given message When getNavigationDestination then navigateToPlayer invoked with expected result`() {
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

            val expected = localDataMessage.playerCommand
            val navigateToPlayerObserver = mock<Observer<Event<LocalPlayerCommand>>>()

            cut.navigateToPlayer.observeForever(navigateToPlayerObserver)

            // When
            cut.getNavigationDestination(message, null)
            val actualValue = cut.navigateToPlayer.value?.peekContent()

            // Then
            assertEquals(expected, actualValue)
        }
    }

    @Test
    fun `Given message When getNavigationDestination then navigateToMessaging invoked with expected result`() {
        runBlocking {
            // Given
            val message =
                buildMessage("{\"messageType\":\"Text-Message\",\"plainMessage\":\"Some text message\" }")
            val localDataMessage = LocalDataMessage(
                LocalMessageType.TextMessage,
                null,
                "Some text message"
            )
            whenever(remoteDataMessageToLocalMapper.toLocalDataMessage(message)).thenReturn(
                localDataMessage
            )

            val expected = Unit
            val navigateToMessagingObserver = mock<Observer<Event<Unit>>>()

            cut.navigateToMessaging.observeForever(navigateToMessagingObserver)

            // When
            cut.getNavigationDestination(message, null)
            val actualValue = cut.navigateToMessaging.value?.peekContent()

            // Then
            assertEquals(expected, actualValue)
        }
    }

    private fun buildMessage(payLoad: String): Message =
        Message.Builder().setPayload(payLoad.toByteArray(StandardCharsets.UTF_8)).build()
}