package io.github.chayanforyou.quickball

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var enableButton: Button
    private lateinit var settingsButton: Button
    private lateinit var brightnessPermissionButton: Button
    
    // Activity result launcher for WRITE_SETTINGS permission
    private val writeSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check permission status after returning from settings
        updateServiceStatus()
    }
    
    // Activity result launcher for device admin permission
    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check device admin status after returning from settings
        updateServiceStatus()
    }
    
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
        brightnessPermissionButton = findViewById(R.id.brightnessPermissionButton)
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
        
        brightnessPermissionButton.setOnClickListener {
            checkAndRequestBrightnessPermission()
        }
    }
    
    private fun updateServiceStatus() {
        if (isAccessibilityServiceEnabled()) {
            val brightnessPermissionStatus = if (canModifyBrightness()) "✓" else "✗"
            val deviceAdminStatus = if (isDeviceAdminEnabled()) "✓" else "✗"
            statusText.text = "QuickBall Service is ENABLED\nFloating ball should be visible on your screen!\n\nBrightness Control: $brightnessPermissionStatus\nLock Screen: $deviceAdminStatus"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            enableButton.text = "Service Enabled"
            enableButton.isEnabled = false
            
            // Update brightness permission button
            updateBrightnessPermissionButton()
        } else {
            statusText.text = "QuickBall Service is DISABLED\nPlease enable the accessibility service to use the floating ball."
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            enableButton.text = "Enable Service"
            enableButton.isEnabled = true
            brightnessPermissionButton.isEnabled = false
        }
    }
    
    private fun updateBrightnessPermissionButton() {
        if (canModifyBrightness()) {
            brightnessPermissionButton.text = "Brightness Permission: ✓ Granted"
            brightnessPermissionButton.isEnabled = false
            brightnessPermissionButton.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            brightnessPermissionButton.text = "Grant Brightness Permission"
            brightnessPermissionButton.isEnabled = true
            brightnessPermissionButton.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
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
    
    private fun canModifyBrightness(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.System.canWrite(this)
            } else {
                true // For older versions, assume we can write
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isDeviceAdminEnabled(): Boolean {
        return try {
            val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
            devicePolicyManager.isAdminActive(adminComponent)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun requestDeviceAdminPermission() {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(this@MainActivity, DeviceAdminReceiver::class.java))
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "QuickBall needs device admin permission to lock the screen.")
            }
            deviceAdminLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open device admin settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkAndRequestBrightnessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            showBrightnessPermissionDialog()
        } else {
            Toast.makeText(this, "Brightness permission is already granted!", Toast.LENGTH_SHORT).show()
            updateServiceStatus()
        }
    }
    
    private fun showBrightnessPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Brightness Control Permission")
            .setMessage("QuickBall needs 'Modify system settings' permission to control screen brightness.")
            .setPositiveButton("Grant Permission") { _, _ -> requestBrightnessPermission() }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun requestBrightnessPermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            writeSettingsLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open system settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }
}