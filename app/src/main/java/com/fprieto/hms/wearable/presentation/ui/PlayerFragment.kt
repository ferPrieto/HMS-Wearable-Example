package com.fprieto.hms.wearable.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.audio.AudioPlayer
import com.fprieto.hms.wearable.credentials.CredentialsProvider
import com.fprieto.hms.wearable.databinding.FragmentPlayerBinding
import com.fprieto.hms.wearable.databinding.ViewLogsBinding
import com.fprieto.hms.wearable.extensions.await
import com.fprieto.hms.wearable.extensions.toLocalData
import com.fprieto.hms.wearable.model.local.LocalMessageType
import com.fprieto.hms.wearable.model.local.LocalPlayerCommand
import com.fprieto.hms.wearable.player.VideoPlayerState
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import kotlinx.coroutines.launch
import timber.log.Timber

private const val PLAYER_STATE_KEY: String = "PLAYER_STATE_KEY"
private const val VIDEO_URL: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"

class PlayerFragment : Fragment(R.layout.fragment_player) {

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPlayerBinding.inflate(inflater, container, false)
        viewLogsBinding = ViewLogsBinding.bind(binding.root)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reSelectDevice()
        initButtons()
        getLatestPlayerState(savedInstanceState)
        renderVideo()
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

    private fun initButtons() {
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

    private fun registerReceivers() {
        val receiver = Receiver { message ->
            message?.let { msg ->
                when (msg.type) {
                    Message.MESSAGE_TYPE_DATA -> {
                        "Received data".logResult()
                        managePlayerCommands(msg)
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

    private fun managePlayerCommands(message: Message) {
        message.toLocalData().let { dataMessage ->
            if (dataMessage.messageType == LocalMessageType.PlayerCommand) {
                when (dataMessage.playerCommand) {
                    LocalPlayerCommand.Play -> {
                        binding.videoPlayer.play()
                        "Play Command Selected".logResult()
                    }
                    LocalPlayerCommand.Pause -> {
                        binding.videoPlayer.pause()
                        "Pause Command Selected".logResult()
                    }
                    LocalPlayerCommand.Rewind -> {
                        binding.videoPlayer.rewind()
                        "Rewind Command Selected".logResult()
                    }
                    else -> {
                        binding.videoPlayer.fastForward()
                        "FastForward Command Selected".logResult()
                    }
                }
            }
        }
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


