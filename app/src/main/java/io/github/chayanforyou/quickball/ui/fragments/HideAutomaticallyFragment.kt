package io.github.chayanforyou.quickball.ui.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.chayanforyou.quickball.databinding.FragmentHideAutomaticallyBinding
import io.github.chayanforyou.quickball.domain.models.AppModel
import io.github.chayanforyou.quickball.ui.adapters.AppListAdapter
import io.github.chayanforyou.quickball.domain.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HideAutomaticallyFragment : Fragment() {

    private var _binding: FragmentHideAutomaticallyBinding? = null
    private val binding get() = _binding!!

    private lateinit var appListAdapter: AppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHideAutomaticallyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoadingState()
        loadAppsAsync()
    }

    private fun showLoadingState() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.rvApps.visibility = View.GONE
    }

    private fun hideLoadingState() {
        binding.progressLoading.visibility = View.GONE
        binding.rvApps.visibility = View.VISIBLE
    }

    private fun loadAppsAsync() {
        lifecycleScope.launch {
            try {
                val apps = withContext(Dispatchers.IO) {
                    loadInstalledApps()
                }

                setupRecyclerView(apps)
                hideLoadingState()
            } catch (_: Exception) {
                hideLoadingState()
            }
        }
    }

    private fun setupRecyclerView(apps: List<AppModel>) {
        appListAdapter = AppListAdapter(
            apps = apps,
            onToggleChanged = { app, isSelected ->
                updateAppSelectionState(app, isSelected)
            }
        )

        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appListAdapter
        }
    }

    private fun loadInstalledApps(): List<AppModel> {
        val packageManager = requireContext().packageManager
        val currentPackage = requireContext().packageName
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val selectedApps = PreferenceManager.getSelectedApps(requireContext())

        return installedApps
            .filter { appInfo ->
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null &&
                        appInfo.packageName != currentPackage
            }
            .mapNotNull { appInfo ->
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                if (appName.isBlank() || appName.equals(appInfo.packageName, ignoreCase = true)) return@mapNotNull null
                val icon = packageManager.getApplicationIcon(appInfo)
                AppModel(
                    appName = appName,
                    packageName = appInfo.packageName,
                    icon = icon,
                    isSelected = selectedApps.contains(appInfo.packageName)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    private fun updateAppSelectionState(app: AppModel, isSelected: Boolean) {
        if (isSelected) {
            PreferenceManager.addSelectedApp(requireContext(), app.packageName)
        } else {
            PreferenceManager.removeSelectedApp(requireContext(), app.packageName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}