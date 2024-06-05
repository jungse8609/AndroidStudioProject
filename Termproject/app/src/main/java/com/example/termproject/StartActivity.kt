package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.termproject.databinding.ActivityStartBinding
import com.google.firebase.firestore.FirebaseFirestore

class StartActivity : AppCompatActivity() {
    // XML Variables
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var userId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init XML
        val btnStart = binding.BtnStart
        userId = intent.getStringExtra("userId").toString()
        val userNick = intent.getStringExtra("userNick").toString()
        val userScore = intent.getLongExtra("userScore", -1)
        val myId = findViewById<TextView>(R.id.myID)
        val myNick = findViewById<TextView>(R.id.myNick)
        val myScore = findViewById<TextView>(R.id.myScore)

        myId.text = userId
        myNick.text = userNick
        myScore.text = userScore.toString()

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

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("종료하시겠습니까?")
            .setCancelable(false)
            .setPositiveButton("종료") { dialog, id ->
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .whereEqualTo("ID", userId)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            document.reference.update("Status", 0)
                        }
                    }
                super.onBackPressed()
            }
            .setNegativeButton("취소") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }
}
