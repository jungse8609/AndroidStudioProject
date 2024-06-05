package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class StartActivity : AppCompatActivity() {
    // XML Variables
    private lateinit var btnStart: Button

    private lateinit var userId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // Init XML
        btnStart = findViewById(R.id.BtnStart)

        btnStart.setOnClickListener {
            userId = intent.getStringExtra("userId").toString()
            //Toast.makeText(this, "$userId", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MatchingActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
    }
}
