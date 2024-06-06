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
    private var selectedProfileImage: Int? = null
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

        val images = listOf(
            R.drawable.profile1,
            R.drawable.profile2,
            R.drawable.profile3,
            R.drawable.profile4,
            R.drawable.profile5,
            R.drawable.profile6,
            R.drawable.profile7
        )

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ProfileAdapter(this, images) { selectedImage ->
            selectedProfileImage = selectedImage
        }

        btnComplete.setOnClickListener {
            val nickname = Nickname.text.toString().trim()
            val id = RID.text.toString().trim()
            val password = RPassword.text.toString().trim()

            if (nickname.isEmpty() || id.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "빈칸없이 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (nickname.length > 8) {
                Toast.makeText(this, "닉네임을 8글자 이내로 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (selectedProfileImage == null) {
                Toast.makeText(this, "프로필 사진을 선택해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                // Perform registration operation
                writeFirebase(nickname, id, password, selectedProfileImage!!)
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

    fun writeFirebase(nickname: String, id: String, pw: String, profileImage: Int) {
        db.collection("users")
            .document(id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Toast.makeText(this, "ID가 이미 존재합니다.", Toast.LENGTH_SHORT).show()
                } else {
                    db.collection("NickName")
                        .document(nickname)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                Toast.makeText(this, "NickName이 이미 존재합니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                val encryptedPW = md5(pw)
                                val user = mapOf(
                                    "ID" to id,
                                    "Password" to encryptedPW,
                                    "NickName" to nickname,
                                    "Score" to 0,
                                    "Status" to 0,
                                    "ProfileImage" to profileImage
                                )
                                val nick = mapOf("ID" to id)
                                db.collection("users").document(id).set(user)
                                    .addOnSuccessListener {
                                        db.collection("NickName").document(nickname).set(nick)
                                            .addOnSuccessListener {
                                                chk = true
                                                Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this, "DB 연결 실패", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "DB 연결 실패", Toast.LENGTH_SHORT).show()
            }
    }
}
