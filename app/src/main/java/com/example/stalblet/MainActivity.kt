package com.example.stalblet

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.firebase.ui.auth.AuthUI
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 1234
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1) Hook up your toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 2) FAB logic (only show on MapFragment)
        fabAdd = findViewById(R.id.fab_add)
        fabAdd.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, AddSubletFragment())
                .addToBackStack(null)
                .commit()
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val curr = supportFragmentManager.findFragmentById(R.id.auth_container)
            if (curr is MapFragment) fabAdd.show() else fabAdd.hide()
        }
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) startSignIn() else showMapFragment()
    }

    private fun startSignIn() {
        val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false, false)
                .build(),
            RC_SIGN_IN
        )
    }

    private fun showMapFragment() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.auth_container, MapFragment())
            .commit()
    }

    // ── Inflate the menu_main.xml into the toolbar ───────────────────────
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // ── Handle toolbar item clicks ─────────────────────────────────────
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_chats -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, ChatListFragment())
                    .addToBackStack(null)
                    .commit()
                return true
            }
            R.id.action_sign_out -> {
                signOut()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun signOut() {
        fabAdd.hide()
        AuthUI.getInstance().signOut(this)
            .addOnCompleteListener { startSignIn() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) showMapFragment() else finish()
        }
    }
}
