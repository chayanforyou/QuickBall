package io.github.chayanforyou.quickball.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.chayanforyou.quickball.databinding.ItemShortcutSelectionBinding
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem

class ShortcutSelectionAdapter(
    private var menuItems: List<QuickBallMenuItem>,
    private val onItemClick: (QuickBallMenuItem) -> Unit
) : RecyclerView.Adapter<ShortcutSelectionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemShortcutSelectionBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size

    inner class ViewHolder(private val binding: ItemShortcutSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuickBallMenuItem) = with(binding) {
            tvMenuTitle.text = item.getTitle(root.context)
            root.isEnabled = !item.isSelected
            root.alpha = if (item.isSelected) 0.5f else 1f

            root.setOnClickListener {
                if (!item.isSelected) onItemClick(item)
            }
        }
    }
}
