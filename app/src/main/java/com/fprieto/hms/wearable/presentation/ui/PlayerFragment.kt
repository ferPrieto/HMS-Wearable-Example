package com.fprieto.hms.wearable.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.audio.AudioPlayer
import com.fprieto.hms.wearable.credentials.CredentialsProvider
import com.fprieto.hms.wearable.databinding.FragmentPlayerBinding
import com.fprieto.hms.wearable.databinding.ViewLogsBinding
import com.fprieto.hms.wearable.extensions.await
import com.fprieto.hms.wearable.player.VideoPlayerState
import com.fprieto.hms.wearable.presentation.vm.PlayerViewModel
import com.fprieto.hms.wearable.presentation.vm.observeEvent
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val PLAYER_STATE_KEY: String = "PLAYER_STATE_KEY"
private const val VIDEO_URL: String =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"

class PlayerFragment @Inject constructor(
    viewModelFactory: ViewModelProvider.Factory
) : Fragment(R.layout.fragment_player) {

    private val viewModel by viewModels<PlayerViewModel> { viewModelFactory }

    private lateinit var binding: FragmentPlayerBinding
    private lateinit var viewLogsBinding: ViewLogsBinding

    private val credentialsProvider: CredentialsProvider = CredentialsProvider()
    private var playerState: VideoPlayerState = VideoPlayerState()

    private var sendCallback: SendCallback? = null

    private var selectedDevice: Device? = null
        set(value) {
            field = value
            setDeviceIntoRadioButton(value)
            registerReceivers()
        }

    private fun setDeviceIntoRadioButton(value: Device?) {
        binding.connectedDevice.text = value?.name
        binding.connectedDevice.isChecked = value?.isConnected ?: false
    }

    private val p2pClient: P2pClient by lazy {
        HiWear.getP2pClient(context).apply {
            setPeerPkgName(credentialsProvider.getPeerPkgName())
            setPeerFingerPrint(credentialsProvider.getPeerFingerPrint())
        }
    }

    private val deviceClient: DeviceClient by lazy {
        HiWear.getDeviceClient(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(PLAYER_STATE_KEY, playerState)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getSelectedDevice()
    }

    override fun onPause() {
        super.onPause()
        sendCallback = null
        selectedDevice = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlayerBinding.inflate(inflater, container, false)
        viewLogsBinding = ViewLogsBinding.bind(binding.root)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        getLatestPlayerState(savedInstanceState)
        renderVideo()
        setViewModelObservers()
    }

    private fun initViews() {
        binding.debugModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewLogsBinding.scrollView.isVisible = isChecked
            binding.clearLogs.isVisible = isChecked
        }

        binding.clearLogs.setOnClickListener {
            viewLogsBinding.logOutputTextView.text = ""
        }
    }

    private fun getLatestPlayerState(bundle: Bundle?) {
        bundle?.getParcelable<VideoPlayerState>(PLAYER_STATE_KEY)
            ?.let { state ->
                playerState = state
            }
    }

    private fun renderVideo() {
        playerState = playerState.checkAndSet(VIDEO_URL)
        Timber.d("Video URL loaded")
        binding.videoPlayer.prepareToPlay(this, playerState)
    }

    private fun setViewModelObservers() {
        viewModel.selectedDevice.observeEvent(this) { lastSelectedDevice ->
            selectDevice(lastSelectedDevice)
        }

        viewModel.playVideo.observeEvent(this) {
            binding.videoPlayer.play()
            Timber.d("Play Command Selected")
        }
        viewModel.pauseVideo.observeEvent(this) {
            binding.videoPlayer.pause()
            Timber.d("Pause Command Selected")
        }
        viewModel.rewindVideo.observeEvent(this) {
            binding.videoPlayer.rewind()
            Timber.d("Rewind Command Selected")
        }
        viewModel.fastForwardVideo.observeEvent(this) {
            binding.videoPlayer.fastForward()
            Timber.d("FastForward Command Selected")
        }
        viewModel.previousVideo.observeEvent(this) {
            binding.videoPlayer.previous()
            Timber.d("Previous Command Selected")
        }
        viewModel.nextVideo.observeEvent(this) {
            binding.videoPlayer.next()
            Timber.d("Next Command Selected")
        }
    }

    private fun selectDevice(lastSelectedDevice: Device) {
        lifecycleScope.launch {
            deviceClient.bondedDevices.await().let { devices ->
                devices.firstOrNull { device -> device.uuid == lastSelectedDevice.uuid }
                    ?.let { selectedDevice = lastSelectedDevice }
            }
        }
    }

    private fun sendCommand(text: String) {
        Message.Builder()
            .setPayload(text.toByteArray())
            .build().let { message ->
                p2pClient.send(selectedDevice, message, sendCallback).addOnSuccessListener {
                    Timber.d("Sent message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()}")
                }.addOnFailureListener {
                    Timber.d("Send message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} failed")
                }
            }
    }

    private fun initResultCallbackAndSender() {
        sendCallback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                Timber.d("Send message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Result: $resultCode")
            }

            override fun onSendProgress(progress: Long) {
                Timber.d("Send message to ${selectedDevice?.name}'s ${credentialsProvider.getPeerPkgName()} Progress: $progress")
            }
        }
        Timber.d("Send Result Callback Registered!")
    }


    private fun registerReceivers() {
        val receiver = Receiver { message ->
            message?.let { message ->
                when (message.type) {
                    Message.MESSAGE_TYPE_DATA -> {
                        viewModel.manageReceivedMessage(message)
                        Timber.d("Received data")
                    }
                    Message.MESSAGE_TYPE_FILE -> Timber.d("Received file")

                    Message.MESSAGE_TYPE_DEFAULT -> Timber.d("Received default message")
                }
            } ?: run {
                Timber.d("Failed to manage the message command")
            }
        }
        p2pClient.registerReceiver(selectedDevice, receiver)
            .addOnSuccessListener { Timber.d("Register receiver listener succeed!") }
            .addOnFailureListener { Timber.e("Register receiver listener failed!", this) }
    }
}


