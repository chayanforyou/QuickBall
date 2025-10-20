package io.github.chayanforyou.quickball.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.chayanforyou.quickball.databinding.ItemMenuShortcutBinding
import io.github.chayanforyou.quickball.domain.models.MenuItemModel
import java.util.Collections

class MenuItemAdapter(
    private var menuItems: List<MenuItemModel>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMenuShortcutBinding.inflate(inflater, parent, false)
        return MenuItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size

    fun getCurrentItems(): List<MenuItemModel> = menuItems.toList()

    fun moveItem(fromPosition: Int, toPosition: Int) {
        Collections.swap(menuItems, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    inner class MenuItemViewHolder(private val binding: ItemMenuShortcutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(item: MenuItemModel) = with(binding) {
            ivMenuIcon.setImageResource(item.iconRes)
            tvMenuTitle.text = item.title

            ivDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this@MenuItemViewHolder)
                }
                false
            }
        }
    }
}

