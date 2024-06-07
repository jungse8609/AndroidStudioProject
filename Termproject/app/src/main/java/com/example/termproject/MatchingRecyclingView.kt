package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.termproject.databinding.MatchingRecyclingViewBinding
import com.google.firebase.firestore.FirebaseFirestore

class MatchingRecyclingView : AppCompatActivity() {
    data class User(
        var profile: Int = 0,
        val id: String = "",
        val nick: String = "",
        val score: Long = 0,
        val status: Long = 0,
        val rank: Long = 0
    )

    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var userNick: String
    private lateinit var userProfile: String
    private lateinit var roomName: String
    private var userScore = 0
    private val userList = mutableListOf<User>()

    private lateinit var waitTimer: CountDownTimer
    private val waitTimeLimit: Long = 10000 // 10 seconds wait time

    private lateinit var binding : MatchingRecyclingViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MatchingRecyclingViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Backspace Button on App Bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        // Init Variables
        db = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("userId").toString()
        userNick = intent.getStringExtra("userNick").toString()
        userProfile = intent.getStringExtra("userProfile").toString()

        // SFX - BGM
        SoundManager.playBackgroundMusic(SoundManager.Bgm.LOBBY)

        waitForOpponentChallenge()

        // User List를 Recycling View에 추가하기
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    for (document in documents) {
                        val dbUser = document.data
                        val id = dbUser["ID"] as String
                        val nick = dbUser["NickName"] as String
                        var profile = dbUser["ProfileImage"] as String
                        val score = dbUser["Score"] as Long
                        val status = dbUser["Status"] as Long
                        var rank = dbUser["Rank"] as Long
                        lateinit var user: User

                        user = User(resources.getIdentifier(profile, "drawable", packageName), id, nick, score, status, rank)
                        userList.add(user)
                    }
                    // score에 따라 내림차순 정렬
                    userList.sortByDescending { it.score }

                    // 결투 버튼 클릭 이벤트
                    val onItemClick: (String) -> Unit = { text ->
                        makeGame(text)
                    }

                    // UserList를 Recycler View 에 띄워줘
                    binding.matchingRecyclingView.layoutManager = LinearLayoutManager(this)
                    binding.matchingRecyclingView.adapter = UserAdapter(userList, onItemClick, userId)
                    binding.matchingRecyclingView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

