# SAVR: Silence-Aware Voice Recorder

SAVR is a Kotlin library that simplifies audio recording with automatic silence & noise detection and recording termination.

## Features

- Record audio and save as WAV format to local file `"recording_${System.currentTimeMillis()}.wav"`
- Once speech is detected, SAVR automatically stops recording after `silenceDurationMs` of non-speech (noise/silence) input
    - Recording resumes and the timer resets every time speech is detected
    - Default value is 5000ms (5 seconds)
- Recording stops entirely after `maxRecordingDurationMs`
    - Default value is 60000ms (60 seconds)
- Control VAD's speech confidence using `vadMode` (higher confidence results in fewer false-positives):
    - `vadMode = 1` -> 50% confidence
    - `vadMode = 2` -> 80% confidence
    - `vadMode = 3` -> 95% confidence
- Control minimum duration in milliseconds for silence segments using `vadMinimumSilenceDurationMs`
    - This parameter defines the necessary and sufficient duration of negative results to recognize it as silence
    - Default value is 300ms (found to work best)
- Control minimum duration in milliseconds for speech segments using `vadMinimumSpeechDurationMs`
    - This parameter defines the necessary and sufficient duration of positive results to recognize the result as speech
    - Default value is 30ms (found to work best)

## Sample App

Run the sample-app to see a demonstration of SAVR in action.

![SAVR Sample App](https://github.com/kfirtaizi/kotlin-silence-aware-vad-recorder/assets/44837286/38993226-366a-4788-b2d5-3403f0c0e891)
