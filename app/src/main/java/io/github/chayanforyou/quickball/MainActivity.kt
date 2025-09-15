package io.github.chayanforyou.quickball

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var enableButton: Button
    private lateinit var settingsButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initViews()
        setupClickListeners()
        updateServiceStatus()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        enableButton = findViewById(R.id.enableButton)
        settingsButton = findViewById(R.id.settingsButton)
    }
    
    private fun setupClickListeners() {
        enableButton.setOnClickListener {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Service is already enabled!", Toast.LENGTH_SHORT).show()
            } else {
                openAccessibilitySettings()
            }
        }
        
        settingsButton.setOnClickListener {
            openAccessibilitySettings()
        }
    }
    
    private fun updateServiceStatus() {
        if (isAccessibilityServiceEnabled()) {
            statusText.text = "QuickBall Service is ENABLED\nFloating ball should be visible on your screen!"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            enableButton.text = "Service Enabled"
            enableButton.isEnabled = false
        } else {
            statusText.text = "QuickBall Service is DISABLED\nPlease enable the accessibility service to use the floating ball."
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            enableButton.text = "Enable Service"
            enableButton.isEnabled = true
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }
    
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Please enable 'QuickBall' in the accessibility services list", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open accessibility settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }
}