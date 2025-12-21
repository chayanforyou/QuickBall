package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.chayanforyou.quickball.databinding.FragmentLanguageSelectionBinding
import io.github.chayanforyou.quickball.ui.adapters.LanguageAdapter
import io.github.chayanforyou.quickball.utils.LanguageUtils

class LanguageSelectionSheet : BottomSheetDialogFragment() {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            val sheet = LanguageSelectionSheet()
            sheet.show(fragmentManager, "LanguageSelectionSheet")
        }
    }

    private var _binding: FragmentLanguageSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var languageAdapter: LanguageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val currentLanguage = LanguageUtils.getCurrentLanguage(requireContext())
        val languages = LanguageUtils.getAllLanguages()

        languageAdapter = LanguageAdapter(
            languages = languages,
            currentLanguage = currentLanguage,
            onLanguageSelected = { selectedLanguage ->
                LanguageUtils.setLanguage(requireContext(), selectedLanguage)
                dismiss()
            }
        )

        binding.recyclerViewLanguages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = languageAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}