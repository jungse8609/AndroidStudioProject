package com.example.termproject

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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

    private lateinit var layoutTutorial : LinearLayout
    private lateinit var imgTutorial : ImageView
    private lateinit var btnTutorial : Button
    private lateinit var btnExit : Button
    private lateinit var btnNext : Button
    private lateinit var btnPrev : Button
    private val tutorialImages = listOf(
        R.drawable.tutorial01,
        R.drawable.tutorial02,
        R.drawable.tutorial03
    )
    private var currentIndex = 0

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
        val userRank = intent.getLongExtra("userRank", -1)
        val userProfile = intent.getStringExtra("profileImage").toString()

        val myNick = findViewById<TextView>(R.id.myNick)
        val myRank = findViewById<TextView>(R.id.myRank)
        val myScore = findViewById<TextView>(R.id.myScore)

        layoutTutorial = findViewById(R.id.LayoutTutorial)

        imgTutorial = findViewById(R.id.ImgTutorial)

        btnTutorial = findViewById(R.id.BtnTutorial)
        btnExit = findViewById(R.id.BtnExit)
        btnNext = findViewById(R.id.BtnNext)
        btnPrev = findViewById(R.id.BtnPrev)

        // # Tutorial Section Start
        updateTutorialImage()
        btnTutorial.setOnClickListener {
            layoutTutorial.visibility = View.VISIBLE
            currentIndex = 0
        }

        btnExit.setOnClickListener {
            layoutTutorial.visibility = View.GONE
            currentIndex = 0
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                updateTutorialImage()
            }
        }

        btnNext.setOnClickListener {
            if (currentIndex < tutorialImages.size - 1) {
                currentIndex++
                updateTutorialImage()
            }
        }
        // # Tutorial Section End

        profileImageView = findViewById(R.id.profileImage)

        myNick.text = userNick
        myScore.text = userScore.toString()
        myRank.text = userRank.toString() + "등"
        profileImageView.setImageResource(resources.getIdentifier(userProfile, "drawable", packageName))

        db = FirebaseFirestore.getInstance()

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
            intent.putExtra("userProfile", userProfile)
            intent.putExtra("userScore", userScore)
            startActivityForResult(intent, 100)
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            logout()
        }
    }

    private fun updateTutorialImage() {
        imgTutorial.setImageResource(tutorialImages[currentIndex])
        btnPrev.visibility = if (currentIndex == 0) View.INVISIBLE else View.VISIBLE
        btnNext.visibility = if (currentIndex == tutorialImages.size - 1) View.INVISIBLE else View.VISIBLE
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        var myRank = findViewById<TextView>(R.id.myRank)
        var myScore = findViewById<TextView>(R.id.myScore)

        var userScore : Long = -1
        var userRank : Long = -1

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                100 -> {
                    db.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null) {
                                userScore = document.getLong("Score") ?: 0
                                userRank = document.getLong("Rank") ?: 0

                                myScore.text = userScore.toString()
                                myRank.text = userRank.toString() + "등"
                            }
                        }

                    SoundManager.playBackgroundMusic(SoundManager.Bgm.LOBBY)
                }
            }
        }
    }
}
