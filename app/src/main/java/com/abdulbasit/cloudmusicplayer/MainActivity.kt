package com.abdulbasit.cloudmusicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private val TAG = "CLOUD_MUSIC_DEBUG"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rvSongs: RecyclerView
    private lateinit var btnGoogleLogin: Button

    private var exoPlayer: ExoPlayer? = null

    // Backup list agar internet bilkul hi na ho
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

        // Player initialization
        initializePlayer()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 🛠️ BUG FIX 1: Silent Sign-In use karenge taaki Token automatic refresh ho jaye
        checkExistingUserSession()

        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            loginLauncher.launch(signInIntent)
        }
    }

    private fun initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build()
        }
    }

    private fun checkExistingUserSession() {
        // Silent sign-in se hamesha valid backend session check hota hai
        googleSignInClient.silentSignIn()
            .addOnSuccessListener { account ->
                Log.d(TAG, "Silent Sign-In Successful for: ${account.displayName}")
                btnGoogleLogin.text = "Logged In as ${account.displayName}"
                fetchRealDriveSongs(account)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Silent Sign-In Failed, user needs to click login button: ${e.message}")
                // Agar user session expired hai, toh UI par safe backup dikha do jab tak login na ho
                loadSongsInAdapter(dummySongsList)
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
                Log.e(TAG, "Login Failed: ${e.message}", e)
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
                    Toast.makeText(this@MainActivity, "No MP3 files found in Drive!", Toast.LENGTH_LONG).show()
                    loadSongsInAdapter(dummySongsList)
                } else {
                    loadSongsInAdapter(realSongs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Drive Fetch Exception: ${e.message}", e)
                Toast.makeText(this@MainActivity, "Session refresh required. Please re-login if songs don't load.", Toast.LENGTH_LONG).show()
                loadSongsInAdapter(dummySongsList)
            }
        }
    }

    private fun loadSongsInAdapter(songs: List<Song>) {
        val adapter = SongAdapter(songs) { song ->
            playSong(song)
        }
        rvSongs.adapter = adapter
    }

    private fun playSong(song: Song) {
        if (song.url.isEmpty()) {
            Toast.makeText(this, "No stream link available for backup items", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Playing: ${song.title}", Toast.LENGTH_SHORT).show()

        try {
            // 🛠️ BUG FIX 2: Player state reset ensure karein taaki multiple audios merge na hon
            initializePlayer()

            exoPlayer?.apply {
                stop()
                clearMediaItems()

                val mediaItem = MediaItem.fromUri(song.url)
                setMediaItem(mediaItem)

                prepare()
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "ExoPlayer Error: ${e.message}", e)
            Toast.makeText(this, "Playback Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // App background mein jane par resource optimize rahein
    override fun onStop() {
        super.onStop()
        // Agar aap chahte ho app minimize hone par music chalta rahe, toh ise comment rakhna.
        // Agar chahte ho band ho jaye, toh yahan exoPlayer?.stop() daal sakte hain.
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}