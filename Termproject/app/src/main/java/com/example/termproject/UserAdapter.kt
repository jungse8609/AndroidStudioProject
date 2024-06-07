package com.example.termproject

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.termproject.databinding.ItemUserBinding

class UserAdapter(private val userList: List<MatchingRecyclingView.User>, private val onItemClick: (String) -> Unit, private val myId: String) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val sortedUserList = userList.sortedByDescending { it.score }

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: MatchingRecyclingView.User) {
            binding.ItemRank.text = user.rank.toString()
            Log.d("LogTemp", user.rank.toString())
            binding.ItemProfile.setImageResource(user.profile)
            binding.ItemNick.text = user.nick
            binding.ItemScore.text = user.score.toString()
            when (user.status) {
                0L -> binding.ItemConnect.setImageResource(R.drawable.grey_dot)
                1L -> binding.ItemConnect.setImageResource(R.drawable.green_dot)
            }

            val position = adapterPosition
            val item = userList[position]

            if (item.id == myId) binding.ItemFight.text = ""
            binding.ItemFight.setOnClickListener {
                if (position != RecyclerView.NO_POSITION) {
                    if (item.id != myId)
                        onItemClick(item.id)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount() = userList.size
}
