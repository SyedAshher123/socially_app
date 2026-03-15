package com.example.i230657_i230007;

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnlineFollowersAdapter(
    private val list: List<OnlineFollower>,
    private val onClick: (OnlineFollower) -> Unit
) : RecyclerView.Adapter<OnlineFollowersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: ImageView = itemView.findViewById(R.id.storyImage)
        val username: TextView = itemView.findViewById(R.id.storyLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.story_item_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = list[position]

        holder.username.text = user.username

        // Load Base64 image
        if (user.profilePicBase64.isNotEmpty()) {
            val decoded = Base64.decode(user.profilePicBase64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            holder.profilePic.setImageBitmap(bmp)
        }

        holder.itemView.setOnClickListener {
            onClick(user)
        }
    }

    override fun getItemCount() = list.size
}

