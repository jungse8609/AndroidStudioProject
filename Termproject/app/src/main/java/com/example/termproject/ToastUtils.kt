package com.example.termproject

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast

object ToastUtils {
    fun createToast(context: Context, message: String) {
        val layoutInflater = LayoutInflater.from(context)
        val layout = layoutInflater.inflate(R.layout.custom_toast_layout, null)
        val toastText: TextView = layout.findViewById(R.id.customToastText)
        toastText.text = message

        val toast = Toast(context.applicationContext)
        toast.setGravity(Gravity.BOTTOM, 0, 100)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }
}
