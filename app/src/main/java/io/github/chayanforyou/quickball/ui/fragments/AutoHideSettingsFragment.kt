package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.chayanforyou.quickball.R
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

    private enum class SelectionState {
        NONE_SELECTED,
        ALL_SELECTED,
    }

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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apps = withContext(Dispatchers.IO) {
                    requireContext().loadInstalledApps(sortBySelectedFirst = true)
                }

                setupRecyclerView(apps)
                hideLoadingState()
                setupMenu()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRecyclerView(apps: List<InstalledAppModel>) {
        appListAdapter = InstalledAppListAdapter(
            apps = apps,
            onToggleChanged = { app, isSelected ->
                updateAppSelectionState(app, isSelected)
                requireActivity().invalidateOptionsMenu()
            },
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

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.auto_hide, menu)
                updateMenuIcon(menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_toggle_selection -> {
                        toggleSelection()
                        requireActivity().invalidateOptionsMenu()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun toggleSelection() {
        val selectionState = getSelectionState()
        when (selectionState) {
            SelectionState.ALL_SELECTED -> appListAdapter.deselectAll()
            SelectionState.NONE_SELECTED -> appListAdapter.selectAll()
        }
    }

    private fun updateMenuIcon(menu: Menu) {
        val toggleItem = menu.findItem(R.id.action_toggle_selection)
        if (toggleItem != null) {
            val selectionState = getSelectionState()
            when (selectionState) {
                SelectionState.ALL_SELECTED -> {
                    toggleItem.setIcon(R.drawable.ic_deselect_all)
                    toggleItem.setTitle(R.string.deselect_all)
                }

                SelectionState.NONE_SELECTED -> {
                    toggleItem.setIcon(R.drawable.ic_select_all)
                    toggleItem.setTitle(R.string.select_all)
                }
            }
        }
    }

    private fun getSelectionState(): SelectionState {
        val selectedCount = appListAdapter.getSelectedCount()
        val totalCount = appListAdapter.getItemCount()

        return when (selectedCount) {
            totalCount -> SelectionState.ALL_SELECTED
            else -> SelectionState.NONE_SELECTED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}