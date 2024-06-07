package com.example.termproject

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

object SoundManager {
    enum class Bgm(val value: Int) {
        LOBBY(0),
        GAME(1);

        fun fromInt(value: Int): Bgm {
            return values().firstOrNull { it.value == value } ?: LOBBY
        }

        fun toInt(): Int {
            return value
        }
    }

    enum class EFFECT(val value: Int) {
        CLICK(0),
        DAMAGING(1),
        DAMAGED(2);

        fun fromInt(value: Int): EFFECT {
            return values().firstOrNull { it.value == value } ?: CLICK
        }

        fun toInt(): Int {
            return value
        }
    }

    private var mediaPlayerList = mutableListOf<MediaPlayer>()
    private lateinit var soundPool: SoundPool
    private val soundMap = mutableMapOf<Int, Int>()
    private var isInitialized = false

    private var playedBgmId : Int = 0

    fun init(context: Context) {
        if (isInitialized) return

        // Initialize MediaPlayer for background music
        val bgmLobby = MediaPlayer.create(context, R.raw.bgm01).apply {
            isLooping = true
            setVolume(1.0f, 1.0f)  // Set volume to maximum
        }
        val bgmGame = MediaPlayer.create(context, R.raw.bgm01).apply {
            isLooping = true
            setVolume(1.0f, 1.0f)  // Set volume to maximum
        }
        mediaPlayerList.add(0, bgmLobby)
        mediaPlayerList.add(1, bgmGame)

        // Initialize SoundPool for sound effects
        soundPool = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
        } else {
            SoundPool(10, android.media.AudioManager.STREAM_MUSIC, 0)
        }

        // Load sound effects into the SoundPool
        soundMap[R.raw.sfx_touch01] = soundPool.load(context, R.raw.sfx_touch01, 1)

        isInitialized = true
    }

    fun playBackgroundMusic(bgmId : Bgm) {
        if (!mediaPlayerList[bgmId.toInt()].isPlaying) {
            if (playedBgmId != bgmId.toInt())
                mediaPlayerList[playedBgmId].stop()
            mediaPlayerList[bgmId.toInt()].start()

            playedBgmId = bgmId.toInt()
        }
    }

    fun stopBackgroundMusic() {
        if (mediaPlayerList[playedBgmId].isPlaying) {
            mediaPlayerList[playedBgmId].stop()
        }
    }

    fun playSoundEffect(effectResId: Int) {
        val soundId = soundMap[effectResId] ?: return
        soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)  // Set volume to maximum
    }

    fun release() {
        for (mediaPlayer in mediaPlayerList) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        }

        soundPool.release()

        isInitialized = false
    }
}
