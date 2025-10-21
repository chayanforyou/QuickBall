package io.github.chayanforyou.quickball.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.chayanforyou.quickball.databinding.ItemAppBinding
import io.github.chayanforyou.quickball.domain.models.AppModel

class AppListAdapter(
    var apps: List<AppModel>,
    private val onToggleChanged: (AppModel, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAppBinding.inflate(inflater, parent, false)
        return AppListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<AppModel>) {
        apps = newApps
        notifyDataSetChanged()
    }

    inner class AppListViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppModel) = with(binding) {
            ivAppIcon.setImageDrawable(app.icon)
            tvAppName.text = app.appName
            
            // Clear listener before setting checked state to avoid triggering callback
            switchSelect.setOnCheckedChangeListener(null)
            switchSelect.isChecked = app.isSelected
            
            // Set listener after setting the checked state
            switchSelect.setOnCheckedChangeListener { _, isChecked ->
                onToggleChanged(app, isChecked)
            }
        }
    }
}
