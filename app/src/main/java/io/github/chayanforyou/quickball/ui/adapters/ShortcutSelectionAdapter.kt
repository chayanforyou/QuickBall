package io.github.chayanforyou.quickball.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.chayanforyou.quickball.databinding.ItemShortcutSelectionBinding
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel

class ShortcutSelectionAdapter(
    private var menuItems: List<QuickBallMenuItemModel>,
    private val onItemClick: (QuickBallMenuItemModel) -> Unit
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

        fun bind(item: QuickBallMenuItemModel) = with(binding) {
            tvMenuTitle.text = item.title
            root.isEnabled = !item.isSelected
            root.alpha = if (item.isSelected) 0.5f else 1f

            root.setOnClickListener {
                if (!item.isSelected) onItemClick(item)
            }
        }
    }
}
