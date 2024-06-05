package com.example.termproject

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var Nickname: EditText
    private lateinit var RID: EditText
    private lateinit var RPassword: EditText
    private lateinit var btnComplete: Button
    private lateinit var btnBack: Button
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

        btnComplete.setOnClickListener {
            val nickname = Nickname.text.toString().trim()
            val id = RID.text.toString().trim()
            val password = RPassword.text.toString().trim()

            if (nickname.isEmpty() || id.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "빈칸없이 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                // Perform registration operation
                writeFirebase(nickname, id, password)
            }
        }
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun md5(input:String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    fun writeFirebase(nickname: String, id: String, pw: String) {
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
                        .addOnSuccessListener { document->
                            if (document.exists()) {
                                Toast.makeText(this, "NickName이 이미 존재합니다.", Toast.LENGTH_SHORT).show()
                            } else{
                                val encryptedPW = md5(pw)
                                val user = mapOf(
                                    "ID" to id,
                                    "Password" to encryptedPW,
                                    "NickName" to nickname,
                                    "Score" to 0,
                                    "Status" to 0,
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