package com.example.termproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.termproject.databinding.ItemUserBinding

class UserAdapter(private val userList: List<MatchingActivity.User>, private val onItemClick: (String) -> Unit) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        /*init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = userList[position]
                    onItemClick(item.nick)
                }
            }
        }*/

        fun bind(user: MatchingActivity.User) {
            binding.ItemNickname.text = user.nick
            binding.ItemScore.text = user.score.toString()
            binding.ItemStatus.text = user.status
            binding.ItemFight.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = userList[position]
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
