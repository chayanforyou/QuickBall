package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.chayanforyou.quickball.databinding.FragmentSelectShortcutBinding
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.models.MenuItemModel
import io.github.chayanforyou.quickball.ui.adapters.SelectShortcutAdapter
import io.github.chayanforyou.quickball.ui.viewmodels.MenuSelectionViewModel

class SelectShortcutFragment : Fragment() {

    private var _binding: FragmentSelectShortcutBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var allItemAdapter: SelectShortcutAdapter

    private val viewModel: MenuSelectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectShortcutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val allMenuItems = MenuItemModel.getAllMenuItems()
        val selectedActions = PreferenceManager.getSelectedMenuItems(requireContext())
            .mapTo(HashSet()) { it.action }

        val menuItems = allMenuItems.map { menuItem ->
            menuItem.copy(isSelected = menuItem.action in selectedActions)
        }
        
        allItemAdapter = SelectShortcutAdapter(
            menuItems = menuItems,
            onItemClick = { selectedMenuItem ->
                handleMenuItemSelection(selectedMenuItem)
            }
        )

        binding.rvAllMenuItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = allItemAdapter
        }
    }

    private fun handleMenuItemSelection(selectedMenuItem: MenuItemModel) {
        viewModel.setSelectedMenuItem(selectedMenuItem)
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}