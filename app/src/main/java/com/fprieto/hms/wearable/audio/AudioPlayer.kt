package com.fprieto.hms.wearable.audio

import android.content.Context
import java.io.File

class AudioPlayer(private val context: Context?) {

    fun analyse(pcmFile: File) {
        val wavFile = File(context?.cacheDir, "temp.wav")
        PCMWav().PCMToWAV(pcmFile, wavFile,
                CHANNEL_STEREO,
                AUDIO_SAMPLE_RATE,
                PCM8BIT)
        // Todo: Play audio file
    }
}