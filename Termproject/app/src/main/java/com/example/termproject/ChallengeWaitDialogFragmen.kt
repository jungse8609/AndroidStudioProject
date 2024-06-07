package com.example.termproject

import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.termproject.databinding.DialogChallengeWaitBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// @return : 에러 : -1, 상대 거절 : 0, 상대 수락 : 1, 본인 취소/시간초과 : 2
class ChallengeWaitDialogFragment(private val waitTime: Long, private val roomName: String, private val opponentId: String, private val opponentAccept: String, private val callback: (Int) -> Unit) : DialogFragment() {
    private lateinit var binding: DialogChallengeWaitBinding
    private var waitTimer: CountDownTimer? = null
    private var listenerWaiteOpponent: ListenerRegistration? = null
    private var listenerRegistration: ListenerRegistration? = null
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogChallengeWaitBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(binding.root)
            .setCancelable(false)

        val dialog = builder.create()

        // Start the countdown timer
        waitTimer = object : CountDownTimer(waitTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.txtWaitTime.text = "Waiting for opponent... (${seconds}s)"
            }

            override fun onFinish() {
                // 시간 초과
                listenerWaiteOpponent?.remove()
                listenerRegistration?.remove()
                callback.invoke(3) // Time expired, return false
                dismiss() // Close the dialog
            }
        }.start()

        // Listen for opponent's response
        listenerWaiteOpponent = db.collection("BattleRooms").document(roomName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    listenerWaiteOpponent?.remove()
                    listenerRegistration?.remove()
                    callback.invoke(-1) // Error occurred, return false
                    dismiss() // Close the dialog
                    return@addSnapshotListener
                }

                // 상대방 거절
                listenerRegistration = db.collection("BattleWait").document(opponentId)
                    .addSnapshotListener { snapshot2, e2 ->
                        if (e2 != null || snapshot2 == null || !snapshot2.exists()) {
                            waitTimer?.cancel() // Stop the timer
                            listenerRegistration?.remove()
                            listenerWaiteOpponent?.remove()
                            callback.invoke(0) // User cancelled, return false
                            dialog.dismiss() // Close the dialog
                        }
                    }

                // 상대방 수락
                if (snapshot != null && snapshot.exists()) {
                    val opponentChk = snapshot.getLong(opponentAccept) ?: 0L
                    if (opponentChk == 1L) {
                        waitTimer?.cancel() // Stop the timer
                        listenerRegistration?.remove()
                        listenerWaiteOpponent?.remove()
                        callback.invoke(1) // Opponent accepted, return true
                        dismiss() // Close the dialog
                    }
                }
            }


        // 내가 취소버튼 누름
        binding.btnCancel.setOnClickListener {
            listenerRegistration?.remove()
            listenerWaiteOpponent?.remove()
            db.collection("BattleRooms").document(roomName).delete()
                .addOnSuccessListener {
                    db.collection("BattleWait").document(opponentId).delete()
                        .addOnSuccessListener {
                            waitTimer?.cancel() // Stop the timer
                            callback.invoke(2) // User cancelled, return false
                            dialog.dismiss() // Close the dialog
                        }
                        .addOnFailureListener { e ->
                            Log.e("LogTemp", e.toString())
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("LogTemp", e.toString())
                }
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        waitTimer?.cancel() // Stop the timer when the dialog is destroyed
    }
}
