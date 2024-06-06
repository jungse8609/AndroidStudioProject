package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.termproject.databinding.MatchingRecyclerViewBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Timer
import java.util.TimerTask

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


                    // 결투 버튼 클릭 이벤트
                    val onItemClick: (String) -> Unit = { text ->
                        makeGame(text)

                        // 상대방 수락 대기 팝업창 띄워야함
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
            "roundTimerId" to userId,
            "acceptId" to opponentId,
            "resultTime" to 0,
            userId + "Accept" to 1,
            userId + "HP" to gameHp,
            userId + "Score" to userScore,
            userId + "Nick" to userNick,
            userId + "Attack" to 0,
            userId + "Defense" to 0,
            userId + "Counter" to 0,
            userId + "Choose" to 0,
            userId + "Round" to 0,
            opponentId + "Accept" to 0,
            opponentId + "HP" to gameHp,
            opponentId + "Score" to opponentScore,
            opponentId + "Nick" to opponentNick,
            opponentId + "Attack" to 0,
            opponentId + "Defense" to 0,
            opponentId + "Counter" to 0,
            opponentId + "Choose" to 0,
            opponentId + "Round" to 0,
        )
        val srcId = mapOf("Opponent" to userId)
        val opponentAccept = opponentId + "Accept"

        db.collection("BattleRooms").document(roomName).set(roomSetting)
            .addOnSuccessListener {
                db.collection("BattleWait").document(opponentId).set(srcId)
                    .addOnSuccessListener {
                        // Step 3: Wait for opponent acceptance
                        waitForOpponentAcceptance(roomName, opponentAccept, opponentId)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to invite opponent", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create battle room", Toast.LENGTH_SHORT).show()
            }
    }

    private fun waitForOpponentAcceptance(roomName: String, opponentAccept: String, opponentId: String) {
        val db = FirebaseFirestore.getInstance()
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                db.collection("BattleRooms").document(roomName).delete()
                    .addOnSuccessListener {
                        db.collection("BattleWait").document(opponentId).delete()
                            .addOnSuccessListener {
                                runOnUiThread {
                                    Toast.makeText(this@MatchingActivity, "Room deleted due to timeout", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                runOnUiThread {
                                    Toast.makeText(this@MatchingActivity, "Error deleting room: $e", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        runOnUiThread {
                            Toast.makeText(this@MatchingActivity, "Error deleting room: $e", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // Schedule the timer to delete the room after 10 seconds
        timer.schedule(timerTask, 10000)

        // 수락 대기 팝업창 띄워

        var hasGameStarted = false;

        db.collection("BattleRooms").document(roomName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error waiting for opponent acceptance", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists() && !hasGameStarted) {
                    val opponentChk = snapshot.getLong(opponentAccept) ?: 0L
                    // 상대가 수락한 경우
                    if (opponentChk == 1L) {
                        hasGameStarted = true // 플래그 설정

                        // Cancel the timer if opponent accepted
                        timer.cancel()

                        // Start the game
                        val intent = Intent(this, InGameActivity::class.java)
                        intent.putExtra("userId", userId)
                        intent.putExtra("opponentId", opponentId)
                        intent.putExtra("roomName", roomName)

                        Log.d("LogTemp", "게임 시작")

                        startActivity(intent)
                        finish()
                    }
                    // 상대가 거절한 경우
                    else if (opponentChk == -1L) {
                        db.collection("BattleRooms").document(roomName).delete()
                            .addOnSuccessListener {
                                runOnUiThread {
                                    Toast.makeText(this@MatchingActivity, "상대방이 거절했습니다", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                runOnUiThread {
                                    Toast.makeText(this@MatchingActivity, "Error deleting room: $e", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
            }
    }

    private fun waitForOpponentChallenge() {
        val fragmentManager: FragmentManager = supportFragmentManager

        var hasAccepted = false
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

                if (snapshot != null && snapshot.exists() && !hasAccepted) {
                    val opponentId = snapshot.getString("Opponent")

                    if (opponentId != null) {
                        val roomName = userId + "_" + opponentId + "_BattleRoom"

                        // 팝업 띄우기
                        val dialog = AcceptDeclineDialogFragment(opponentId) { accepted ->
                            if (accepted) {
                                hasAccepted = true

                                db.collection("BattleRooms")
                                    .document(roomName)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document != null) {
                                            document.reference.update(userId + "Accept", 1)
                                            val intent = Intent(this, InGameActivity::class.java)
                                            intent.putExtra("userId", userId)
                                            intent.putExtra("opponentId", opponentId)
                                            intent.putExtra("roomName", roomName)

                                            startActivity(intent)
                                            finish()
                                        }
                                    }
                            } else {
                                // 거절한 경우 처리
                                db.collection("BattleWait").document(userId).delete()
                                    .addOnSuccessListener {
                                        runOnUiThread {
                                            Toast.makeText(this@MatchingActivity, "거절했습니다", Toast.LENGTH_SHORT).show()
                                            db.collection("BattleRooms")
                                                .document(roomName)
                                                .get()
                                                .addOnSuccessListener { document ->
                                                    if (document != null) {
                                                        document.reference.update(userId + "Accept", -1)
                                                    }
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        runOnUiThread {
                                            Toast.makeText(this@MatchingActivity, "Error deleting room: $e", Toast.LENGTH_SHORT).show()
                                        }
                                            }
                            }
                        }
                        dialog.show(fragmentManager, "AcceptDeclineDialog")
                    }
                }
            }
    }

}