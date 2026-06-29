package com.abdulbasit.cloudmusicplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abdulbasit.cloudmusicplayer.adapter.SongAdapter
import com.abdulbasit.cloudmusicplayer.model.Song
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

class MainActivity : AppCompatActivity() {

    // 🔴 APNI NOTEPAD WALI WEB CLIENT ID YAHA PASTE KARO
    private val webClientId = "939513048631-dvs53ktq14qmen9uoilavb4nnrlittle.apps.googleusercontent.com"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rvSongs: RecyclerView
    private lateinit var btnGoogleLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        rvSongs = findViewById(R.id.rvSongs)

        // RecyclerView settings
        rvSongs.layoutManager = LinearLayoutManager(this)

        // Google Sign-In options setup (Drive Scope ke sath)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY)) // Sirf files read karne ki permission
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check agar user pehle se login hai
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            btnGoogleLogin.text = "Logged In as ${account.displayName}"
            loadDummySongs()
        }

        // Login Button Click Listener
        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            loginLauncher.launch(signInIntent)
        }
    }

    // Login Result Callback
    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                Toast.makeText(this, "Welcome ${account?.displayName}", Toast.LENGTH_SHORT).show()
                btnGoogleLogin.text = "Logged In as ${account?.displayName}"

                // Login hone ke baad list load karo
                loadDummySongs()
            } catch (e: Exception) {
                Toast.makeText(this, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Sign-In cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Testing ke liye sample list dikhane ke liye function
    private var dummySongsList = listOf(
        Song("1", "Dil De Diya Hai.mp3"),
        Song("2", "Tum Hi Ho.mp3"),
        Song("3", "Kesariya.mp3")
    )

    private fun loadDummySongs() {
        val adapter = SongAdapter(dummySongsList) { song ->
            Toast.makeText(this, "Clicked: ${song.name}", Toast.LENGTH_SHORT).show()
        }
        rvSongs.adapter = adapter
    }
}