package io.github.chayanforyou.quickball.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.chayanforyou.quickball.databinding.ItemSelectShortcutBinding
import io.github.chayanforyou.quickball.domain.models.MenuItemModel

class SelectShortcutAdapter(
    private var menuItems: List<MenuItemModel>,
    private val onItemClick: (MenuItemModel) -> Unit
) : RecyclerView.Adapter<SelectShortcutAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSelectShortcutBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size

    inner class ViewHolder(private val binding: ItemSelectShortcutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuItemModel) = with(binding) {
            tvMenuTitle.text = item.title

            root.isEnabled = !item.isSelected
            root.alpha = if (item.isSelected) 0.5f else 1f

            root.setOnClickListener {
                if (!item.isSelected) onItemClick(item)
            }
        }
    }
}
