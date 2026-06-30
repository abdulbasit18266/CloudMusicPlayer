package com.abdulbasit.cloudmusicplayer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale

class PlayerActivity : AppCompatActivity() {

    private val TAG = "PLAYER_ACTIVITY_DEBUG"

    private lateinit var btnBack: ImageView
    private lateinit var tvPlayerSongTitle: TextView
    private lateinit var playerSeekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalDuration: TextView
    private lateinit var btnPlayPause: ImageView

    private var exoPlayer: ExoPlayer? = null
    private var isPlayerReady = false

    // Handler jo seekbar aur timer text ko har second background mein chalata rahega
    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let {
                if (it.isPlaying) {
                    playerSeekBar.progress = it.currentPosition.toInt()
                    tvCurrentTime.text = formatTime(it.currentPosition)
                }
            }
            mainHandler.postDelayed(this, 1000) // Har 1 second baad call hoga
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // UI Binding
        btnBack = findViewById(R.id.btnBack)
        tvPlayerSongTitle = findViewById(R.id.tvPlayerSongTitle)
        playerSeekBar = findViewById(R.id.playerSeekBar)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalDuration = findViewById(R.id.tvTotalDuration)
        btnPlayPause = findViewById(R.id.btnPlayPause)

        // Intent se song data nikalna
        val songTitle = intent.getStringExtra("SONG_TITLE") ?: "Unknown Track"
        val songUrl = intent.getStringExtra("SONG_URL") ?: ""

        tvPlayerSongTitle.text = songTitle

        // ExoPlayer ko start aur song feed karna
        if (songUrl.isNotEmpty()) {
            startPlayback(songUrl)
        } else {
            Toast.makeText(this, "Invalid streaming URL", Toast.LENGTH_SHORT).show()
        }

        // Back action button
        btnBack.setOnClickListener {
            finish()
        }

        // Play/Pause manual click listener toggle
        btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        // User seekbar drag listener
        setupSeekBarChangeListener()
    }

    private fun startPlayback(url: String) {
        try {
            exoPlayer = ExoPlayer.Builder(this).build()
            val mediaItem = MediaItem.fromUri(url)

            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()

                // Real-time states ke liye listener add karna
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            isPlayerReady = true
                            val duration = duration
                            playerSeekBar.max = duration.toInt()
                            tvTotalDuration.text = formatTime(duration)

                            // Seekbar runnable start karo
                            mainHandler.post(updateSeekBarRunnable)

                            // Fixed: Added '.R.' here
                            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            // Fixed: Added '.R.' here
                            btnPlayPause.setImageResource(android.R. drawable.ic_media_pause)
                        } else {
                            // Fixed: Added '.R.' here
                            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "ExoPlayer error in PlayerActivity: ${e.message}", e)
            Toast.makeText(this, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                // Fixed: Added '.R.' here
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                it.play()
                // Fixed: Added '.R.' here
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun setupSeekBarChangeListener() {
        playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvCurrentTime.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    exoPlayer?.seekTo(it.progress.toLong())
                }
            }
        })
    }

    // Long milliseconds to clean "00:00" converter string function
    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(updateSeekBarRunnable) // Background loops clear karein
        exoPlayer?.release()
        exoPlayer = null
    }
}