package io.github.chayanforyou.quickball.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.chayanforyou.quickball.databinding.ItemLanguageBinding
import io.github.chayanforyou.quickball.utils.LanguageUtils

class LanguageAdapter(
    private val languages: List<LanguageUtils.Language>,
    private val currentLanguage: LanguageUtils.Language,
    private val onLanguageSelected: (LanguageUtils.Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount(): Int = languages.size

    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(language: LanguageUtils.Language) {
            binding.textLanguageName.text = language.displayName
            binding.radioLanguage.isChecked = language == currentLanguage

            binding.root.setOnClickListener {
                onLanguageSelected(language)
            }

            binding.radioLanguage.setOnClickListener {
                onLanguageSelected(language)
            }
        }
    }
}