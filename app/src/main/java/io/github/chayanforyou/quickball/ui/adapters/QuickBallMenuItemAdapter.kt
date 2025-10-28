package io.github.chayanforyou.quickball.ui.adapters

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import io.github.chayanforyou.quickball.databinding.ItemQuickballMenuShortcutBinding
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel
import io.github.chayanforyou.quickball.utils.getAppIcon
import java.util.Collections

class QuickBallMenuItemAdapter(
    private var menuItems: List<QuickBallMenuItemModel>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onItemClick: (Int) -> Unit,
) : RecyclerView.Adapter<QuickBallMenuItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemQuickballMenuShortcutBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    override fun getItemCount(): Int = menuItems.size

    fun getCurrentItems(): MutableList<QuickBallMenuItemModel> = menuItems.toMutableList()

    @SuppressLint("NotifyDataSetChanged")
    fun updateMenuItems(newMenuItems: List<QuickBallMenuItemModel>) {
        menuItems = newMenuItems
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        Collections.swap(menuItems, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    inner class ViewHolder(private val binding: ItemQuickballMenuShortcutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(item: QuickBallMenuItemModel) = with(binding) {
            when {
                item.packageName != null -> {
                    val appIcon = root.context.getAppIcon(item.packageName)
                    ivMenuIcon.setImageDrawable(appIcon)
                }
                else -> {
                    val tintColor = MaterialColors.getColor(ivMenuIcon, R.attr.colorOnSurface)
                    ivMenuIcon.imageTintList = ColorStateList.valueOf(tintColor)
                    ivMenuIcon.setImageResource(item.iconRes)
                }
            }
            tvMenuTitle.text = item.title

            ivDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this@ViewHolder)
                }
                false
            }

            root.setOnClickListener {
                onItemClick(bindingAdapterPosition)
            }
        }
    }
}

