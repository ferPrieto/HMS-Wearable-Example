package com.fprieto.hms.wearable.audio

import java.io.*

const val PCM8BIT = 8
const val PCM16BIT = 16
const val CHANNEL_STEREO = 2
const val AUDIO_SAMPLE_RATE = 44100
const val TRANSFER_BUFFER_SIZE = 10 * 1024

//based on https://stackoverflow.com/a/55993895/13782111

class PCMWav {

    @Throws(IOException::class)
    fun PCMToWAV(input: File, output: File?, channelCount: Int, sampleRate: Int, bitsPerSample: Int) {
        val inputSize = input.length().toInt()
        FileOutputStream(output).use { encoded ->
            // WAVE RIFF header
            writeToOutput(encoded, "RIFF") // chunk id
            writeToOutput(encoded, 36 + inputSize) // chunk size
            writeToOutput(encoded, "WAVE") // format

            // SUB CHUNK 1 (FORMAT)
            writeToOutput(encoded, "fmt ") // subchunk 1 id
            writeToOutput(encoded, 16) // subchunk 1 size
            writeToOutput(encoded, 1.toShort()) // audio format (1 = PCM)
            writeToOutput(encoded, channelCount.toShort()) // number of channelCount
            writeToOutput(encoded, sampleRate) // sample rate
            writeToOutput(encoded, sampleRate * channelCount * bitsPerSample / 8) // byte rate
            writeToOutput(encoded, (channelCount * bitsPerSample / 8).toShort()) // block align
            writeToOutput(encoded, bitsPerSample.toShort()) // bits per sample

            // SUB CHUNK 2 (AUDIO DATA)
            writeToOutput(encoded, "data") // subchunk 2 id
            writeToOutput(encoded, inputSize) // subchunk 2 size
            copy(FileInputStream(input), encoded)
        }
    }

    @Throws(IOException::class)
    fun writeToOutput(output: OutputStream, data: String) {
        for (element in data) output.write(element.toInt())
    }

    @Throws(IOException::class)
    fun writeToOutput(output: OutputStream, data: Int) {
        output.write(data shr 0)
        output.write(data shr 8)
        output.write(data shr 16)
        output.write(data shr 24)
    }

    @Throws(IOException::class)
    fun writeToOutput(output: OutputStream, data: Short) {
        output.write(data.toInt() shr 0)
        output.write(data.toInt() shr 8)
    }

    @Throws(IOException::class)
    fun copy(source: InputStream, output: OutputStream): Long {
        return copy(source, output, TRANSFER_BUFFER_SIZE)
    }

    @Throws(IOException::class)
    fun copy(source: InputStream, output: OutputStream, bufferSize: Int): Long {
        var read = 0L
        val buffer = ByteArray(bufferSize)
        var n: Int
        while (source.read(buffer).also { n = it } != -1) {
            output.write(buffer, 0, n)
            read += n.toLong()
        }
        return read
    }
}