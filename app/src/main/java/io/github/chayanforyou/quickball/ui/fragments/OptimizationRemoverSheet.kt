package io.github.chayanforyou.quickball.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.elevation.SurfaceColors
import io.github.chayanforyou.quickball.databinding.SheetDokiTutorialBinding

class OptimizationRemoverSheet : BottomSheetDialogFragment() {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            OptimizationRemoverSheet().show(fragmentManager, "optimization_remover_sheet")
        }
    }

    private var _binding: SheetDokiTutorialBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = SheetDokiTutorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheetDialog = dialog as BottomSheetDialog
        val behavior = bottomSheetDialog.behavior
        
        // Set initial peek height to 85% of screen height
        behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.85).toInt()

        binding.dokiView.apply {
            setButtonsVisibility(false)

            val appbar = findViewById<AppBarLayout>(dev.doubledot.doki.R.id.appbar)

            (appbar.getChildAt(0) as CollapsingToolbarLayout).setContentScrimColor(
                ContextCompat.getColor(context, android.R.color.transparent)
            )

            headerBackgroundColor = SurfaceColors.SURFACE_1.getColor(context)

            findViewById<View>(dev.doubledot.doki.R.id.divider3).visibility = View.GONE

            loadContent(appName = "Quick Ball")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
