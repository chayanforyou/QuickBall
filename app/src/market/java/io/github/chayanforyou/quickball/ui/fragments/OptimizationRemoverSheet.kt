package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.elevation.SurfaceColors
import io.github.chayanforyou.quickball.databinding.SheetOptimizationRemoverBinding

class OptimizationRemoverSheet : BottomSheetDialogFragment() {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            OptimizationRemoverSheet().show(fragmentManager, "optimization_remover_sheet")
        }
    }

    private var _binding: SheetOptimizationRemoverBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetOptimizationRemoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dokiView.apply {
            setButtonsVisibility(false)
            headerBackgroundColor = SurfaceColors.SURFACE_1.getColor(context)
            findViewById<View?>(dev.doubledot.doki.R.id.divider3)?.visibility = View.GONE
            loadContent(appName = "Quick Ball")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
