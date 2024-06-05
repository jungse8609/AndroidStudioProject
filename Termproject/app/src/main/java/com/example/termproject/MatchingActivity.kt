package com.example.termproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.termproject.databinding.MatchingRecyclerViewBinding
import com.google.firebase.firestore.FirebaseFirestore

class MatchingActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var userId : String

    data class User(
        val id: String = "",
        val nick: String = "",
        val score: Long = 0,
        val status: String = ""
    )

    private val userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding = MatchingRecyclerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init Variables
        db = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("userId").toString()

        // Read Users Informations From Firebase
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    for (document in documents) {
                        val dbUser = document.data
                        val id = dbUser["ID"] as String
                        val nick = dbUser["NickName"] as String
                        val score = dbUser["Score"] as Long
                        val status = dbUser["Status"] as Long
                        lateinit var user: User
                        if (status == 1.toLong()){
                            user = User(id, nick, score, "online")
                        }
                        else{
                            user = User(id, nick, score, "offline")
                        }

                        userList.add(user)
                    }
                    // score에 따라 내림차순 정렬
                    userList.sortByDescending { it.score }

                    val onItemClick: (String) -> Unit = { text ->
                        Toast.makeText(this, "Clicked: $text", Toast.LENGTH_SHORT).show()
                    }

                    binding.matchingRecyclingView.adapter = UserAdapter(userList, onItemClick)


                    // UserList를 Recycler View 에 띄워줘
                    binding.matchingRecyclingView.layoutManager = LinearLayoutManager(this)
                    binding.matchingRecyclingView.adapter = UserAdapter(userList, onItemClick)
                    binding.matchingRecyclingView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "DB 연결 실패", Toast.LENGTH_SHORT).show()
            }
    }

}