package support.bymason.kiosk.checkin.feature.report

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collect
import support.bymason.kiosk.checkin.R
import support.bymason.kiosk.checkin.core.ui.FragmentBase
import support.bymason.kiosk.checkin.core.ui.LifecycleAwareLazy
import support.bymason.kiosk.checkin.databinding.ReportFragmentBinding

class ReportFragment : FragmentBase(R.layout.report_fragment) {
    private val vm by viewModels<ReportViewModel> {
        ReportViewModel.Factory()
    }
    private val binding by LifecycleAwareLazy { ReportFragmentBinding.bind(requireView()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            vm.onContinue()
        }

        lifecycleScope.launchWhenStarted {
            vm.actions.collect { onActionRequested(it) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.next.setOnClickListener { vm.onContinue() }
    }

    private fun onActionRequested(action: ReportViewModel.Action) {
        when (action) {
            is ReportViewModel.Action.Navigate -> findNavController().navigate(action.directions)
        }
    }
}
