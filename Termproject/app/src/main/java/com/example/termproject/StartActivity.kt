package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.termproject.databinding.ActivityStartBinding
import com.google.firebase.firestore.FirebaseFirestore

class StartActivity : AppCompatActivity() {
    // XML Variables
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var userId: String
    lateinit var profileImageView: ImageView
    private val profileImages = arrayOf(
        R.drawable.profile1,
        R.drawable.profile2,
        R.drawable.profile3,
        R.drawable.profile4,
        R.drawable.profile5,
        R.drawable.profile6,
        R.drawable.profile7
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init XML
        val btnStart = binding.BtnStart
        userId = intent.getStringExtra("userId").toString()
        val userNick = intent.getStringExtra("userNick").toString()
        val userScore = intent.getLongExtra("userScore", -1)
        val profileImage = intent.getIntExtra("profileImage", R.drawable.profile1)

        val myId = findViewById<TextView>(R.id.myID)
        val myNick = findViewById<TextView>(R.id.myNick)
        val myScore = findViewById<TextView>(R.id.myScore)
        profileImageView = findViewById(R.id.profileImage)

        myId.text = userId
        myNick.text = userNick
        myScore.text = userScore.toString()
        profileImageView.setImageResource(profileImage)

        toggle = ActionBarDrawerToggle(this, binding.drawer, R.string.drawer_opened, R.string.drawer_closed)
        binding.drawer.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle.syncState()

        profileImageView.setOnClickListener {
            showProfileImageDialog()
        }

        btnStart.setOnClickListener {
            val intent = Intent(this, MatchingRecyclingView::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("userNick", userNick)
            intent.putExtra("userScore", userScore)
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
        builder.setTitle("프로필 사진 선택")

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
            imageView.setImageResource(profileImages[index])
            imageView.setOnClickListener {
                val selectedImage = profileImages[index]
                profileImageView.setImageResource(selectedImage)
                updateProfileImageInDatabase(selectedImage)
                dialog.dismiss() // Close the dialog when an image is selected
            }
        }

        dialog.show()
    }

    private fun updateProfileImageInDatabase(selectedImage: Int) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .update("ProfileImage", selectedImage)
            .addOnSuccessListener {
                Toast.makeText(this, "프로필 사진이 변경되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "프로필 사진 변경 실패", Toast.LENGTH_SHORT).show()
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
}
