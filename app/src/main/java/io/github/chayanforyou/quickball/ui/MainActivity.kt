package io.github.chayanforyou.quickball.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.core.DeviceAdminReceiver
import io.github.chayanforyou.quickball.core.QuickBallAccessibilityService
import io.github.chayanforyou.quickball.databinding.ActivityMainBinding
import io.github.chayanforyou.quickball.domain.PreferenceManager


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private var currentPermission: PermissionType? = null

    private val devicePolicyManager: DevicePolicyManager by lazy {
        getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val adminComponent: ComponentName by lazy {
        ComponentName(this, DeviceAdminReceiver::class.java)
    }

    enum class PermissionType { ACCESSIBILITY, SYSTEM_SETTINGS, DEVICE_ADMIN }

    private val checkPermissionRunnable = object : Runnable {
        override fun run() {
            when (currentPermission) {
                PermissionType.ACCESSIBILITY -> {
                    if (isAccessibilityServiceEnabled()) {
                        bringAppToFront()
                        return
                    }
                }

                PermissionType.SYSTEM_SETTINGS -> {
                    if (canModifySystemSettings()) {
                        bringAppToFront()
                        return
                    }
                }

                PermissionType.DEVICE_ADMIN -> {
                    if (isDeviceAdminEnabled()) {
                        bringAppToFront()
                        return
                    }
                }

                null -> return
            }

            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val activityPadding = resources.getDimension(R.dimen.activity_padding).toInt()
            view.setPadding(
                systemBars.left + activityPadding,
                systemBars.top + activityPadding,
                systemBars.right + activityPadding,
                systemBars.bottom + activityPadding
            )
            insets
        }

        setupClickListeners()
        updatePermissionStates()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
    }

    // Event Listeners
    private fun setupClickListeners() {
        binding.switchEnableQuickBall.setOnCheckedChangeListener { _, isChecked ->
            when {
                isChecked && hasAllRequiredPermissions() -> enableQuickBall()
                isChecked && !hasAllRequiredPermissions() -> handleMissingPermissions()
                !isChecked -> disableQuickBall()
            }
        }

        binding.layoutAccessibilityPermission.setOnClickListener {
            when {
                !isAccessibilityServiceEnabled() -> openAccessibilitySettings()
                else -> showToast("Accessibility service is already enabled!")
            }
        }

        binding.layoutSystemSettingsPermission.setOnClickListener {
            when {
                !canModifySystemSettings() -> requestModifySystemSettingsPermission()
                else -> showToast("System settings permission is already granted!")
            }
        }

        binding.layoutDeviceAdminPermission.setOnClickListener {
            when {
                !isDeviceAdminEnabled() -> requestDeviceAdminPermission()
                else -> showToast("Device administrator permission is already granted!")
            }
        }
    }

    // Quick Ball Control
    private fun enableQuickBall() {
        PreferenceManager.setQuickBallEnabled(this, true)
        startService(Intent(this, QuickBallAccessibilityService::class.java)
            .setAction(QuickBallAccessibilityService.ACTION_ENABLE_QUICK_BALL))
        showToast("Quick Ball is now enabled!")
    }

    private fun disableQuickBall() {
        PreferenceManager.setQuickBallEnabled(this, false)
        startService(Intent(this, QuickBallAccessibilityService::class.java)
            .setAction(QuickBallAccessibilityService.ACTION_DISABLE_QUICK_BALL))
        showToast("Quick Ball is now disabled")
    }

    private fun handleMissingPermissions() {
        binding.switchEnableQuickBall.isChecked = false
        showToast("Please grant all required permissions first")
    }

    // Permission Management
    private fun updatePermissionStates() {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val systemSettingsEnabled = canModifySystemSettings()
        val deviceAdminEnabled = isDeviceAdminEnabled()
        val allPermissionsGranted = hasAllRequiredPermissions()

        binding.imgAccessibilityStatus.setImageResource(
            if (accessibilityEnabled) R.drawable.ic_checked else R.drawable.ic_unchecked
        )

        binding.imgSystemSettingsStatus.setImageResource(
            if (systemSettingsEnabled) R.drawable.ic_checked else R.drawable.ic_unchecked
        )

        // Show only for older versions where accessibility service lock might not work reliably
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.layoutDeviceAdminPermission.visibility = View.GONE
        } else {
            binding.layoutDeviceAdminPermission.visibility = View.VISIBLE
            binding.imgDeviceAdminStatus.setImageResource(
                if (deviceAdminEnabled) R.drawable.ic_checked else R.drawable.ic_unchecked
            )
        }

        binding.switchEnableQuickBall.isEnabled = allPermissionsGranted

        // Turn off switch if permissions are revoked
        if (!allPermissionsGranted && binding.switchEnableQuickBall.isChecked) {
            binding.switchEnableQuickBall.isChecked = false
        }

        // Restore saved state if permissions are available
        if (allPermissionsGranted) {
            binding.switchEnableQuickBall.isChecked = PreferenceManager.isQuickBallEnabled(this)
        }
    }

    private fun hasAllRequiredPermissions(): Boolean {
        return isAccessibilityServiceEnabled() && canModifySystemSettings()
    }

    // Permission Checks
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )

        return enabledServices.any { service ->
            service.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    private fun canModifySystemSettings(): Boolean {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Settings.System.canWrite(this)
                else -> true // Older versions don't require this permission
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isDeviceAdminEnabled(): Boolean {
        return try {
            devicePolicyManager.isAdminActive(adminComponent)
        } catch (_: Exception) {
            false
        }
    }

    // Permission Requests
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            requestPermission(intent, PermissionType.ACCESSIBILITY)
        } catch (_: Exception) {
            showToast("Could not open accessibility settings")
        }
    }

    private fun requestModifySystemSettingsPermission() {
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = "package:$packageName".toUri()
                    }
                    requestPermission(intent, PermissionType.SYSTEM_SETTINGS)
                }
                else -> showToast("System settings permission is not required on this Android version")
            }
        } catch (_: Exception) {
            showToast("Could not request system settings permission")
        }
    }

    private fun requestDeviceAdminPermission() {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                    "QuickBall needs device admin permission to lock your screen on older Android versions and Xiaomi devices.")
            }
            requestPermission(intent, PermissionType.DEVICE_ADMIN)
        } catch (_: Exception) {
            showToast("Could not open device admin settings")
        }
    }

    private fun requestPermission(intent: Intent, permission: PermissionType) {
        currentPermission = permission
        startActivity(intent)
        handler.postDelayed(checkPermissionRunnable, 1000)
    }

    private fun bringAppToFront() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        currentPermission = null
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}