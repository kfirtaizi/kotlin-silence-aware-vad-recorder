package com.example.sample_app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.savr_library.AudioRecordingManager

class MainActivity : AppCompatActivity(), AudioRecordingManager.RecordingResultListener {
    private lateinit var audioRecordingManager: AudioRecordingManager
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioRecordingManager = AudioRecordingManager(this, this)

        findViewById<Button>(R.id.startButton).setOnClickListener {
            requestAudioPermissions()
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            audioRecordingManager.stopRecording()
        }
    }

    private fun requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            // Permission has already been granted
            audioRecordingManager.startRecording()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            runOnUiThread {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, start recording
                    audioRecordingManager.startRecording()
                } else {
                    // Permission denied, inform the user and possibly disable functionality
                    Toast.makeText(this@MainActivity, "Permission denied to record audio", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRecordingComplete(audioFilePath: String) {
        runOnUiThread {
            // Play the recorded audio
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFilePath)
                prepare()
                start()
            }
            Toast.makeText(this@MainActivity, "Recording complete. Playing audio...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRecordingError(errorMessage: String) {
        runOnUiThread {
            Log.e("MainActivity", "Recording error: $errorMessage")
            Toast.makeText(this@MainActivity, "Recording error: $errorMessage", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecordingManager.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
