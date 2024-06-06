package com.example.termproject

import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.termproject.R
import com.example.termproject.databinding.DialogChallengeWaitBinding

class ChallengeWaitDialogFragment(private val waitTime: Long, private val callback: (Boolean) -> Unit) : DialogFragment() {
    private lateinit var binding: DialogChallengeWaitBinding
    private var waitTimer: CountDownTimer? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogChallengeWaitBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(binding.root)
            .setCancelable(false)

        val dialog = builder.create()

        // 대기 시간 카운트다운 시작
        waitTimer = object : CountDownTimer(waitTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.txtWaitTime.text = "상대방 대기 중... (${seconds}s)"
            }

            override fun onFinish() {
                callback.invoke(false) // 시간이 다 되었을 때 콜백 호출하여 거절 처리
                dismiss() // 대기 시간이 끝나면 다이얼로그 닫기
            }
        }.start()

        binding.btnCancel.setOnClickListener {
            waitTimer?.cancel() // 카운트다운 중지
            callback.invoke(false) // 거절 처리
            dialog.dismiss() // 다이얼로그 닫기
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        waitTimer?.cancel() // 다이얼로그가 닫힐 때 카운트다운 중지
    }
}
