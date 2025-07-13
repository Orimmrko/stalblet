package com.example.stalblet


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.firebase.ui.auth.AuthUI
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // If you have a Toolbar:
        // setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startSignIn()
        } else {
            showMapFragment()
        }
    }

    private fun startSignIn() {
        val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
        )
        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false, false)
            .build()
        startActivityForResult(intent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Signed in
                showMapFragment()
            } else {
                // Sign-in canceled or failed; exit or re-prompt
                finish()
            }
        }
    }

    private fun showMapFragment() {
        // Clear back stack
        supportFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.auth_container, MapFragment())
            .commit()
        findViewById<FloatingActionButton>(R.id.fab_add)
            .apply {
                show()  // make sure it’s visible
                setOnClickListener {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.auth_container, AddSubletFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
    }
}
