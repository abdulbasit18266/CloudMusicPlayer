package com.abdulbasit.cloudmusicplayer

import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvPlayerSongTitle: TextView
    private lateinit var playerSeekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalDuration: TextView
    private lateinit var btnPlayPause: ImageView

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

        // Intent se Song Title receive karke set karna
        val songTitle = intent.getStringExtra("SONG_TITLE") ?: "Unknown Track"
        tvPlayerSongTitle.text = songTitle

        // Back action button trigger
        btnBack.setOnClickListener {
            finish() // Isse page close hoga aur wapas list screen aa jayegi
        }
    }
}