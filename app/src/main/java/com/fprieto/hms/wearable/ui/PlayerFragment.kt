package com.fprieto.hms.wearable.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fprieto.hms.wearable.R
import com.fprieto.hms.wearable.audio.AudioPlayer
import com.fprieto.hms.wearable.credentials.CredentialsProvider
import com.fprieto.hms.wearable.databinding.FragmentPlayerBinding
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
                            Toast.makeText(context, "The device selected is unavailable", Toast.LENGTH_SHORT).show()
                            Timber.e("The device selected is unavailable")
                            findNavController().navigateUp()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error getting available devices", Toast.LENGTH_SHORT).show()
                    Timber.e(e, "Error getting available devices: ${e.message}")
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
            binding.logOutputTextView.text = ""
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
        printResultOnUIThread("Video URL loaded")
        binding.videoPlayer.prepareToPlay(this, playerState)
    }

    private fun printResultOnUIThread(string: String) = requireActivity().runOnUiThread {
        binding.logOutputTextView.append(string + System.lineSeparator())
        binding.logOutputTextView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun registerReceivers() {
        val receiver = Receiver { message ->
            message?.let { msg ->
                when (msg.type) {
                    Message.MESSAGE_TYPE_DATA -> {
                        printResultOnUIThread("Received data")
                        managePlayerCommands(msg)
                    }
                    Message.MESSAGE_TYPE_FILE -> {
                        printResultOnUIThread("Received file")
                    }
                    Message.MESSAGE_TYPE_DEFAULT -> printResultOnUIThread("Received default message")
                }
            } ?: run {
                printResultOnUIThread("Failed to manage the message command")
                Timber.d("Failed to manage the message on Player")
            }
        }
        p2pClient.registerReceiver(selectedDevice, receiver)
                .addOnSuccessListener { printResultOnUIThread("Register receiver listener succeed!") }
                .addOnFailureListener { printResultOnUIThread("Register receiver listener failed!") }
    }

    private fun managePlayerCommands(message: Message) {
        message.toLocalData().let { dataMessage ->
            if (dataMessage.messageType == LocalMessageType.PlayerCommand) {
                when (dataMessage.playerCommand) {
                    LocalPlayerCommand.Play -> {
                        binding.videoPlayer.play()
                        printResultOnUIThread("Play Command Selected")
                    }
                    LocalPlayerCommand.Pause ->{
                        binding.videoPlayer.pause()
                        printResultOnUIThread("Pause Command Selected")
                    }
                    LocalPlayerCommand.Rewind -> {
                        binding.videoPlayer.rewind()
                        printResultOnUIThread("Rewind Command Selected")
                    }
                    else -> {
                        binding.videoPlayer.fastForward()
                        printResultOnUIThread("FastForward Command Selected")
                    }
                }
            }
        }
    }
}