                    monitoring()
                }
            }
            .addOnFailureListener {
                createToast("DB 연결 실패")
            }
    }

    fun monitoring() {
        // Firestore에서 사용자 상태를 실시간으로 모니터링
        db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("LogTemp", "Listen failed", e)
                return@addSnapshotListener
            }

            // Firestore에서 받아온 데이터를 userList에 추가
            val updatedUserList = mutableListOf<User>()
            for (document in snapshot!!.documents) {
                val dbUser = document.data
                val id = dbUser?.get("ID") as String
                val nick = dbUser["NickName"] as String
                var profile = dbUser["ProfileImage"] as String
                val score = dbUser["Score"] as Long
                val status = dbUser["Status"] as Long
                lateinit var user: User
                if (status == 1L){
                    user = User(resources.getIdentifier(profile, "drawable", packageName), id, nick, score, 1)
                }
                else{
                    user = User(resources.getIdentifier(profile, "drawable", packageName), id, nick, score, 0)
                }

                updatedUserList.add(user)
            }
            // score에 따라 내림차순 정렬
            updatedUserList.sortByDescending { it.score }


            // 결투 버튼 클릭 이벤트
            val onItemClick: (String) -> Unit = { text ->
                makeGame(text)
            }

            userList.clear()
            userList.addAll(updatedUserList)

            // UserList를 Recycler View 에 띄워줘

            // 기존 RecyclingView의 Adapter 및 LayoutManager 제거
            binding.matchingRecyclingView.adapter = null
            binding.matchingRecyclingView.layoutManager = null
            for (i in 0 until binding.matchingRecyclingView.itemDecorationCount) {
                binding.matchingRecyclingView.removeItemDecorationAt(i)
            }

            // 새로운 RecyclingView
            binding.matchingRecyclingView.layoutManager = LinearLayoutManager(this)
            binding.matchingRecyclingView.adapter = UserAdapter(userList, onItemClick, userId)
            binding.matchingRecyclingView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        }
    }

    fun makeGame(opponentId: String) {
        roomName = opponentId + "_" + userId + "_BattleRoom"
        val gameHp = 20
        var opponentScore = 0L
        var opponentNick: String? = null
        var opponentProfile: String? = null

        // Step 1: Fetch opponent details
        db.collection("users")
            .whereEqualTo("ID", opponentId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    for (document in documents) {
                        opponentScore = document["Score"] as Long
                        opponentNick = document["NickName"] as String
                        opponentProfile = document["ProfileImage"] as String
                    }
                    // Step 2: Create room settings and proceed
                    if (opponentNick != null) {
                        createBattleRoom(opponentId, gameHp, opponentScore, opponentNick!!, opponentProfile!!)
                    } else {
                        createToast("상대방을 찾을 수 없습니다.")
                    }
                } else {
                    createToast("상대방을 찾을 수 없습니다.")
                }
            }
            .addOnFailureListener {
                createToast("상대방을 찾을 수 없습니다.")
            }
    }

    private fun createBattleRoom(opponentId: String, gameHp: Int, opponentScore: Long, opponentNick: String, opponentProfile: String) {
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
            userId + "Attack"  to 1,
            userId + "Defense" to 1,
            userId + "Counter" to 1,
            userId + "Choose"  to 0,
            userId + "Round"   to 0,
            userId + "Status" to 1,
            userId + "Profile" to userProfile,
            opponentId + "Accept" to 0,
            opponentId + "HP" to gameHp,
            opponentId + "Score" to opponentScore,
            opponentId + "Nick" to opponentNick,
            opponentId + "Attack"  to 1,
            opponentId + "Defense" to 1,
            opponentId + "Counter" to 1,
            opponentId + "Choose"  to 0,
            opponentId + "Round"   to 0,
            opponentId + "Status" to 1,
            opponentId + "Profile" to opponentProfile
        )
        val srcId = mapOf("Opponent" to userId,"OpponentNick" to opponentNick)
        val opponentAccept = opponentId + "Accept"

        db.collection("BattleRooms").document(roomName).set(roomSetting)
            .addOnSuccessListener {
                db.collection("BattleWait").document(opponentId).set(srcId)
                    .addOnSuccessListener {
                        // Step 3: Wait for opponent acceptance
                        waitForOpponentAcceptance(roomName, opponentAccept, opponentId)
                    }
                    .addOnFailureListener {
                        createToast("Failed to invite opponent")
                    }
            }
            .addOnFailureListener {
                createToast("Failed to create battle room")
            }
    }

    private fun toastMatchingMessageAndDeleteDB(message : String, opponentId: String) {
        db.collection("BattleRooms").document(roomName).delete()
        db.collection("BattleWait").document(userId).delete()
        db.collection("BattleWait").document(opponentId).delete()

        createToast(message)
    }

    private fun createToast(message: String) {
        ToastUtils.createToast(this, message)
    }

    private fun waitForOpponentAcceptance(roomName: String, opponentAccept: String, opponentId: String) {
        val db = FirebaseFirestore.getInstance()
        val dialog = ChallengeWaitDialogFragment(waitTimeLimit, roomName, opponentId, opponentAccept) { accepted ->
            when (accepted) {
                -1 -> createToast("Error")
                0 -> toastMatchingMessageAndDeleteDB("상대방이 거절했습니다", opponentId)
                1 -> { // 상대 수락 : 인게임으로 넘어감
                    val intent = Intent(this, InGameActivity::class.java)
                    intent.putExtra("userId", userId)
                    intent.putExtra("opponentId", opponentId)
                    intent.putExtra("roomName", roomName)

                    startActivity(intent)
                }
                2 -> toastMatchingMessageAndDeleteDB("취소했습니다", opponentId)
                3 -> toastMatchingMessageAndDeleteDB("시간 초과", opponentId)
            }
        }
        dialog.show(supportFragmentManager, "ChallengeWaitDialog")
    }

    private fun waitForOpponentChallenge() {
        val fragmentManager: FragmentManager = supportFragmentManager
        var hasAccepted = false
        db.collection("BattleWait").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    createToast("오류가 발생했습니다.")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists() && !hasAccepted) {
                    val opponentId = snapshot.getString("Opponent")
                    val opponentNick = snapshot.getString("OpponentNick")

                    if (opponentId != null) {
                        roomName = userId + "_" + opponentId + "_BattleRoom"

                        // 팝업 띄우기
                        val dialog = AcceptDeclineDialogFragment(userId, opponentNick.toString()) { accepted ->
                            when (accepted) {
                                0 -> toastMatchingMessageAndDeleteDB("거절했습니다", opponentId)
                                1 -> { // 상대 수락 : 인게임으로 넘어감
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

                                                db.collection("BattleWait").document(userId).delete()
                                            }
                                        }
                                }
                                2 -> toastMatchingMessageAndDeleteDB("시간 초과", opponentId)
                                3 -> toastMatchingMessageAndDeleteDB("상대방이 취소했습니다", opponentId)
                            }
                        }
                        try {
                            dialog.show(fragmentManager, "AcceptDeclineDialog")
                        } catch (e : Exception) {
                            Log.w("LogTemp", e.toString())
                        }

                    }
                }
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}