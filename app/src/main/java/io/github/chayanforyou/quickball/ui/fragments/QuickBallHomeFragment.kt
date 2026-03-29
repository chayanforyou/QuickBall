package io.github.chayanforyou.quickball.ui.fragments

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
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
        showVersionInfo()
    }

    override fun onResume() {
        super.onResume()
        removeSwitchListeners()
        updatePermissionStates()
        updateSettingsEnabledState()
        setupSwitchListeners()
        checkAndShowPermissionDialogs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(checkPermissionRunnable)
        _binding = null
    }

    // Event Listeners
    private fun setupClickListeners() {
        binding.layoutShortcutsSelection.setOnClickListener {
            val action =
                QuickBallHomeFragmentDirections.actionQuickBallHomeFragmentToShortcutMenuFragment()
            findNavController().navigate(action)
        }

        binding.layoutAutoHideSettings.setOnClickListener {
            val action =
                QuickBallHomeFragmentDirections.actionQuickBallHomeFragmentToAutoHideSettingsFragment()
            findNavController().navigate(action)
        }

        binding.buttonSupportMe.setOnClickListener {
            val url = "https://chayanforyou.gumroad.com/coffee"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        binding.layoutFooter.setOnClickListener {
            val url = "https://github.com/chayanforyou/QuickBall"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }
    }

    private fun setupSwitchListeners() {
        binding.switchEnableQuickBall.setOnCheckedChangeListener { _, isChecked ->
            when {
                isChecked && hasAllRequiredPermissions() -> enableQuickBall()
                isChecked && !hasAllRequiredPermissions() -> handleMissingPermissions()
                !isChecked -> disableQuickBall()
            }

            updateSettingsEnabledState()
        }

        binding.switchStickToEdge.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) stashQuickBall() else unstashQuickBall()
        }

        binding.switchEnableOnLockScreen.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.setShowOnLockScreenEnabled(requireContext(), isChecked)
        }

        binding.switchHideOnLandscape.setOnCheckedChangeListener { _, isChecked ->
            PreferenceManager.setHideOnLandscapeEnabled(requireContext(), isChecked)
        }

        binding.sliderBallSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) updateQuickBallSize(value)
        }
    }

    private fun removeSwitchListeners() {
        binding.switchEnableQuickBall.setOnCheckedChangeListener(null)
        binding.switchStickToEdge.setOnCheckedChangeListener(null)
        binding.switchEnableOnLockScreen.setOnCheckedChangeListener(null)
        binding.switchHideOnLandscape.setOnCheckedChangeListener(null)
    }

    private fun updateSettingsEnabledState() {
        val isQuickBallOn = PreferenceManager.isQuickBallEnabled(requireContext())
        binding.touchBlockerView.visibility = if (isQuickBallOn) View.GONE else View.VISIBLE
        binding.settingsCard.alpha = if (isQuickBallOn) 1f else 0.6f
    }

    private fun showVersionInfo() {
        binding.tvVersion.text = String.format("v%s", BuildConfig.VERSION_NAME)
    }

    // Quick Ball Control
    private fun handleQuickBall(action: String, preferenceUpdate: (Context) -> Unit) {
        val context = requireContext()

        preferenceUpdate(context)

        context.startService(
            Intent(context, QuickBallService::class.java).apply {
                this.action = action
            }
        )
    }

    private fun enableQuickBall() {
        handleQuickBall(
            action = QuickBallService.ACTION_ENABLE
        ) {
            PreferenceManager.setQuickBallEnabled(it, true)
        }
    }

    private fun disableQuickBall() {
        handleQuickBall(
            action = QuickBallService.ACTION_DISABLE
        ) {
            PreferenceManager.setQuickBallEnabled(it, false)
        }
    }

    private fun stashQuickBall() {
        handleQuickBall(
            action = QuickBallService.ACTION_STASH
        ) {
            PreferenceManager.setStickToEdgeEnabled(it, true)
        }
    }

    private fun unstashQuickBall() {
        handleQuickBall(
            action = QuickBallService.ACTION_UNSTASH
        ) {
            PreferenceManager.setStickToEdgeEnabled(it, false)
        }
    }

    private fun updateQuickBallSize(value: Float) {
        handleQuickBall(
            action = QuickBallService.ACTION_UPDATE_SIZE
        ) {
            PreferenceManager.setBallSize(it, value)
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
            binding.switchEnableQuickBall.isChecked =
                PreferenceManager.isQuickBallEnabled(requireContext())
            binding.switchEnableQuickBall.jumpDrawablesToCurrentState()
        }

        // Restore lock screen preference
        binding.switchEnableOnLockScreen.isChecked =
            PreferenceManager.isShowOnLockScreenEnabled(requireContext())
        binding.switchEnableOnLockScreen.jumpDrawablesToCurrentState()

        // Restore hide on landscape preference
        binding.switchHideOnLandscape.isChecked =
            PreferenceManager.isHideOnLandscapeEnabled(requireContext())
        binding.switchHideOnLandscape.jumpDrawablesToCurrentState()

        // Restore stick to edge preference
        binding.switchStickToEdge.isChecked =
            PreferenceManager.isStickToEdgeEnabled(requireContext())
        binding.switchStickToEdge.jumpDrawablesToCurrentState()

        // Restore ball size preference
        binding.sliderBallSize.value = PreferenceManager.getBallSize(requireContext())
    }

    private fun hasAllRequiredPermissions(): Boolean {
        return isAccessibilityServiceEnabled() && canModifySystemSettings()
    }

    // Permission Checks
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager =
            getSystemService(requireContext(), AccessibilityManager::class.java)
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
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Settings.System.canWrite(
                    requireContext()
                )

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
                val key = ComponentName(
                    requireContext().packageName,
                    QuickBallService::class.java.name
                ).flattenToString()
                putExtra(":settings:fragment_args_key", key)
                putExtra(
                    ":settings:show_fragment_args",
                    bundleOf(":settings:fragment_args_key" to key)
                )
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