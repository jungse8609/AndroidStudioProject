package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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
                            Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                            document.reference.update("Status", 1)
//                                .addOnSuccessListener {
//                                    Toast.makeText(this, "상태 업데이트 성공", Toast.LENGTH_SHORT).show()
//                                }
//                                .addOnFailureListener { e ->
//                                    Toast.makeText(this, "상태 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
//                                }
                            val intent = Intent(this, StartActivity::class.java)
                            intent.putExtra("userId", ids)
                            startActivity(intent)
                            finish()
                        }
                        else{
                            //Toast.makeText(this, "${encryptedPW}", Toast.LENGTH_SHORT).show()
                            Toast.makeText(this, "ID 혹은 PW가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "ID 혹은 PW가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "DB 연결 실패", Toast.LENGTH_SHORT).show()
            }
    }
}
