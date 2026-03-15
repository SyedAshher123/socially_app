package com.example.i230657_i230007

import User
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class SearchAdapter(
    private val context: Context,
    private var userList: List<User>
) : RecyclerView.Adapter<SearchAdapter.UserViewHolder>() {

    private val prefs = context.getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)
    private val currentUserId = prefs.getString("user_id", "") ?: ""

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.default_profile)
        val displayName: TextView = itemView.findViewById(R.id.tvDisplayName)
        val username: TextView = itemView.findViewById(R.id.tvUsername)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.displayName.text = user.displayName
        holder.username.text = user.username

        if (user.profilePictureUrl.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(user.profilePictureUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        holder.itemView.setOnClickListener {
            if (user.userId == currentUserId) {
                val intent = Intent(context, profile_page::class.java)
                context.startActivity(intent)
            } else {
                val intent = Intent(context, celebrity_follow_page::class.java)
                intent.putExtra("userId", user.userId)
                context.startActivity(intent)
            }
        }
    }

    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }
}
