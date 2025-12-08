package io.github.chayanforyou.quickball.ui.fragments

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.github.chayanforyou.quickball.BuildConfig
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.core.QuickBallService
import io.github.chayanforyou.quickball.databinding.FragmentQuickballHomeBinding
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.utils.DialogUtil

class QuickBallHomeFragment : Fragment() {

    private var _binding: FragmentQuickballHomeBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private var currentPermission: PermissionType? = null

    enum class PermissionType { ACCESSIBILITY, SYSTEM_SETTINGS }

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

                null -> return
            }

            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuickballHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updatePermissionStates()
        showVersionInfo()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
        checkAndShowPermissionDialogs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(checkPermissionRunnable)
        _binding = null
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

        binding.switchEnableOnLockScreen.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.setShowOnLockScreenEnabled(requireContext(), isChecked)
        }

        binding.layoutShortcutsSelection.setOnClickListener {
            val action = QuickBallHomeFragmentDirections.actionQuickBallHomeFragmentToShortcutMenuFragment()
            findNavController().navigate(action)
        }

        binding.layoutAutoHideSettings.setOnClickListener {
            val action = QuickBallHomeFragmentDirections.actionQuickBallHomeFragmentToAutoHideSettingsFragment()
            findNavController().navigate(action)
        }

        binding.buttonSupportMe.setOnClickListener {

        }

        binding.layoutFooter.setOnClickListener {
            val url = "https://github.com/chayanforyou/QuickBall"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
    }

    private fun showVersionInfo() {
        binding.tvVersion.text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
    }

    // Quick Ball Control
    private fun enableQuickBall() {
        requireContext().let { context ->
            PreferenceManager.setQuickBallEnabled(context, true)
            context.startService(Intent(context, QuickBallService::class.java)
                .setAction(QuickBallService.ACTION_ENABLE_QUICK_BALL))
        }
    }

    private fun disableQuickBall() {
        requireContext().let { context ->
            PreferenceManager.setQuickBallEnabled(context, false)
            context.startService(Intent(context, QuickBallService::class.java)
                .setAction(QuickBallService.ACTION_DISABLE_QUICK_BALL))
        }
    }

    private fun handleMissingPermissions() {
        binding.switchEnableQuickBall.isChecked = false
        showToast("Please grant all required permissions first")
    }

    // Permission Management
    private fun updatePermissionStates() {
        val allPermissionsGranted = hasAllRequiredPermissions()

        binding.switchEnableQuickBall.isEnabled = allPermissionsGranted

        // Turn off switch if permissions are revoked
        if (!allPermissionsGranted && binding.switchEnableQuickBall.isChecked) {
            binding.switchEnableQuickBall.isChecked = false
        }

        // Restore saved state if permissions are available
        if (allPermissionsGranted) {
            binding.switchEnableQuickBall.isChecked = PreferenceManager.isQuickBallEnabled(requireContext())
            binding.switchEnableQuickBall.jumpDrawablesToCurrentState()
        }
        
        // Restore lock screen preference
        binding.switchEnableOnLockScreen.isChecked = PreferenceManager.isShowOnLockScreenEnabled(requireContext())
        binding.switchEnableOnLockScreen.jumpDrawablesToCurrentState()
    }

    private fun hasAllRequiredPermissions(): Boolean {
        return isAccessibilityServiceEnabled() && canModifySystemSettings()
    }

    // Permission Checks
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(requireContext(), AccessibilityManager::class.java)
        val enabledServices = accessibilityManager?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ) ?: return false

        return enabledServices.any { service ->
            service.resolveInfo.serviceInfo.packageName == requireContext().packageName
        }
    }

    private fun canModifySystemSettings(): Boolean {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Settings.System.canWrite(requireContext())
                else -> true // Older versions don't require this permission
            }
        } catch (_: Exception) {
            false
        }
    }

    // Permission Requests
    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                val key = ComponentName(requireContext().packageName, QuickBallService::class.java.name).flattenToString()
                putExtra(":settings:fragment_args_key", key)
                putExtra(":settings:show_fragment_args", bundleOf(":settings:fragment_args_key" to key))
            }
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
                        data = "package:${requireContext().packageName}".toUri()
                    }
                    requestPermission(intent, PermissionType.SYSTEM_SETTINGS)
                }
                else -> showToast("System settings permission is not required on this Android version")
            }
        } catch (_: Exception) {
            showToast("Could not request system settings permission")
        }
    }

    private fun requestPermission(intent: Intent, permission: PermissionType) {
        currentPermission = permission
        startActivity(intent)
        handler.postDelayed(checkPermissionRunnable, 1000L)
    }

    private fun bringAppToFront() {
        val intent = Intent(requireContext(), requireActivity()::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        currentPermission = null
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    // Permission Dialog Management
    private fun checkAndShowPermissionDialogs() {
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityPermissionDialog()
            return
        }

        if (!canModifySystemSettings()) {
            showSystemSettingsPermissionDialog()
            return
        }
    }

    private fun showAccessibilityPermissionDialog() {
        DialogUtil.showAccessibilityPermissionDialog(
            context = requireContext(),
            onAccept = { openAccessibilitySettings() },
            onQuit = { requireActivity().finish() }
        )
    }

    private fun showSystemSettingsPermissionDialog() {
        DialogUtil.showSystemSettingsPermissionDialog(
            context = requireContext(),
            onAccept = { requestModifySystemSettingsPermission() },
            onQuit = { requireActivity().finish() }
        )
    }
}