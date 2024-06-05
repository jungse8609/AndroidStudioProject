package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.example.termproject.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {
    // XML Variables
    lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init XML
        val btnStart = binding.BtnStart
        val userId = intent.getStringExtra("userId").toString()
        val userNick = intent.getStringExtra("userNick").toString()
        val userScore = intent.getLongExtra("userScore", -1)
        val myId = findViewById<TextView>(R.id.myID)
        val myNick = findViewById<TextView>(R.id.myNick)
        val myScore = findViewById<TextView>(R.id.myScore)

        myId.setText(userId)
        myNick.setText(userNick)
        myScore.setText(userScore.toString())

        toggle = ActionBarDrawerToggle(this, binding.drawer, R.string.drawer_opened, R.string.drawer_closed)
        binding.drawer.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle.syncState()

        btnStart.setOnClickListener {
            val intent = Intent(this, MatchingActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
