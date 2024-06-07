package com.example.termproject

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.termproject.databinding.DialogAcceptDeclineBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AcceptDeclineDialogFragment(
    private val userId: String,
    private val opponentId: String,
    private val onResult: (Int) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogAcceptDeclineBinding
    private var countDownTimer: CountDownTimer? = null
    private var listenerRegistration: ListenerRegistration? = null
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

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
            listenerRegistration?.remove()
            onResult(1)
            dismiss()
        }

        binding.buttonDecline.setOnClickListener {
            countDownTimer?.cancel()
            listenerRegistration?.remove()
            onResult(0)
            dismiss()
        }

        // Start 10-second timer
        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.textViewTimer.text = "Time remaining: ${millisUntilFinished / 1000} seconds"
            }

            override fun onFinish() {
                listenerRegistration?.remove()
                onResult(2) // Time expired, treat as declined
                dismiss()
            }
        }.start()

        // Add snapshot listener to detect deletion of opponentId document
        listenerRegistration = db.collection("BattleWait").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    listenerRegistration?.remove()
                    countDownTimer?.cancel()
                    onResult(3) // Document no longer exists
                    dismiss()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        listenerRegistration?.remove()
    }
}
