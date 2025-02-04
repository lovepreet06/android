package mega.privacy.android.app.meeting.adapter

import android.graphics.PorterDuff
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemMeetingParticipantBinding

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class ParticipantViewHolder(
    private val binding: ItemMeetingParticipantBinding,
    private val onParticipantOption: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.threeDots.setOnClickListener {
            onParticipantOption(adapterPosition)
        }
    }

    fun bind(participant: Participant) {
        binding.avatar.setImageBitmap(participant.avatar)
        binding.name.text = participant.name

        if (participant.isModerator) {
            val drawable = ContextCompat.getDrawable(binding.name.context, R.drawable.ic_moderator)
            drawable?.setTint(ContextCompat.getColor(binding.name.context, R.color.teal_300_teal_200))

            binding.name.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                drawable,
                null
            )
        } else {
            binding.name.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        binding.name.text = participant.getDisplayName(binding.name.context)

        if (participant.isAudioOn) {
            binding.audioStatus.setImageResource(R.drawable.ic_mic_on)
            binding.audioStatus.setColorFilter(
                ContextCompat.getColor(binding.audioStatus.context, R.color.grey_054_white_054),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.audioStatus.setImageResource(R.drawable.ic_mic_off_grey_red)
            binding.audioStatus.colorFilter = null
        }

        if (participant.isVideoOn) {
            binding.videoStatus.setImageResource(R.drawable.ic_video)
            binding.videoStatus.setColorFilter(
                ContextCompat.getColor(binding.videoStatus.context, R.color.grey_054_white_054),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.videoStatus.setImageResource(R.drawable.ic_video_off_grey_red)
            binding.videoStatus.colorFilter = null
        }
    }
}
