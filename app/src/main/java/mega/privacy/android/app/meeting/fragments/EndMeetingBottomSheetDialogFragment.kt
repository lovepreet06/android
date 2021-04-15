package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetEndMeetingBinding
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.LogUtil

class EndMeetingBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetEndMeetingBinding
    private val viewModel: EndMeetingBottomSheetDialogViewModel by viewModels()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        viewModel.clickEvent.observe(this, {
            when (it) {
                LEAVE_ANYWAY -> leaveAnyway()
                END_MEETING_FOR_ALL -> askConfirmationEndMeetingForAll()
                ASSIGN_MODERATOR -> assignModerator()

            }
        })

        binding =
            BottomSheetEndMeetingBinding.inflate(LayoutInflater.from(context), null, false).apply {
                lifecycleOwner = this@EndMeetingBottomSheetDialogFragment
                viewmodel = viewModel
            }

        dialog.setContentView(binding.root)
    }

    private fun assignModerator() {
        Toast.makeText(requireContext(), "Assign Moderator", Toast.LENGTH_SHORT).show()
    }

    private fun leaveAnyway() {
        Toast.makeText(requireContext(), "Leave anyway", Toast.LENGTH_SHORT).show()
    }

    private fun askConfirmationEndMeetingForAll() {
        LogUtil.logDebug("askConfirmationEndMeeting")
        val dialogClickListener =
            DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        //End Meeting for all
                        dialog.dismiss()
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                        dialog.dismiss()
                    }
                }
            }
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.end_meeting_for_all_dialog_title))
            setMessage(getString(R.string.end_meeting_dialog_message))
            setPositiveButton(R.string.general_ok, dialogClickListener)
            setNegativeButton(R.string.general_cancel, dialogClickListener)
            show()
        }
    }


    companion object {
        fun newInstance(): EndMeetingBottomSheetDialogFragment {
            val fragment = EndMeetingBottomSheetDialogFragment()
            return fragment
        }
    }
}