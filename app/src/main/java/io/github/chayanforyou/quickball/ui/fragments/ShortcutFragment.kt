package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.chayanforyou.quickball.databinding.FragmentShortcutBinding
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.models.MenuItemModel
import io.github.chayanforyou.quickball.ui.adapters.MenuItemAdapter
import io.github.chayanforyou.quickball.ui.helpers.MenuItemTouchHelper
import io.github.chayanforyou.quickball.ui.viewmodels.MenuSelectionViewModel

class ShortcutFragment : Fragment() {

    private var _binding: FragmentShortcutBinding? = null
    private val binding get() = _binding!!

    private lateinit var menuItemAdapter: MenuItemAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val viewModel: MenuSelectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShortcutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupViewModelObservers()
    }
    
    private fun setupViewModelObservers() {
        viewModel.selectedMenuItem.observe(viewLifecycleOwner) { selectedMenuItem ->
            selectedMenuItem?.let { menuItem ->
                updateMenuItem(menuItem)
                viewModel.clearSelectedMenuItem()
            }
        }
    }

    private fun setupRecyclerView() {
        val menuItems = PreferenceManager.getSelectedMenuItems(requireContext())

        menuItemAdapter = MenuItemAdapter(
            menuItems = menuItems,
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            },
            onItemClick = { position ->
                viewModel.setSelectedPosition(position)
                navigateToSelectShortcut()
            }
        )

        binding.rvMenuItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = menuItemAdapter
        }

        val touchHelperCallback = MenuItemTouchHelper(
            adapter = menuItemAdapter,
            onItemMoved = {
                saveMenuItemOrder()
            }
        )
        itemTouchHelper = ItemTouchHelper(touchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvMenuItems)
    }

    private fun saveMenuItemOrder() {
        val reorderedItems = menuItemAdapter.getCurrentItems()
        PreferenceManager.updateMenuItemOrder(requireContext(), reorderedItems)
    }

    private fun navigateToSelectShortcut() {
        val action = ShortcutFragmentDirections.actionShortcutFragmentToSelectShortcutFragment()
        findNavController().navigate(action)
    }
    
    private fun updateMenuItem(newMenuItem: MenuItemModel) {
        val currentPosition = viewModel.selectedPosition.value ?: return
        val currentItems = menuItemAdapter.getCurrentItems()

        if (currentPosition in currentItems.indices) {
            currentItems[currentPosition] = newMenuItem
            PreferenceManager.updateMenuItemOrder(requireContext(), currentItems)
            menuItemAdapter.updateMenuItems(currentItems)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}