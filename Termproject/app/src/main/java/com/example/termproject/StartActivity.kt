package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.termproject.databinding.ActivityStartBinding
import com.google.firebase.firestore.FirebaseFirestore

class StartActivity : AppCompatActivity() {
    // XML Variables
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var userId: String
    lateinit var profileImageView: ImageView
    private val profileImageNames = arrayOf(
        "profile1",
        "profile2",
        "profile3",
        "profile4",
        "profile5",
        "profile6",
        "profile7"
    )

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sound Effect - BGM
        SoundManager.init(this)
        SoundManager.playBackgroundMusic(SoundManager.Bgm.LOBBY)

        // Init XML
        val btnStart = binding.BtnStart
        userId = intent.getStringExtra("userId").toString()
        val userNick = intent.getStringExtra("userNick").toString()
        val userScore = intent.getLongExtra("userScore", -1)
        val userProfile = intent.getStringExtra("profileImage").toString()

        val myNick = findViewById<TextView>(R.id.myNick)
        val myRank = findViewById<TextView>(R.id.myRank)
        val myScore = findViewById<TextView>(R.id.myScore)
        profileImageView = findViewById(R.id.profileImage)

        myNick.text = userNick
        myScore.text = userScore.toString()
        profileImageView.setImageResource(resources.getIdentifier(userProfile, "drawable", packageName))

        db = FirebaseFirestore.getInstance()

        /*// 유저 등수 실시간 업데이트
        db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("LogTemp", "Listen failed", e)
                return@addSnapshotListener
            }

            // Firestore에서 받아온 데이터를 userList에 추가
            var userList : MutableList<Pair<String, Int>> = mutableListOf()

            var rank : Int = 0

            for (document in snapshot!!.documents) {
                val dbUser = document.data
                val id = dbUser?.get("ID") as String
                val score = (dbUser["Score"] as Long).toInt()
                userList.add(Pair(id, score))

                rank = (dbUser["Rank"] as Long).toInt()
            }
            // score에 따라 내림차순 정렬
            userList.sortByDescending { it.second }
        }*/


        // DrawerView
        toggle = ActionBarDrawerToggle(this, binding.drawer, R.string.drawer_opened, R.string.drawer_closed)
        binding.drawer.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle.syncState()

        // 마이페이지 프로필 사진 업데이트
        profileImageView.setOnClickListener {
            showProfileImageDialog()
        }

        // 게임 시작 버튼
        btnStart.setSoundEffectsEnabled(false)
        btnStart.setOnClickListener {
            // Sound
            SoundManager.playSoundEffect(R.raw.sfx_click02)

            // Intent to Matching Acivity
            val intent = Intent(this, MatchingRecyclingView::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("userNick", userNick)
            intent.putExtra("userScore", userScore)
            intent.putExtra("userProfile", userProfile)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            logout()
        }
    }

    private fun showProfileImageDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_profile_images, null)
        builder.setView(dialogLayout)
        builder.setTitle("프로필 이미지 변경")

        val imageViews = arrayOf(
            dialogLayout.findViewById<ImageView>(R.id.image1),
            dialogLayout.findViewById<ImageView>(R.id.image2),
            dialogLayout.findViewById<ImageView>(R.id.image3),
            dialogLayout.findViewById<ImageView>(R.id.image4),
            dialogLayout.findViewById<ImageView>(R.id.image5),
            dialogLayout.findViewById<ImageView>(R.id.image6),
            dialogLayout.findViewById<ImageView>(R.id.image7)
        )

        val dialog = builder.create()

        imageViews.forEachIndexed { index, imageView ->
            val imageName = profileImageNames[index]
            val resourceId = resources.getIdentifier(imageName, "drawable", packageName)
            imageView.setImageResource(resourceId)
            imageView.setOnClickListener {
                profileImageView.setImageResource(resourceId)
                updateProfileImageInDatabase(imageName) // Pass image name instead of resource ID
                dialog.dismiss() // Close the dialog when an image is selected
            }
        }

        dialog.show()
    }

    private fun updateProfileImageInDatabase(selectedImage: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .update("ProfileImage", selectedImage)
            .addOnSuccessListener {
                ToastUtils.createToast(this, "프로필 이미지 업데이트")
            }
            .addOnFailureListener {
                Toast.makeText(this, "프로필 이미지 업데이트 실패", Toast.LENGTH_SHORT).show()
            }
    }


    private fun logout() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .update("Status", 0)
            .addOnSuccessListener {
                startActivity(Intent(this, LoginActivity::class.java))
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .whereEqualTo("ID", userId)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            document.reference.update("Status", 0)
                        }
                    }
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "로그아웃 실패", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("종료하시겠습니까?")
            .setCancelable(false)
            .setPositiveButton("종료") { dialog, id ->
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .whereEqualTo("ID", userId)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            document.reference.update("Status", 0)
                        }
                    }
                super.onBackPressed()
            }
            .setNegativeButton("취소") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}
