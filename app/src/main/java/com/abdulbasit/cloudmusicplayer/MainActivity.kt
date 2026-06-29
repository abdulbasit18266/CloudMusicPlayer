package com.abdulbasit.cloudmusicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        checkExistingUserSession()

        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            loginLauncher.launch(signInIntent)
        }
    }

    private fun playSong(song: Song) {
        if (song.url.isEmpty()) {
            Toast.makeText(this, "No stream link available for backup items", Toast.LENGTH_SHORT).show()
            return
        }

        // Song details intent ke zariye naye PlayerActivity page par redirect kar rahe hain
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("SONG_TITLE", song.title)
            putExtra("SONG_URL", song.url)
        }
        startActivity(intent)
    }

    private fun checkExistingUserSession() {
        googleSignInClient.silentSignIn()
            .addOnSuccessListener { account ->
                btnGoogleLogin.text = "Logged In as ${account.displayName}"
                fetchRealDriveSongs(account)
            }
            .addOnFailureListener { e ->
                loadSongsInAdapter(dummySongsList)
            }
    }

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                btnGoogleLogin.text = "Logged In as ${account?.displayName}"
                if (account != null) fetchRealDriveSongs(account)
            } catch (e: Exception) {
                Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchRealDriveSongs(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(DriveScopes.DRIVE_READONLY)
        ).setSelectedAccount(account.account)

        val googleDriveService = Drive.Builder(
            NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
        ).setApplicationName("CloudMusicPlayer").build()

        val driveHelper = DriveServiceHelper(googleDriveService)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val realSongs = driveHelper.queryMp3Files()
                if (realSongs.isEmpty()) {
                    loadSongsInAdapter(dummySongsList)
                } else {
                    loadSongsInAdapter(realSongs)
                }
            } catch (e: Exception) {
                loadSongsInAdapter(dummySongsList)
            }
        }
    }

    private fun loadSongsInAdapter(songs: List<Song>) {
        val adapter = SongAdapter(songs) { song -> playSong(song) }
        rvSongs.adapter = adapter
    }
}