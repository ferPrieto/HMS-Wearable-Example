package com.fprieto.hms.wearable.player

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.fprieto.hms.wearable.databinding.VideoPlayerBinding
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout

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
        binding.playerView.setRewindIncrementMs(10)
    }

    fun fastForward() {
        binding.playerView.setFastForwardIncrementMs(10)
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

    private fun hideSystemUi() {
        binding.playerView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
    }

    private fun expand() {
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
    }
}