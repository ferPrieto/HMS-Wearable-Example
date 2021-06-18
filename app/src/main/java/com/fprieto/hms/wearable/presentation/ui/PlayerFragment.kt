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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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

    private val audioPlayer: AudioPlayer by lazy {
        AudioPlayer(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(PLAYER_STATE_KEY, playerState)
    }

    override fun onResume() {
        super.onResume()
        registerReceivers()
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
        reSelectDevice()
        initViews()
        getLatestPlayerState(savedInstanceState)
        renderVideo()
        setViewModelObservers()
    }

    private fun reSelectDevice() {
        getDeviceUuId().let { uuId ->
            lifecycleScope.launch {
                try {
                    deviceClient.bondedDevices.await().let { devices ->
                        devices.firstOrNull { device -> device.uuid == uuId }?.let { device ->
                            selectedDevice = device
                        } ?: run {
                            "The device selected is unavailable".logResult()
                            findNavController().navigateUp()
                        }
                    }
                } catch (e: Exception) {
                    "Error getting available devices: ${e.message}".logError(e)
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun getDeviceUuId(): String {
        val args: PlayerFragmentArgs by navArgs()
        return args.deviceUuid
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
        "Video URL loaded".logResult()
        binding.videoPlayer.prepareToPlay(this, playerState)
    }

    private fun setViewModelObservers() {
        viewModel.playVideo.observeEvent(this) {
            binding.videoPlayer.play()
            "Play Command Selected".logResult()
        }
        viewModel.pauseVideo.observeEvent(this) {
            binding.videoPlayer.pause()
            "Pause Command Selected".logResult()
        }
        viewModel.rewindVideo.observeEvent(this) {
            binding.videoPlayer.rewind()
            "Rewind Command Selected".logResult()
        }
        viewModel.fastForwardVideo.observeEvent(this) {
            binding.videoPlayer.fastForward()
            "FastForward Command Selected".logResult()
        }
    }

    private fun registerReceivers() {
        val receiver = Receiver { message ->
            message?.let { message ->
                when (message.type) {
                    Message.MESSAGE_TYPE_DATA -> {
                        "Received data".logResult()
                        viewModel.manageReceivedMessage(message)
                    }
                    Message.MESSAGE_TYPE_FILE -> "Received file".logResult()

                    Message.MESSAGE_TYPE_DEFAULT -> "Received default message".logResult()
                }
            } ?: run {
                "Failed to manage the message command".logResult()
            }
        }
        p2pClient.registerReceiver(selectedDevice, receiver)
            .addOnSuccessListener { "Register receiver listener succeed!".logResult() }
            .addOnFailureListener { "Register receiver listener failed!".logError(it) }
    }

    private fun String.logResult() {
        appendOnOutputView(this)
        Timber.d(this)
    }

    private fun String.logError(exception: Exception) {
        appendOnOutputView(this)
        Timber.e(exception, this)
    }

    private fun appendOnOutputView(text: String) {
        requireActivity().runOnUiThread {
            viewLogsBinding.logOutputTextView.append(text + System.lineSeparator())
            viewLogsBinding.logOutputTextView.post {
                viewLogsBinding.scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}


