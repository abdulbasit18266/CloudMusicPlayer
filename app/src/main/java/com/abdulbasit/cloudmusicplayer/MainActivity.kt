package com.abdulbasit.cloudmusicplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abdulbasit.cloudmusicplayer.adapter.SongAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private val webClientId = "939513048631-dvs53ktq14qmen9uoilavb4nnrlittle.apps.googleusercontent.com"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rvSongs: RecyclerView
    private lateinit var btnGoogleLogin: Button

    // 🎵 ExoPlayer Variable Declaration
    private var exoPlayer: ExoPlayer? = null

    private var dummySongsList = listOf(
        Song("1", "Dil De Diya Hai.mp3", ""),
        Song("2", "Tum Hi Ho.mp3", ""),
        Song("3", "Kesariya.mp3", "")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        rvSongs = findViewById(R.id.rvSongs)
        rvSongs.layoutManager = LinearLayoutManager(this)

        // 🎵 ExoPlayer Initialization
        exoPlayer = ExoPlayer.Builder(this).build()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            btnGoogleLogin.text = "Logged In as ${account.displayName}"
            fetchRealDriveSongs(account)
        }

        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            loginLauncher.launch(signInIntent)
        }
    }

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                Toast.makeText(this, "Welcome ${account?.displayName}", Toast.LENGTH_SHORT).show()
                btnGoogleLogin.text = "Logged In as ${account?.displayName}"

                if (account != null) {
                    fetchRealDriveSongs(account)
                }
            } catch (e: Exception) {
                android.util.Log.e("GOOGLE_SIGNIN", "Error Code: ${e.message}", e)
                Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Sign-In cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchRealDriveSongs(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(DriveScopes.DRIVE_READONLY)
        ).setSelectedAccount(account.account)

        val googleDriveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("CloudMusicPlayer").build()

        val driveHelper = DriveServiceHelper(googleDriveService)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Toast.makeText(this@MainActivity, "Fetching MP3 files from Drive...", Toast.LENGTH_SHORT).show()
                val realSongs = driveHelper.queryMp3Files()

                if (realSongs.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No MP3 files found! Loading Backup List.", Toast.LENGTH_LONG).show()
                    loadSongsInAdapter(dummySongsList)
                } else {
                    Toast.makeText(this@MainActivity, "Found ${realSongs.size} songs!", Toast.LENGTH_SHORT).show()
                    loadSongsInAdapter(realSongs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Failed to fetch songs: ${e.message}", Toast.LENGTH_LONG).show()
                loadSongsInAdapter(dummySongsList)
            }
        }
    }

    private fun loadSongsInAdapter(songs: List<Song>) {
        val adapter = SongAdapter(songs) { song ->
            // 🎵 Real Song Playback Logic Trigger!
            playSong(song)
        }
        rvSongs.adapter = adapter
    }

    // 🎵 Function to Stream/Play Music from Google Drive Link
    private fun playSong(song: Song) {
        if (song.url.isEmpty()) {
            Toast.makeText(this, "Dummy song has no stream link!", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Playing: ${song.title}", Toast.LENGTH_SHORT).show()

        try {
            exoPlayer?.apply {
                stop() // Purana chal raha gaana stop karo
                clearMediaItems() // List clear karo

                // Drive ka URL se MediaItem banayein
                val mediaItem = MediaItem.fromUri(song.url)
                setMediaItem(mediaItem)

                prepare() // Stream buffer karein
                play() // Shuru karo!
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Playback Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 🚨 Memory Leak se bachne ke liye Activity destroy hote waqt player ko release karein
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}