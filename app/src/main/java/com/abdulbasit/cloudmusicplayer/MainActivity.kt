package com.abdulbasit.cloudmusicplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abdulbasit.cloudmusicplayer.adapter.FolderAdapter
import com.abdulbasit.cloudmusicplayer.adapter.SongAdapter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

    private lateinit var llBackToFoldersContainer: LinearLayout // layout wrap-up check

    private val webClientId = "939513048631-dvs53ktq14qmen9uoilavb4nnrlittle.apps.googleusercontent.com"
    private val TAG = "CLOUD_MUSIC_DEBUG"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rvSongs: RecyclerView
    private lateinit var btnGoogleLogin: Button
    private lateinit var btnProfileMenuTrigger: ImageView
    private lateinit var btnRefresh: FloatingActionButton

    private var currentUserEmail: String = "No Email Found"
    private var isRefreshing: Boolean = false

    // State management for navigation
    private var currentFoldersList: List<Folder> = emptyList()
    private var currentSongsList: List<Song> = emptyList()
    private var isInsideFolder: Boolean = false // Track karega ki user folder ke andar hai ya main screen par

    private val dummySongsList = listOf(
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
        btnProfileMenuTrigger = findViewById(R.id.btnProfileMenuTrigger)
        btnRefresh = findViewById(R.id.btnRefresh)

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

        btnProfileMenuTrigger.setOnClickListener { view ->
            showProfilePopupMenu(view)
        }

        btnRefresh.setOnClickListener {
            handleRefreshAction()
        }

        // Handle Back Press: Agar user kisi folder ke andar hai toh back dabane par folder list par aaye, app band na ho
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isInsideFolder) {
                    // Wapas main folder list load karo
                    displayFolders(currentFoldersList)
                } else {
                    // Agar pehle se bahar hai toh normal back behaviour (app close)
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        llBackToFoldersContainer = findViewById(R.id.llBackToFoldersContainer)
        llBackToFoldersContainer.setOnClickListener {
            displayFolders(currentFoldersList) // Back dabane par folders par wapas le jaye
        }
    }

    private fun showProfilePopupMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add(0, 1, 0, currentUserEmail).isEnabled = false
        popup.menu.add(0, 2, 1, "Sign Out")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                2 -> {
                    googleSignInClient.signOut().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
                            btnProfileMenuTrigger.visibility = View.GONE
                            btnGoogleLogin.visibility = View.VISIBLE
                            isInsideFolder = false
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

    private fun checkExistingUserSession() {
        googleSignInClient.silentSignIn()
            .addOnSuccessListener { account ->
                updateUiForLoggedInUser(account)
                fetchDriveFolders(account)
            }
            .addOnFailureListener {
                btnProfileMenuTrigger.visibility = View.GONE
                btnGoogleLogin.visibility = View.VISIBLE
                isInsideFolder = false
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
                    fetchDriveFolders(account)
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

    private fun handleRefreshAction() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            Toast.makeText(this, "Please login first to refresh!", Toast.LENGTH_SHORT).show()
            return
        }

        if (isRefreshing) {
            Toast.makeText(this, "Refreshing in progress...", Toast.LENGTH_SHORT).show()
            return
        }

        isRefreshing = true
        fetchDriveFolders(account)
    }

    // STEP 1: Google Drive se saare folders nikalna
    private fun fetchDriveFolders(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(DriveScopes.DRIVE_READONLY)
        ).setSelectedAccount(account.account)

        val googleDriveService = Drive.Builder(
            NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
        ).setApplicationName("CloudMusicPlayer").build()

        val driveHelper = DriveServiceHelper(googleDriveService)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val folders = driveHelper.queryFolders()
                currentFoldersList = folders
                if (folders.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No folders found in Drive", Toast.LENGTH_SHORT).show()
                    isInsideFolder = false
                    loadSongsInAdapter(dummySongsList)
                } else {
                    displayFolders(folders)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load folders: ${e.message}", Toast.LENGTH_SHORT).show()
                isInsideFolder = false
                loadSongsInAdapter(dummySongsList)
            } finally {
                isRefreshing = false
            }
        }
    }

    // STEP 2: Folders ko list mein show karna
    private fun displayFolders(folders: List<Folder>) {
        isInsideFolder = false
        llBackToFoldersContainer.visibility = View.GONE // Main page par back button chipa do
        val folderAdapter = FolderAdapter(folders) { selectedFolder ->
            // Jab user folder par click karega, tab uske songs fetch honge
            val account = GoogleSignIn.getLastSignedInAccount(this)
            if (account != null) {
                fetchSongsFromFolder(account, selectedFolder.id)
            }
        }
        rvSongs.adapter = folderAdapter
    }

    // STEP 3: Clicked folder ke andar se MP3 files fetch karna
    private fun fetchSongsFromFolder(account: GoogleSignInAccount, folderId: String) {
        val credential = GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(DriveScopes.DRIVE_READONLY)
        ).setSelectedAccount(account.account)

        val googleDriveService = Drive.Builder(
            NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
        ).setApplicationName("CloudMusicPlayer").build()

        val driveHelper = DriveServiceHelper(googleDriveService)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val realSongs = driveHelper.queryMp3FilesFromFolder(folderId)
                if (realSongs.isEmpty()) {
                    Toast.makeText(this@MainActivity, "This folder is empty", Toast.LENGTH_SHORT).show()
                } else {
                    loadSongsInAdapter(realSongs)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error fetching songs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSongsInAdapter(songs: List<Song>) {
        currentSongsList = songs
        isInsideFolder = songs != dummySongsList // Agar dummy list nahi hai, toh user folder ke andar hai
        val adapter = SongAdapter(songs) { song -> playSong(song) }
        rvSongs.adapter = adapter
        if (isInsideFolder) {
            llBackToFoldersContainer.visibility = View.VISIBLE // Folder ke andar aate hi button dikhao
        } else {
            llBackToFoldersContainer.visibility = View.GONE
        }
    }

    private fun playSong(selectedSong: Song) {
        if (selectedSong.url.isEmpty()) {
            Toast.makeText(this, "No stream link available for backup items", Toast.LENGTH_SHORT).show()
            return
        }

        val titlesList = ArrayList<String>()
        val urlsList = ArrayList<String>()
        var targetPosition = 0

        currentSongsList.forEachIndexed { index, song ->
            titlesList.add(song.title)
            urlsList.add(song.url)
            if (song.id == selectedSong.id) {
                targetPosition = index
            }
        }

        val intent = Intent(this, PlayerActivity::class.java).apply {
            putStringArrayListExtra("SONG_TITLES_LIST", titlesList)
            putStringArrayListExtra("SONG_URLS_LIST", urlsList)
            putExtra("SONG_POSITION", targetPosition)
        }
        startActivity(intent)
    }
}