package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.databinding.FragmentSelectShortcutBinding
import io.github.chayanforyou.quickball.databinding.FragmentShortcutBinding
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.ui.adapters.MenuItemAdapter
import io.github.chayanforyou.quickball.ui.helpers.MenuItemTouchHelper

class ShortcutFragment : Fragment() {

    private var _binding: FragmentShortcutBinding? = null
    private val binding get() = _binding!!

    private lateinit var menuItemAdapter: MenuItemAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

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
    }

    private fun setupRecyclerView() {
        val menuItems = PreferenceManager.getSelectedMenuItems(requireContext())

        menuItemAdapter = MenuItemAdapter(
            menuItems = menuItems,
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}