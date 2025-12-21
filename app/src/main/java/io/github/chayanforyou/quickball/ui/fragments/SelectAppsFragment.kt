package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.chayanforyou.quickball.databinding.FragmentSelectAppsBinding
import io.github.chayanforyou.quickball.domain.models.InstalledAppModel
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel
import io.github.chayanforyou.quickball.ui.adapters.SelectAppListAdapter
import io.github.chayanforyou.quickball.ui.viewmodels.MenuSelectionViewModel
import io.github.chayanforyou.quickball.utils.loadInstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectAppsFragment : Fragment() {

    private var _binding: FragmentSelectAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var appListAdapter: SelectAppListAdapter
    private val viewModel: MenuSelectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectAppsBinding.inflate(inflater, container, false)
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apps = withContext(Dispatchers.IO) {
                    requireContext().loadInstalledApps()
                }

                setupRecyclerView(apps)
                hideLoadingState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRecyclerView(apps: List<InstalledAppModel>) {
        appListAdapter = SelectAppListAdapter(
            apps = apps,
            onAppSelect = { app ->
                handleAppSelection(app)
            }
        )

        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appListAdapter
        }
    }

    private fun handleAppSelection(selectedApp: InstalledAppModel) {
        val appMenuItem = QuickBallMenuItemModel.createAppMenuItem(
            appName = selectedApp.appName,
            packageName = selectedApp.packageName,
            iconRes = android.R.drawable.sym_def_app_icon,
        )
        viewModel.setSelectedMenuItem(appMenuItem)
        // Navigate back to ShortcutMenuFragment
        val action = SelectAppsFragmentDirections.actionSelectAppsFragmentToShortcutMenuFragment()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}