package io.github.chayanforyou.quickball.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.chayanforyou.quickball.databinding.ItemInstalledAppBinding
import io.github.chayanforyou.quickball.domain.models.InstalledAppModel

class InstalledAppListAdapter(
    var apps: List<InstalledAppModel>,
    private val onToggleChanged: (InstalledAppModel, Boolean) -> Unit
) : RecyclerView.Adapter<InstalledAppListAdapter.InstalledAppListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstalledAppListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemInstalledAppBinding.inflate(inflater, parent, false)
        return InstalledAppListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InstalledAppListViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class InstalledAppListViewHolder(private val binding: ItemInstalledAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: InstalledAppModel) = with(binding) {
            ivAppIcon.setImageDrawable(app.icon)
            tvAppName.text = app.appName

            // Remove listener to avoid triggering on recycler view rebind
            switchSelect.setOnCheckedChangeListener(null)
            switchSelect.isChecked = app.isSelected

            switchSelect.setOnCheckedChangeListener { _, isChecked ->
                app.isSelected = isChecked
                onToggleChanged(app, isChecked)
            }
        }
    }
}
