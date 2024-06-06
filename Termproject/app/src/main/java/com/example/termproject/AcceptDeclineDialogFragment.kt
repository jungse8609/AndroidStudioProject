package com.example.termproject

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.termproject.databinding.DialogAcceptDeclineBinding

class AcceptDeclineDialogFragment(
    private val opponentId: String,
    private val onResult: (Boolean) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogAcceptDeclineBinding
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogAcceptDeclineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textViewOpponentId.text = opponentId

        binding.buttonAccept.setOnClickListener {
            countDownTimer?.cancel()
            onResult(true)
            dismiss()
        }

        binding.buttonDecline.setOnClickListener {
            countDownTimer?.cancel()
            onResult(false)
            dismiss()
        }

        // 10초 타이머 시작
        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.textViewTimer.text = "Time remaining: ${millisUntilFinished / 1000} seconds"
            }

            override fun onFinish() {
                onResult(false) // 시간 초과 시 거절 처리
                dismiss()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}
