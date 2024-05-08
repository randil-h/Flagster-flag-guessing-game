package com.example.flags

import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var soundSwitch: MaterialSwitch
    private var isHapticFeedbackEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT // Game is not distorted when rotated

        val sharedPreferences = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        soundSwitch = findViewById(R.id.sound_switch)

        // Set initial switch states based on saved preferences
        soundSwitch.isChecked = sharedPreferences.getBoolean("SoundEnabled", true)

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveSwitchState("SoundEnabled", isChecked)
        }

        val clearHighScoreBtn = findViewById<Button>(R.id.clear_high_score_btn)
        clearHighScoreBtn.setOnClickListener {
            clearHighScoreConfirmationPopup() // Pop up when high score clear button is clicked
        }
    }

    private fun saveSwitchState(key: String, isChecked: Boolean) {
        val sharedPreferences = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, isChecked)
        editor.apply()
    }


    private fun clearHighScoreConfirmationPopup() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.clear_high_score_confirmation)  // Display content
        val confirmClearBtn = dialog.findViewById<Button>(R.id.confirm_clear_high_score_btn)
        confirmClearBtn.setOnClickListener {
            dialog.dismiss() // Close pop up when clear clicked
            val sharedPreferences = getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putInt("HighScore", 0) // Update shared preferences
            editor.apply()
            val rootView = findViewById<View>(android.R.id.content)
            val snackbar = Snackbar.make(rootView, "High Score Cleared", Snackbar.LENGTH_LONG).apply {
                setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.black)) // Set text color
                duration = Snackbar.LENGTH_LONG // Set duration
                animationMode = Snackbar.ANIMATION_MODE_SLIDE // Set animation mode
                setBackgroundTint(ContextCompat.getColor(this@SettingsActivity, R.color.yellow)) // Set background tint color
                show()
            }
        }
        val cancelClearBtn = dialog.findViewById<Button>(R.id.cancel_clear_high_score_btn)
        cancelClearBtn.setOnClickListener{
            dialog.dismiss() // Close pop up when cancel is clicked
        }
        dialog.show()
    }
}
