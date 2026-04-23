package io.github.chayanforyou.quickball.doki

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.elevation.SurfaceColors
import io.github.chayanforyou.quickball.databinding.DokiViewContentBinding

class DokiContentView : BottomSheetDialogFragment() {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            DokiContentView().show(fragmentManager, "doki_content")
        }
    }

    private var _binding: DokiViewContentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DokiViewContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDeviceInfo()
        applyHeaderBackground()
        setupListeners()
        updateAutoStartVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initDeviceInfo() = with(binding.header) {
        deviceManufacturer.text = Build.MANUFACTURER
        deviceModel.text = Build.MODEL
        deviceAndroidVersion.text = fullAndroidVersion
    }

    private fun applyHeaderBackground() = with(binding.header) {
        headerBackground.setBackgroundColor(
            SurfaceColors.SURFACE_1.getColor(requireContext())
        )
    }

    private fun setupListeners() = with(binding) {
        btnAutoStart.setOnClickListener {
            AutoStartPermissionHelper.getAutoStartPermission(requireContext())
        }

        btnBatteryOptimization.setOnClickListener {
            BatteryPermissionHelper.getPermission(requireContext())
        }
    }

    private fun updateAutoStartVisibility() {
        val isAvailable =
            AutoStartPermissionHelper.isAutoStartPermissionAvailable(requireContext())

        binding.lyAutoStart.visibility =
            if (isAvailable) View.VISIBLE else View.GONE
    }
}