package com.example.termproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.termproject.databinding.MatchingRecyclerViewBinding
import com.google.firebase.firestore.FirebaseFirestore

class MatchingActivity : AppCompatActivity() {
    data class User(
        val id: String = "",
        val nick: String = "",
        val score: Long = 0,
        val status: String = ""
    )

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var userNick: String
    private lateinit var roomName: String
    private var userScore = 0
    private val userList = mutableListOf<User>()

    private var waitTimer: CountDownTimer? = null
    private val waitTimeLimit: Long = Long.MAX_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding = MatchingRecyclerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init Variables
        db = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("userId").toString()
        userNick = intent.getStringExtra("userNick").toString()

        // Start Timer Waiting Opponent's Chanllenge
        waitForOpponentChallenge()

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
        roomName = opponentId + "_" + userId + "_BattleRoom"
        val gameHp = 20
        var opponentScore = 0L
        var opponentNick: String? = null

        // Step 1: Fetch opponent details
        db.collection("users")
            .whereEqualTo("ID", opponentId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    for (document in documents) {
                        opponentScore = document["Score"] as Long
                        opponentNick = document["NickName"] as String
                    }
                    // Step 2: Create room settings and proceed
                    if (opponentNick != null) {
                        createBattleRoom(opponentId, gameHp, opponentScore, opponentNick!!)
                    } else {
                        Toast.makeText(this, "Opponent nickname not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Opponent not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch opponent details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createBattleRoom(opponentId: String, gameHp: Int, opponentScore: Long, opponentNick: String) {
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
        val opponentAccept = opponentId + "Accept"

        db.collection("BattleRooms").document(roomName).set(roomSetting)
            .addOnSuccessListener {
                db.collection("BattleWait").document(opponentId).set(srcId)
                    .addOnSuccessListener {
                        // Step 3: Wait for opponent acceptance
                        waitForOpponentAcceptance(roomName, opponentAccept)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to invite opponent", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create battle room", Toast.LENGTH_SHORT).show()
            }
    }

    private fun waitForOpponentAcceptance(roomName: String, opponentAccept: String) {
        db.collection("BattleRooms").document(roomName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error waiting for opponent acceptance", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val opponentChk = snapshot.getLong(opponentAccept) ?: 0L
                    if (opponentChk == 1L) {
                        // Step 4: Start the game
                        val intent = Intent(this, InGameActivity::class.java)
                        intent.putExtra("userId", userId)
                        intent.putExtra("roomName", roomName)
                        startActivity(intent)
                    }
                }
            }
    }

    private fun waitForOpponentChallenge() {
        db.collection("BattleWait").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(
                        this,
                        "Error waiting for opponent acceptance",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val ChallengeId = snapshot.getString(userId)
                    val ChallengeRoom = userId + "_" + ChallengeId + "_BattleRoom"

                    // 팝업 띄우기


                    db.collection("BattleRooms")
                        .document(ChallengeRoom)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null) {
                                document.reference.update(userId + "Accept", 1)
                                val intent = Intent(this, InGameActivity::class.java)
                                intent.putExtra("userId", userId)
                                intent.putExtra("roomName", roomName)
                                startActivity(intent)
                            }
                        }
                }
            }
    }


    // 상대방 수락 올 때까지 대기
    private fun waitChallenge() {
        waitTimer = object : CountDownTimer(waitTimeLimit, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // 결투 신청 왔는지 확인하기


                // 왔다면 timer 종료
                if (false)
                    waitTimer?.cancel()
            }

            override fun onFinish() {
                // 결투 신청 올 때까지 계속 wait
                waitChallenge()
            }
        }.start()
    }
}