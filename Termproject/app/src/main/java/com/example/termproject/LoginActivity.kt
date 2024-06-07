package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigInteger
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var ID: EditText
    private lateinit var Password: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        FirebaseApp.initializeApp(this)

        db = FirebaseFirestore.getInstance()
        ID = findViewById(R.id.ID)
        Password = findViewById(R.id.Password)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val id = ID.text.toString().trim()
            val password = Password.text.toString().trim()

            if (id.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "ID, PW를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                loginFirebase(id, password)
            }
        }

        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun md5(input:String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    private fun loginFirebase(ids: String, pws: String) {
        db.collection("users")
            .whereEqualTo("ID", ids)
            //.whereEqualTo("Password", PW)  // 비밀번호도 함께 확인
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    for (document in documents) {
                        val user = document.data
                        // 사용자 데이터를 처리합니다
                        val pw = user["Password"] as String
                        val encryptedPW = md5(pws)
                        if (pw != null && encryptedPW==pw){
                            val userStatus = user["Status"] as Long
                            if (userStatus == 1L){
                                ToastUtils.createToast(this, "접속 중인 아이디입니다")
                            }
                            else{
                                document.reference.update("Status", 1)

                                db.collection("Ranking")
                                    .document("Ranking")
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document != null && document.exists()) {
                                            val intent = Intent(this, StartActivity::class.java)
                                            val userNick = user["NickName"] as String
                                            val userScore = user["Score"] as Long
                                            val userImg = user["ProfileImage"] as String

                                            val scoreList = document.get("scoreList") as List<Map<String, Any>>

                                            // score에 따라 내림차순 정렬
                                            var userList = scoreList.map {
                                                Pair(it["ID"] as String, (it["Score"] as Long).toInt())
                                            }.toMutableList()

                                            userList.sortByDescending { it.second }

                                            // Ranking 계산
                                            val rankingList = mutableListOf<Triple<String, Int, Int>>()
                                            var currentRank = 1
                                            var currentScore = userList.first().second
                                            var sameRankCounter = 0
                                            for ((index, item) in userList.withIndex()) {
                                                var (id, score) = item
                                                if (score == currentScore) {
                                                    rankingList.add(Triple(id, score, currentRank))
                                                    sameRankCounter += 1
                                                }
                                                else {
                                                    currentRank += sameRankCounter
                                                    sameRankCounter = 1
                                                    currentScore = score
                                                    rankingList.add(Triple(id, score, currentRank))
                                                }
                                            }

                                            // User 정보 업데이트
                                            for (item in rankingList) {
                                                db.collection("users").document(item.first).update("Rank", item.third)
                                                if (item.first == ids) {
                                                    intent.putExtra("userRank", item.third.toLong())
                                                }
                                            }


                                            intent.putExtra("userId", ids)
                                            intent.putExtra("userNick", userNick)
                                            intent.putExtra("userScore", userScore)
                                            intent.putExtra("profileImage", userImg)
                                            startActivity(intent)
                                            finish()
                                        }
                                    }


                            }
                        }
                        else{
                            ToastUtils.createToast(this, "ID 혹은 비밀번호가 틀렸습니다")
                        }
                    }
                } else {
                    ToastUtils.createToast(this, "ID 혹은 비밀번호가 틀렸습니다")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "DB 연결 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRanking() {

    }
}
