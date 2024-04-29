package com.example.savr_library

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Environment
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
import java.io.File
import java.io.IOException

class AudioRecordingManager(private val context: Context,
                            private val listener: RecordingResultListener,
                            vadMinimumSilenceDurationMs: Int = 300,
                            vadMinimumSpeechDurationMs: Int = 30,
                            vadMode: Int = 1,
                            private val silenceDurationMs: Int = 5000,
                            private val maxRecordingDurationMs: Int = 60000) : Recorder.AudioCallback {
    private var isRecording: Boolean = false
    private lateinit var audioFile: File
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private var vad: VadSilero = Vad.builder()
        .setContext(context)
        .setSampleRate(SampleRate.SAMPLE_RATE_16K)
        .setFrameSize(FrameSize.FRAME_SIZE_512)
        .setMode(Mode.entries.find { it.value == vadMode}!!)
        .setSilenceDurationMs(vadMinimumSilenceDurationMs)
        .setSpeechDurationMs(vadMinimumSpeechDurationMs)
        .build();
    private var recorder: Recorder = Recorder(this)
    private var silenceStartTime: Long = 0
    private var hasSpoken: Boolean = false
    private var recordingStartTime: Long = 0

    interface RecordingResultListener {
        fun onRecordingComplete(audioFilePath: String)
        fun onRecordingError(errorMessage: String)
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) {
            return
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            listener.onRecordingError("External storage is not available")
            return
        }

        try {
            val audioDirectory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            if (audioDirectory != null) {
                val fileName = "recording_${System.currentTimeMillis()}.wav"
                audioFile = File(audioDirectory, fileName)

                recorder.setOutputFile(audioFile)

                isRecording = true
                hasSpoken = false

                playBeep()
                startSilenceDetection()
            } else {
                listener.onRecordingError("Failed to access audio directory")
            }
        } catch (e: IOException) {
            listener.onRecordingError("Failed to start recording: ${e.message}")
        }
    }

    private fun startSilenceDetection() {
        recordingStartTime = System.currentTimeMillis()
        recorder.start(vad.sampleRate.value, vad.frameSize.value)
    }

    fun stopRecording() {
        if (!isRecording) {
            return
        }

        try {
            isRecording = false

            recorder.stop()
        } catch (e: RuntimeException) {
            listener.onRecordingError("Failed to stop recording: ${e.message}")
        }
    }

    fun isRecording(): Boolean {
        return isRecording
    }

    private fun playBeep() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
    }

    override fun onAudio(audioData: ShortArray) {
        val totalRecordingTime = System.currentTimeMillis() - recordingStartTime
        val isSpeech = vad.isSpeech(audioData)

        if (isSpeech) {
            hasSpoken = true
            silenceStartTime = 0
        } else {
            if (hasSpoken) {
                if (silenceStartTime == 0L) {
                    silenceStartTime = System.currentTimeMillis()
                } else {
                    val elapsedTime = System.currentTimeMillis() - silenceStartTime
                    if (elapsedTime >= silenceDurationMs) {
                        completeRecording()
                    }
                }
            }
        }

        if (totalRecordingTime >= maxRecordingDurationMs) {
            completeRecording()
        }
    }

    private fun completeRecording() {
        playBeep()
        stopRecording()
        listener.onRecordingComplete(audioFile.absolutePath)
    }

    fun onDestroy() {
        recorder.stop()
        vad.close()
    }
}