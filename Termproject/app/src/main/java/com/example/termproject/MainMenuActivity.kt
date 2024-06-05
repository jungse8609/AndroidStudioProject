package com.example.termproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class MainMenuActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var buttonOpenDrawer: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainmenu)
        FirebaseApp.initializeApp(this)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        buttonOpenDrawer = findViewById(R.id.button_open_drawer)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        buttonOpenDrawer.setOnClickListener {
            drawerLayout.openDrawer(navView)
        }

        // Inflate header view
        val headerView = navView.inflateHeaderView(R.layout.nav_header_main)

        // Assuming you are passing userId from the login activity
        val userId = intent.getStringExtra("userId")
        if (userId != null) {
            loadUserInfo(userId, headerView)
        }

        loadUsers()
    }

    private fun loadUserInfo(userId: String, headerView: View) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val nickname = document.getString("NickName")
                    val id = document.getString("ID")
                    val score = document.getLong("Score")?.toInt()

                    val drawerNickname = headerView.findViewById<TextView>(R.id.drawer_nickname)
                    val drawerId = headerView.findViewById<TextView>(R.id.drawer_id)
                    val drawerScore = headerView.findViewById<TextView>(R.id.drawer_score)

                    drawerNickname.text = nickname
                    drawerId.text = id
                    drawerScore.text = score.toString()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUsers() {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                val userList = mutableListOf<User>()
                for (document in result) {
                    val user = document.toObject(User::class.java)
                    userList.add(user)
                }
                recyclerView.adapter = UserAdapter(userList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
    }
}

data class User(
    val ID: String = "",
    val NickName: String = "",
    val Score: Int = 0,
    val Status: Int = 0
)

class UserAdapter(private val userList: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewNickName: TextView = itemView.findViewById(R.id.text_view_nickname)
        val textViewScore: TextView = itemView.findViewById(R.id.text_view_score)
        val textViewStatus: TextView = itemView.findViewById(R.id.text_view_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.textViewNickName.text = currentUser.NickName
        holder.textViewScore.text = currentUser.Score.toString()
        holder.textViewStatus.text = if (currentUser.Status == 1) "Online" else "Offline"
    }

    override fun getItemCount() = userList.size
}
