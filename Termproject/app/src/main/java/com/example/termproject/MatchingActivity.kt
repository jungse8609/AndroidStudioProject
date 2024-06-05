package com.example.termproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class MatchingActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var userId : String

    data class User(
        val nick: String = "",
        val score: Long = 0,
        val status: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching)

        userId = intent.getStringExtra("userId").toString()
        //Toast.makeText(this, "$userId", Toast.LENGTH_SHORT).show()
        db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    val userList = mutableListOf<User>()
                    for (document in documents) {
                        val dbUser = document.data
                        val nick = dbUser["NickName"] as String
                        val score = dbUser["Score"] as Long
                        val status = dbUser["Status"] as Long
                        lateinit var user: User
                        if (status == 1.toLong()){
                            user = User(nick, score, "online")
                        }
                        else{
                            user = User(nick, score, "offline")
                        }

                        userList.add(user)
                    }
                    // score에 따라 내림차순 정렬
                    userList.sortByDescending { it.score }

                    // 확인용 Toast 메시지
                    userList.forEach { user ->
                        Toast.makeText(this, "${user}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "DB 연결 실패", Toast.LENGTH_SHORT).show()
            }
    }
}