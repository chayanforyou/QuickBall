package io.github.chayanforyou.quickball.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.chayanforyou.quickball.databinding.ItemInstalledAppBinding
import io.github.chayanforyou.quickball.domain.models.InstalledApp

class SelectAppListAdapter(
    private val apps: List<InstalledApp>,
    private val onAppSelect: (InstalledApp) -> Unit
) : RecyclerView.Adapter<SelectAppListAdapter.InstalledAppListViewHolder>() {

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

        fun bind(app: InstalledApp) = with(binding) {
            ivAppIcon.setImageDrawable(app.icon)
            tvAppName.text = app.appName
            switchSelect.visibility = View.GONE

            root.setOnClickListener {
                onAppSelect(app)
            }
        }
    }
}
