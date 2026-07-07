package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sin

object AudioService {
    // Play Windows-style boot chime using multi-oscillator sine wave synthesis
    suspend fun playBootSound() {
        withContext(Dispatchers.Default) {
            try {
                val sampleRate = 22050
                val durationSec = 2.5
                val numSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(numSamples)

                // Synthesize a beautiful harmonic chord progression (like the modern Win11/10/7 chime)
                // Progression: Eb Maj7 -> Ab Maj9 -> Bb Add9
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    
                    // Attack-Sustain-Decay-Release (ASDR) envelope
                    val envelope = when {
                        t < 0.4 -> t / 0.4 // gentle attack fade-in
                        t > 1.8 -> ((durationSec - t) / 0.7).coerceIn(0.0, 1.0) // smooth decay fade-out
                        else -> 1.0
                    }

                    // Frequencies (Hz) of notes in our chord:
                    // Pad (sustained low harmony) + rising chime notes
                    val baseFreq = 155.56 // Eb3
                    val freq1 = 233.08 // Bb3
                    val freq2 = 311.13 // Eb4
                    val freq3 = 392.00 // G4
                    val freq4 = 466.16 // Bb4
                    
                    // Arpeggiate the top notes slightly to give it that retro "Windows" chimerical feel
                    val amp1 = 0.3
                    val amp2 = if (t > 0.15) 0.3 else (t / 0.15) * 0.3
                    val amp3 = if (t > 0.3) 0.25 else if (t < 0.15) 0.0 else ((t - 0.15) / 0.15) * 0.25
                    val amp4 = if (t > 0.45) 0.2 else if (t < 0.3) 0.0 else ((t - 0.3) / 0.15) * 0.2

                    val wave1 = sin(2.0 * Math.PI * baseFreq * t)
                    val wave2 = sin(2.0 * Math.PI * freq1 * t)
                    val wave3 = sin(2.0 * Math.PI * freq2 * t)
                    val wave4 = sin(2.0 * Math.PI * freq3 * t)
                    val wave5 = sin(2.0 * Math.PI * freq4 * t)

                    // Combine signals and scale to PCM 16-bit
                    val combined = (wave1 * 0.25 + wave2 * amp1 + wave3 * amp2 + wave4 * amp3 + wave5 * amp4)
                    val sample = (combined * Short.MAX_VALUE * envelope * 0.45).toInt()
                    
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay(2500)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Play Windows-style shutdown chime (descending notes with a warm fade out)
    suspend fun playShutdownSound() {
        withContext(Dispatchers.Default) {
            try {
                val sampleRate = 22050
                val durationSec = 1.8
                val numSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    
                    val envelope = when {
                        t < 0.15 -> t / 0.15 // rapid fade-in
                        t > 1.2 -> ((durationSec - t) / 0.6).coerceIn(0.0, 1.0) // clean fade-out
                        else -> 1.0
                    }

                    // Descending notes: G4 (392.00 Hz) -> Eb4 (311.13 Hz) -> Bb3 (233.08 Hz) -> G3 (196.00 Hz)
                    // Note transitions based on elapsed time
                    val freq1 = if (t < 0.3) 392.00 else if (t < 0.6) 311.13 else 233.08
                    val freq2 = if (t < 0.3) 311.13 else if (t < 0.6) 233.08 else 196.00
                    val freq3 = 130.81 // Low C3 warm hum

                    val wave1 = sin(2.0 * Math.PI * freq1 * t)
                    val wave2 = sin(2.0 * Math.PI * freq2 * t)
                    val wave3 = sin(2.0 * Math.PI * freq3 * t)

                    val combined = (wave1 * 0.35 + wave2 * 0.35 + wave3 * 0.3)
                    val sample = (combined * Short.MAX_VALUE * envelope * 0.4).toInt()

                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay(1800)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
