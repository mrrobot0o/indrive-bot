package com.indriver.bot.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.indriver.bot.R
import com.indriver.bot.databinding.ActivityMainBinding
import com.indriver.bot.service.BotService
import com.indriver.bot.utils.PermissionHelper
import com.indriver.bot.utils.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferenceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferenceManager(this)
        
        setupUI()
        checkPermissions()
        loadStats()
    }

    private fun setupUI() {
        binding.switchAutoAccept.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !checkAllPermissions()) {
                binding.switchAutoAccept.isChecked = false
                return@setOnCheckedChangeListener
            }
            toggleBot(isChecked)
        }
        
        binding.btnSettings.setOnClickListener { showSettingsDialog() }
        binding.btnStats.setOnClickListener { showStatsDialog() }
        binding.btnGrantPermissions.setOnClickListener { requestAllPermissions() }
    }

    private fun toggleBot(enable: Boolean) {
        if (enable) {
            BotService.start(this)
            updateStatusRunning()
            Toast.makeText(this, "🟢 Bot Started!", Toast.LENGTH_SHORT).show()
        } else {
            BotService.stop(this)
            updateStatusStopped()
            Toast.makeText(this, "🔴 Bot Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatusRunning() {
        binding.tvStatus.text = "🟢 RUNNING"
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green_500))
    }

    private fun updateStatusStopped() {
        binding.tvStatus.text = "🔴 STOPPED"
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red_500))
    }

    private fun updateStatusPermissionNeeded() {
        binding.tvStatus.text = "⚠️ PERMISSIONS NEEDED"
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_500))
    }

    private fun checkPermissions(): Boolean {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = PermissionHelper.isAccessibilityEnabled(this)
        
        if (!hasOverlay || !hasAccessibility) {
            updateStatusPermissionNeeded()
            binding.btnGrantPermissions.visibility = View.VISIBLE
            return false
        }
        
        binding.btnGrantPermissions.visibility = View.GONE
        return true
    }

    private fun checkAllPermissions(): Boolean {
        if (!checkPermissions()) {
            showPermissionExplanation()
            return false
        }
        return true
    }

    private fun requestAllPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            return
        }
        
        if (!PermissionHelper.isAccessibilityEnabled(this)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivityForResult(intent, REQUEST_ACCESSIBILITY_PERMISSION)
        }
    }

    private fun showPermissionExplanation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs:\n\n📱 Overlay Permission\n♿ Accessibility Service\n\nPlease grant these permissions to continue.")
            .setPositiveButton("Grant") { _, _ -> requestAllPermissions() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadStats() {
        binding.tvAccepted.text = prefs.getAcceptedCount().toString()
        binding.tvRejected.text = prefs.getMissedCount().toString()
        binding.tvEarnings.text = "$${String.format("%.2f", prefs.getTotalEarnings())}"
    }

    private fun showSettingsDialog() {
        Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showStatsDialog() {
        val stats = """
            📊 Statistics
            
            ✅ Accepted: ${prefs.getAcceptedCount()}
            ❌ Missed: ${prefs.getMissedCount()}
            💰 Earnings: $${String.format("%.2f", prefs.getTotalEarnings())}
            📈 Win Rate: ${String.format("%.1f", prefs.getWinRate())}%
        """.trimIndent()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("📈 Statistics")
            .setMessage(stats)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                if (Settings.canDrawOverlays(this)) requestAllPermissions()
            }
            REQUEST_ACCESSIBILITY_PERMISSION -> {
                if (PermissionHelper.isAccessibilityEnabled(this)) {
                    checkPermissions()
                    Toast.makeText(this, "✅ All permissions granted!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        loadStats()
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_ACCESSIBILITY_PERMISSION = 1002
    }
}
