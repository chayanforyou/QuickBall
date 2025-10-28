package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.chayanforyou.quickball.databinding.FragmentShortcutSelectionBinding
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel
import io.github.chayanforyou.quickball.ui.adapters.ShortcutSelectionAdapter
import io.github.chayanforyou.quickball.ui.viewmodels.MenuSelectionViewModel

class ShortcutSelectionFragment : Fragment() {

    private var _binding: FragmentShortcutSelectionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var allItemAdapter: ShortcutSelectionAdapter

    private val viewModel: MenuSelectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShortcutSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        val allMenuItems = QuickBallMenuItemModel.getAllMenuItems()
        val selectedActions = PreferenceManager.getSelectedMenuItems(requireContext())
            .mapTo(HashSet()) { it.action }

        val menuItems = allMenuItems.map { menuItem ->
            menuItem.copy(isSelected = menuItem.action in selectedActions)
        }
        
        allItemAdapter = ShortcutSelectionAdapter(
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

    private fun setupClickListeners() {
        binding.layoutSelectApp.setOnClickListener {
            val action = ShortcutSelectionFragmentDirections.actionShortcutSelectionFragmentToSelectAppsFragment()
            findNavController().navigate(action)
        }
    }

    private fun handleMenuItemSelection(selectedMenuItem: QuickBallMenuItemModel) {
        viewModel.setSelectedMenuItem(selectedMenuItem)
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}