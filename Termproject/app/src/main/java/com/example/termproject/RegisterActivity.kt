package com.example.termproject

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigInteger
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var Nickname: EditText
    private lateinit var RID: EditText
    private lateinit var RPassword: EditText
    private lateinit var btnComplete: Button
    private lateinit var btnBack: Button
    private lateinit var recyclerView: RecyclerView
    private var selectedProfileImage: String? = null
    private var chk = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        FirebaseApp.initializeApp(this)

        db = FirebaseFirestore.getInstance()

        Nickname = findViewById(R.id.Nickname)
        RID = findViewById(R.id.RID)
        RPassword = findViewById(R.id.RPassword)
        btnComplete = findViewById(R.id.btnComplete)
        btnBack = findViewById(R.id.btnBack)
        recyclerView = findViewById(R.id.recyclerView)

        val imageNames = listOf(
            "profile1",
            "profile2",
            "profile3",
            "profile4",
            "profile5",
            "profile6",
            "profile7"
        )

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ProfileAdapter(this, imageNames) { selectedImage ->
            selectedProfileImage = selectedImage // Receive image name
        }

        btnComplete.setOnClickListener {
            val nickname = Nickname.text.toString().trim()
            val id = RID.text.toString().trim()
            val password = RPassword.text.toString().trim()

            if (nickname.isEmpty() || id.isEmpty() || password.isEmpty()) {
                ToastUtils.createToast(this, "모든 칸을 채워주세요.")
            } else if (nickname.length > 5) {
                ToastUtils.createToast(this, "닉네임은 5자 이내로 작성해주세요")
            } else if (selectedProfileImage == null) {
                ToastUtils.createToast(this, "프로필 사진을 선택해주세요")
            } else {
                // Perform registration operation
                writeFirebase(nickname, id, password, selectedProfileImage!!) // Pass image name
            }
        }


        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    fun writeFirebase(nickname: String, id: String, pw: String, profileImage: String) {
        db.collection("users")
            .document(id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    ToastUtils.createToast(this, "ID가 이미 존재합니다.")
                } else {
                    db.collection("NickName")
                        .document(nickname)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                ToastUtils.createToast(this, "닉네임이 이미 존재합니다.")
                            } else {
                                val encryptedPW = md5(pw)
                                val user = mapOf(
                                    "ID" to id,
                                    "Password" to encryptedPW,
                                    "NickName" to nickname,
                                    "Score" to 0,
                                    "Status" to 0,
                                    "ProfileImage" to profileImage,
                                    "Rank" to 1
                                )
                                val nick = mapOf("ID" to id)
                                db.collection("users").document(id).set(user)
                                    .addOnSuccessListener {
                                        db.collection("NickName").document(nickname).set(nick)
                                            .addOnSuccessListener {
                                                chk = true
                                                finish()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this, "회원가입 실패", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "회원가입 실패", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "DB 연결 실패1", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "DB 연결 실패2", Toast.LENGTH_SHORT).show()
            }
    }
}
