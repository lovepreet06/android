package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_meeting.*
import kotlinx.android.synthetic.main.meeting_ringing_fragment.*
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnSwipeTouchListener
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.databinding.MeetingRingingFragmentBinding
import mega.privacy.android.app.meeting.AnimationTool.clearAnimationAndGone
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.meeting.listeners.AnswerChatCallListener
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor
import mega.privacy.android.app.utils.CallUtil.getDefaultAvatarCall
import mega.privacy.android.app.utils.CallUtil.getImageAvatarCall
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AVATAR_SIZE
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.permission.permissionsBuilder
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatError
import java.util.*


@AndroidEntryPoint
class RingingMeetingFragment : MeetingBaseFragment(),
    AnswerChatCallListener.OnCallAnsweredCallback {

    private val inMeetingViewModel by viewModels<InMeetingViewModel>()

    private lateinit var binding: MeetingRingingFragmentBinding

    private lateinit var toolbarTitle: EmojiTextView
    private lateinit var toolbarSubtitle: TextView

    private var chatId: Long = MEGACHAT_INVALID_HANDLE

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewModel()
        permissionsRequester.launch(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            chatId = it.getLong(MEETING_CHAT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity() as MeetingActivity).let {
            toolbarTitle = it.title_toolbar
            toolbarSubtitle = it.subtitle_toolbar
        }

        binding = MeetingRingingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initComponent()
    }

    /**
     * Initialize components of UI
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initComponent() {

        // Always be 'calling'.
        toolbarSubtitle.text = StringResourcesUtils.getString(R.string.outgoing_call_starting)

        binding.answerVideoFab.startAnimation(
            AnimationUtils.loadAnimation(
                meetingActivity,
                R.anim.shake
            )
        )
        binding.answerAudioFab.setOnClickListener {
            inMeetingViewModel.checkAnotherCallsInProgress(inMeetingViewModel.currentChatId)

            answerCall(enableVideo = false)
        }

        binding.answerVideoFab.setOnTouchListener(object : OnSwipeTouchListener(meetingActivity) {

            override fun onSwipeTop() {
                binding.answerVideoFab.clearAnimation()
                animationButtons()
            }
        })

        binding.rejectFab.setOnClickListener {
            inMeetingViewModel.removeIncomingCallNotification(chatId)

            if (inMeetingViewModel.isOneToOneCall()) {
                inMeetingViewModel.leaveMeeting()
            } else {
                inMeetingViewModel.ignoreCall()
            }
            requireActivity().finish()
        }

        animationAlphaArrows(binding.fourthArrowCall)
        runDelay(ALPHA_ANIMATION_DELAY) {
            animationAlphaArrows(binding.thirdArrowCall)
            runDelay(ALPHA_ANIMATION_DELAY) {
                animationAlphaArrows(binding.secondArrowCall)
                runDelay(ALPHA_ANIMATION_DELAY) {
                    animationAlphaArrows(binding.firstArrowCall)
                }
            }
        }
    }

    /**
     * Let the button execute fade out animation.
     * Go up then disappear.
     */
    private fun animationButtons() {
        val translateAnim = TranslateAnimation(0f, 0f, 0f, DELTA_Y).apply {
            duration = TRANSLATE_DURATION
            fillAfter = true
            fillBefore = true
            repeatCount = 0
            setAnimationListener(object : Animation.AnimationListener {

                override fun onAnimationStart(animation: Animation) {
                    binding.rejectFab.isEnabled = false
                }

                override fun onAnimationRepeat(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    binding.videoLabel.isVisible = false
                    binding.answerVideoFab.hide()
                    inMeetingViewModel.checkAnotherCallsInProgress(inMeetingViewModel.currentChatId)

                    answerCall(enableVideo = true)
                }
            })
        }

        val alphaAnim = AlphaAnimation(1.0f, 0.0f).apply {
            duration = DISAPPEAR_DURATION
            fillAfter = true
            fillBefore = true
            repeatCount = 0
        }

        //false means don't share interpolator
        val s = AnimationSet(false)
        s.addAnimation(translateAnim)
        s.addAnimation(alphaAnim)

        binding.answerVideoFab.startAnimation(s)

        binding.firstArrowCall.clearAnimationAndGone()
        binding.secondArrowCall.clearAnimationAndGone()
        binding.thirdArrowCall.clearAnimationAndGone()
        binding.fourthArrowCall.clearAnimationAndGone()
    }

    /**
     * Method to answer the call with audio enabled
     *
     * @param enableVideo True, if it should be answered with video on. False, if it should be answered with video off
     */
    private fun answerCall(enableVideo: Boolean) {
        inMeetingViewModel.getCall()?.let {
            inMeetingViewModel.answerChatCall(
                enableVideo,
                true,
                AnswerChatCallListener(requireContext(), this)
            )
        }
    }

    /**
     * Let the arrow icon execute alpha animation. Disappear then appear.
     *
     * @param arrow The arrow image view that will execute the animation.
     */
    private fun animationAlphaArrows(arrow: ImageView) {
        logDebug("animationAlphaArrows")
        val alphaAnimArrows = AlphaAnimation(1.0f, 0.0f).apply {
            duration = ALPHA_ANIMATION_DURATION
            fillAfter = true
            fillBefore = true
            repeatCount = Animation.INFINITE
        }

        arrow.startAnimation(alphaAnimArrows)
    }

    /**
     * Initialize ViewModel
     */
    private fun initViewModel() {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            sharedModel.updateChatRoomId(chatId)
            inMeetingViewModel.setChatId(chatId)
        }

        inMeetingViewModel.chatTitle.observe(viewLifecycleOwner) { title ->
            toolbarTitle.text = title
        }

        var bitmap: Bitmap?

        // Set caller's name and avatar
        inMeetingViewModel.getChat()?.let {
            if (inMeetingViewModel.isOneToOneCall()) {
                val callerId = it.getPeerHandle(0)

                bitmap = getImageAvatarCall(it, callerId)
                if (bitmap == null) {
                    bitmap = getDefaultAvatarCall(context, callerId)
                }
            } else {
                bitmap = getDefaultAvatar(
                    getSpecificAvatarColor(Constants.AVATAR_GROUP_CHAT_COLOR),
                    getTitleChat(it),
                    AVATAR_SIZE,
                    true,
                    true
                )
            }

            avatar.setImageBitmap(bitmap)
        }

        LiveEventBus.get(EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT, Long::class.java)
            .observe(this) {
                if (chatId == it) {
                    requireActivity().finish()
                }
            }

        // Caller cancelled the call.
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeSticky(this) {
                if (it.status == MegaChatCall.CALL_STATUS_DESTROYED) {
                    requireActivity().finish()
                }
            }

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.CAMERA).toCollection(
                        ArrayList()
                    )
                )
                    .setOnRequiresPermission { l -> onRequiresCameraPermission(l) }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onCameraNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }

        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.RECORD_AUDIO).toCollection(
                        ArrayList()
                    )
                )
                    .setOnRequiresPermission { l -> onRequiresAudioPermission(l) }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onAudioNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionsRequester = permissionsBuilder(permissions.toCollection(ArrayList()))
            .setOnPermissionDenied { l -> onPermissionDenied(l) }
            .setOnRequiresPermission { l -> onRequiresPermission(l) }
            .setOnShowRationale { l -> onShowRationale(l) }
            .setOnNeverAskAgain { l -> onNeverAskAgain(l) }
            .setPermissionEducation { showPermissionsEducation() }
            .build()
    }

    private fun showSnackBar(message: String) =
        (activity as BaseActivity).showSnackbar(binding.root, message)

    private fun onAudioNeverAskAgain(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.RECORD_AUDIO)) {
            logDebug("user denies the RECORD_AUDIO permissions")
            showSnackBar(StringResourcesUtils.getString(R.string.meeting_required_permissions_warning))
        }
    }

    private fun onCameraNeverAskAgain(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.CAMERA)) {
            logDebug("user denies the CAMERA permissions")
            showSnackBar(StringResourcesUtils.getString(R.string.meeting_required_permissions_warning))
        }
    }

    override fun onDestroy() {
        RunOnUIThreadUtils.stop()
        super.onDestroy()
    }

    companion object {
        private const val ALPHA_ANIMATION_DURATION = 1000L
        private const val ALPHA_ANIMATION_DELAY = 250L
        private const val DELTA_Y = -380f
        private const val TRANSLATE_DURATION = 500L
        private const val DISAPPEAR_DURATION = 600L
    }

    override fun onCallAnswered(chatId: Long, flag: Boolean) {
        val actionString = if (flag) {
            logDebug("Call answered with video ON and audio ON")
            MEETING_ACTION_RINGING_VIDEO_ON
        } else {
            logDebug("Call answered with video OFF and audio ON")
            MEETING_ACTION_RINGING_VIDEO_OFF
        }

        val action = RingingMeetingFragmentDirections.actionGlobalInMeeting(
            actionString,
            chatId
        )
        findNavController().navigate(action)
    }

    override fun onErrorAnsweredCall(errorCode: Int) {
        logDebug("Error answering the call")
        inMeetingViewModel.removeIncomingCallNotification(chatId)
        binding.answerVideoFab.clearAnimation()
        binding.firstArrowCall.clearAnimationAndGone()
        binding.secondArrowCall.clearAnimationAndGone()
        binding.thirdArrowCall.clearAnimationAndGone()
        binding.fourthArrowCall.clearAnimationAndGone()
        requireActivity().finish()
    }
}