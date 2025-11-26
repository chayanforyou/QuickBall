package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.chayanforyou.quickball.databinding.FragmentAutoHideSettingsBinding
import io.github.chayanforyou.quickball.domain.models.InstalledAppModel
import io.github.chayanforyou.quickball.ui.adapters.InstalledAppListAdapter
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.utils.loadInstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoHideSettingsFragment : Fragment() {

    private var _binding: FragmentAutoHideSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var appListAdapter: InstalledAppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAutoHideSettingsBinding.inflate(inflater, container, false)
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
                    requireContext().loadInstalledApps(sortBySelectedFirst = true)
                }

                setupRecyclerView(apps)
                hideLoadingState()
            } catch (_: Exception) {
                hideLoadingState()
            }
        }
    }

    private fun setupRecyclerView(apps: List<InstalledAppModel>) {
        appListAdapter = InstalledAppListAdapter(
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


    private fun updateAppSelectionState(app: InstalledAppModel, isSelected: Boolean) {
        if (isSelected) {
            PreferenceManager.addAutoHideApp(requireContext(), app.packageName)
        } else {
            PreferenceManager.removeAutoHideApp(requireContext(), app.packageName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}