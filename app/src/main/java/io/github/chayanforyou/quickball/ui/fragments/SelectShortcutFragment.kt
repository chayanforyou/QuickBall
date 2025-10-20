package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.chayanforyou.quickball.databinding.FragmentSelectShortcutBinding
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.ui.adapters.MenuItemAdapter
import io.github.chayanforyou.quickball.ui.helpers.MenuItemTouchHelper

class SelectShortcutFragment : Fragment() {

    private var _binding: FragmentSelectShortcutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectShortcutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}