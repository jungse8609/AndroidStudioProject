package com.example.termproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.termproject.databinding.ItemUserBinding

class UserAdapter(private val userList: List<MatchingRecyclingView.User>, private val onItemClick: (String) -> Unit) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val sortedUserList = userList.sortedByDescending { it.score }

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: MatchingRecyclingView.User) {
            binding.ItemRank.text = getRank(user).toString()
            binding.ItemProfile.setImageResource(user.profile.toInt())
            binding.ItemNick.text = user.nick
            binding.ItemScore.text = user.score.toString()
            when (user.status) {
                0L -> binding.ItemConnect.setImageResource(R.drawable.grey_dot)
                1L -> binding.ItemConnect.setImageResource(R.drawable.green_dot)
            }
            binding.ItemFight.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = userList[position]
                    onItemClick(item.id)
                }
            }
        }
    }

    private fun getRank(user: MatchingRecyclingView.User) : Int {
        return sortedUserList.indexOfFirst { it.id == user.id } + 1
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
