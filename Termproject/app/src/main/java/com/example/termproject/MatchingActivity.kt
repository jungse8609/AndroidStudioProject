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
    private lateinit var userId: String
    private lateinit var userNick: String
    private var userScore = 0
    private lateinit var roomName: String

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
        userNick = intent.getStringExtra("userNick").toString()

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
                        //Toast.makeText(this, "Clicked: $text", Toast.LENGTH_SHORT).show()
                        makeGame(text)
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

    fun makeGame(opponentId: String) {
        roomName = opponentId + userId
        var gameHp = 20
        var opponentScore = 0.toLong()
        lateinit var opponentNick: String
        db.collection("users")
            .whereEqualTo("ID", opponentId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    for (document in documents) {
                        opponentScore = document["Score"] as Long
                        opponentNick = document["NickName"] as String
                    }
                }
            }
        val roomSetting = mapOf(
            "roomName" to roomName,
            "roundTime" to 0,
            "resultTime" to 0,
            userId + "Accept" to 1,
            userId + "HP" to gameHp,
            userId + "Score" to userScore,
            userId + "Nick" to userNick,
            userId + "Attack" to 0,
            userId + "Shield" to 0,
            userId + "Counter" to 0,
            userId + "Choose" to 0,
            opponentId + "Accept" to 0,
            opponentId + "HP" to gameHp,
            opponentId + "Score" to opponentScore,
            opponentId + "Nick" to opponentNick,
            opponentId + "Attack" to 0,
            opponentId + "Shield" to 0,
            opponentId + "Counter" to 0,
            opponentId + "Choose" to 0,
        )
        val srcId = mapOf("Opponent" to userId)
        var opponentChk = 0.toLong()
        val opponentAccept = opponentId + "Accept"
        db.collection("BattleRooms").document(roomName).set(roomSetting)
            .addOnSuccessListener {
                db.collection("BattleWait").document(opponentId).set(srcId)
                    .addOnSuccessListener {
                        while(opponentChk==0.toLong()){
                            db.collection("BattleRooms")
                                .whereEqualTo("roomName", roomName)
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (documents != null && !documents.isEmpty) {
                                        for (document in documents) {
                                            opponentChk = document[opponentAccept] as Long
                                        }
                                    }
                                }
                        }
                        val intent = Intent(this, InGameActivity::class.java)
                        intent.putExtra("userId", userId)
                        intent.putExtra("roomName", roomName)
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "상대방 초대 실패", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "결투장 진입 실패", Toast.LENGTH_SHORT).show()
            }
    }
}