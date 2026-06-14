package com.pine.launcher

import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PineSettingsActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.hide()
        window.statusBarColor = android.graphics.Color.parseColor("#0A0A0C")

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        findViewById<View>(R.id.btn_back_settings).setOnClickListener { finish() }

        // Secret trigger: volume slider at max -> open secret settings
        val volumeSlider = findViewById<SeekBar>(R.id.volume_slider)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeSlider.max = maxVol
        volumeSlider.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        volumeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                if (progress >= maxVol) triggerSecret()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // Normal settings options
        val switchTransition = findViewById<Switch>(R.id.switch_transition)
        val switchGrid = findViewById<Switch>(R.id.switch_grid_labels)
        switchTransition.isChecked = getSharedPreferences("pine_prefs", MODE_PRIVATE)
            .getBoolean("transition_anim", true)
        switchTransition.setOnCheckedChangeListener { _, checked ->
            getSharedPreferences("pine_prefs", MODE_PRIVATE).edit()
                .putBoolean("transition_anim", checked).apply()
        }
        switchGrid.isChecked = getSharedPreferences("pine_prefs", MODE_PRIVATE)
            .getBoolean("show_labels", true)
        switchGrid.setOnCheckedChangeListener { _, checked ->
            getSharedPreferences("pine_prefs", MODE_PRIVATE).edit()
                .putBoolean("show_labels", checked).apply()
        }
    }

    private fun triggerSecret() {
        // Navigate to secret hidden apps manager
        startActivity(android.content.Intent(this, SecretSettingsActivity::class.java))
    }
}
