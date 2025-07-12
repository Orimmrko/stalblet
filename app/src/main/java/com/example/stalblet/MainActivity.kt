
package com.example.stalblet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.example.stalblet.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Explicitly initialize Firebase *in this Activity*
        FirebaseApp.initializeApp(this)

        // 2) Only now do we get Auth
        auth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_main)
        findViewById<FloatingActionButton>(R.id.fab_add)
            .setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, AddSubletFragment.newInstance())
                    .addToBackStack(null)
                    .commit()}

        if (auth.currentUser == null) {
            startSignInFlow()
        } else {
            launchMapScreen()
        }
    }

    private fun startSignInFlow() {
        val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
        )
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)    // ← disable Smart Lock/hint picker
            .build()
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == RESULT_OK) {
                Toast.makeText(
                    this,
                    "Welcome, ${auth.currentUser?.displayName
                        ?: auth.currentUser?.phoneNumber}",
                    Toast.LENGTH_SHORT
                ).show()
                launchMapScreen()
            } else {
                Toast.makeText(
                    this,
                    "Sign-in failed: ${response?.error?.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun launchMapScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.auth_container, MapFragment.newInstance())
            .commit()
    }
}
