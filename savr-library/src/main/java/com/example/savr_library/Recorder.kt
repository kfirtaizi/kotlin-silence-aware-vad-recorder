package com.example.savr_library

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Recorder(val callback: AudioCallback) {
    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private var isListening = false

    private var sampleRate: Int = 0
    private var frameSize: Int = 0

    private var outputFile: File? = null

    fun start(sampleRate: Int, frameSize: Int) {
        this.sampleRate = sampleRate
        this.frameSize = frameSize
        stop()

        audioRecord = createAudioRecord()
        if (audioRecord != null) {
            isListening = true
            audioRecord?.startRecording()

            thread = Thread(ProcessVoice())
            thread?.start()
        }
    }

    fun stop() {
        isListening = false
        thread?.interrupt()
        thread = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): AudioRecord? {
        try {
            val minBufferSize = maxOf(
                AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ),
                2 * frameSize
            )

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )

            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                return audioRecord
            } else {
                audioRecord.release()
            }
        } catch (e: IllegalArgumentException) {
            Log.e("Recorder", "Error can't create AudioRecord ", e)
        }
        return null
    }

    private inner class ProcessVoice : Runnable {
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val outputStream = outputFile?.let { RandomAccessFile(it, "rw") }

            try {
                // Preparing the WAV file header
                writeWavHeader(outputStream, AudioFormat.CHANNEL_IN_MONO, sampleRate)

                while (!Thread.interrupted() && isListening) {
                    val buffer = ShortArray(frameSize)
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        outputStream?.write(shortArrayToByteArray(buffer))
                    }

                    callback.onAudio(buffer)
                }

                // Update the WAV header to include the final chunk size
                updateWavHeader(outputStream)
            } finally {
                outputStream?.close()
            }
        }

        private fun writeWavHeader(file: RandomAccessFile?, channelConfig: Int, sampleRate: Int) {
            val byteRate = sampleRate * 2
            val blockAlign = 2
            val bitsPerSample = 16
            val dataSize = 0 // Placeholder, update after recording
            val subChunk2Size = dataSize * channelConfig * bitsPerSample / 8
            val chunkSize = 36 + subChunk2Size

            val header = ByteBuffer.allocate(44)
            header.order(ByteOrder.LITTLE_ENDIAN)

            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(chunkSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // Audio format (PCM)
            header.putShort(1) // Number of channels
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(subChunk2Size)

            file?.write(header.array())
        }

        private fun updateWavHeader(file: RandomAccessFile?) {
            file?.let {
                it.seek(4)  // Move to file size position
                val fileSize = it.length()
                it.writeInt((fileSize - 8).toInt())  // Update file size

                it.seek(40)  // Move to data chunk size position
                it.writeInt((fileSize - 44).toInt())  // Update data chunk size
            }
        }

        private fun shortArrayToByteArray(shortArray: ShortArray): ByteArray {
            val byteBuffer = ByteBuffer.allocate(shortArray.size * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            for (sample in shortArray) {
                byteBuffer.putShort(sample)
            }
            return byteBuffer.array()
        }
    }

    fun setOutputFile(file: File) {
        outputFile = file
    }

    interface AudioCallback {
        fun onAudio(audioData: ShortArray)
    }
}
