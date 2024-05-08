package com.example.flags

import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home) //load UI
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT  //lock to portrait to ensure no distortion
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logo = findViewById<ImageView>(R.id.home_logo)
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_logo)
        logo.startAnimation(rotateAnimation) //animate logo upon opening

        val newGameBtn = findViewById<Button>(R.id.home_new_game_btn)
        newGameBtn.setOnClickListener {
            showInstructionsPopup() //Give instructions when new game button is clicked
        }

        val settingsBtn = findViewById<Button>(R.id.home_settings_btn)
        settingsBtn.setOnClickListener{
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent) //Navigate to settings
        }

        val exitBtn = findViewById<Button>(R.id.home_exit_btn)
        exitBtn.setOnClickListener{
            finish() //Close App
        }
    }

    private fun showInstructionsPopup() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.instructions_popup) //show pop up
        val startGameBtn = dialog.findViewById<Button>(R.id.popup_start_game_btn)
        startGameBtn.setOnClickListener {
            dialog.dismiss() //close dialog
            startGame() //start game
        }
        dialog.show() //show dialog
    }

    private fun startGame() {
        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)  //Start game when clicked
    }
}