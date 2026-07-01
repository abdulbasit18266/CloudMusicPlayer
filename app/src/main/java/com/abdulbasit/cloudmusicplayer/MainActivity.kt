package com.abdulbasit.cloudmusicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
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

    // Phase 10: Profile Circle View Object
    private lateinit var btnProfileMenuTrigger: android.widget.ImageView
    private var currentUserEmail: String = "No Email Found"

    private var dummySongsList = listOf(
        Song("1", "Dil De Diya Hai.mp3", ""),
        Song("2", "Tum Hi Ho.mp3", ""),
        Song("3", "Kesariya.mp3", "")
    )

    // Phase 11: Tracking active playlist globally
    private var currentSongsList: List<Song> = dummySongsList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializing Views safely
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        rvSongs = findViewById(R.id.rvSongs)
        rvSongs.layoutManager = LinearLayoutManager(this)

        // Phase 10: Mapping ONLY the active profile trigger icon
        btnProfileMenuTrigger = findViewById(R.id.btnProfileMenuTrigger)

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

        // Profile Icon click karne par Premium Dropdown dikhana
        btnProfileMenuTrigger.setOnClickListener { view ->
            showProfilePopupMenu(view)
        }
    }
    // Phase 10: Dynamic Drop-down Generator
    private fun showProfilePopupMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)

        // Menu item programmatically inject kar rahe hain (1: Email Address, 2: Sign Out Option)
        popup.menu.add(0, 1, 0, currentUserEmail).isEnabled = false // Email non-clickable rahegi (Just label)
        popup.menu.add(0, 2, 1, "Sign Out")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                2 -> { // Sign Out Action
                    googleSignInClient.signOut().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
                            // Reset State
                            btnProfileMenuTrigger.visibility = View.GONE
                            btnGoogleLogin.visibility = View.VISIBLE
                            loadSongsInAdapter(dummySongsList)
                        } else {
                            Toast.makeText(this, "Sign Out Failed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // Phase 11: Enhanced playSong that wraps the entire list architecture
    private fun playSong(selectedSong: Song) {
        if (selectedSong.url.isEmpty()) {
            Toast.makeText(this, "No stream link available for backup items", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Pure titles aur URLs ki string list alag kar rahe hain intent compatible banane ke liye
        val titlesList = ArrayList<String>()
        val urlsList = ArrayList<String>()
        var targetPosition = 0

        currentSongsList.forEachIndexed { index, song ->
            titlesList.add(song.title)
            urlsList.add(song.url)
            if (song.id == selectedSong.id) {
                targetPosition = index // Klik kiye huye gaane ka exact index position map kar rahe hain
            }
        }

        // 2. Intent ke sath lists aur position dono forward kar rahe hain
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putStringArrayListExtra("SONG_TITLES_LIST", titlesList)
            putStringArrayListExtra("SONG_URLS_LIST", urlsList)
            putExtra("SONG_POSITION", targetPosition)
        }
        startActivity(intent)
    }

    private fun checkExistingUserSession() {
        googleSignInClient.silentSignIn()
            .addOnSuccessListener { account ->
                updateUiForLoggedInUser(account)
                fetchRealDriveSongs(account)
            }
            .addOnFailureListener { e ->
                btnProfileMenuTrigger.visibility = View.GONE
                btnGoogleLogin.visibility = View.VISIBLE
                loadSongsInAdapter(dummySongsList)
            }
    }

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                if (account != null) {
                    updateUiForLoggedInUser(account)
                    fetchRealDriveSongs(account)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUiForLoggedInUser(account: GoogleSignInAccount) {
        btnGoogleLogin.visibility = View.GONE
        btnProfileMenuTrigger.visibility = View.VISIBLE
        currentUserEmail = account.email ?: "No Email Linked"
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
        // Phase 11: Updating the global tracker
        currentSongsList = songs
        val adapter = SongAdapter(songs) { song -> playSong(song) }
        rvSongs.adapter = adapter
    }
}