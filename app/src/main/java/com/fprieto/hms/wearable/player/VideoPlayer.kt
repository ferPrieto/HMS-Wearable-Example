package com.fprieto.hms.wearable.player

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.fprieto.hms.wearable.databinding.VideoPlayerBinding

class VideoPlayer @JvmOverloads constructor(context: Context, attributeSet: AttributeSet) :
    FrameLayout(context, attributeSet) {

    private var binding: VideoPlayerBinding
    private lateinit var videoComponent: VideoPlayerComponent

    init {
        isSaveEnabled = true
        val inflater: LayoutInflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        binding = VideoPlayerBinding.inflate(inflater, this, true)
    }

    fun prepareToPlay(owner: LifecycleOwner, playerState: VideoPlayerState) {
        initVideoComponent(playerState)
        setPlayerState(playerState, owner)
    }

    fun play() {
        videoComponent.setPlayerState(true)
    }

    fun pause() {
        videoComponent.setPlayerState(false)
    }

    fun rewind() {
        binding.playerView.player.seekTo(binding.playerView.player.currentPosition - 10000L)
    }

    fun fastForward() {
        binding.playerView.player.seekTo(binding.playerView.player.currentPosition + 10000L)
    }

    fun previous() {
        binding.playerView.player.seekTo(0)
    }

    fun next() {
        binding.playerView.player.seekTo(binding.playerView.player.contentDuration)
    }

    private fun setPlayerState(playerState: VideoPlayerState, owner: LifecycleOwner) {
        if (playerState.videoUrl.isNullOrEmpty()) {
            binding.playerView.onPause()
            videoComponent.disposePlayer()
            owner.lifecycle.removeObserver(videoComponent)
        } else {
            owner.lifecycle.removeObserver(videoComponent)
            owner.lifecycle.addObserver(videoComponent)
        }
    }

    private fun initVideoComponent(playerState: VideoPlayerState) {
        if (::videoComponent.isInitialized.not()) {
            videoComponent = VideoPlayerComponent(context, binding.playerView, playerState)
        }
    }
}