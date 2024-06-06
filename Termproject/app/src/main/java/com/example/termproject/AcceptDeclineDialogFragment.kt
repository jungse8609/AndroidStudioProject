package com.example.termproject

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog

class AcceptDeclineDialogFragment(private val opponentId: String, private val onResult: (Boolean) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage("Do you want to accept the challenge from $opponentId?")
                .setPositiveButton("Accept",
                    DialogInterface.OnClickListener { dialog, id ->
                        onResult(true)
                    })
                .setNegativeButton("Decline",
                    DialogInterface.OnClickListener { dialog, id ->
                        onResult(false)
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
