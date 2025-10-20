package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.databinding.FragmentHideAutomaticallyBinding
import io.github.chayanforyou.quickball.databinding.FragmentSelectShortcutBinding

class HideAutomaticallyFragment : Fragment() {

    private var _binding: FragmentHideAutomaticallyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHideAutomaticallyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}