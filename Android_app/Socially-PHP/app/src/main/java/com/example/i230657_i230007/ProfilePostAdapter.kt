package com.example.i230657_i230007

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ProfilePostAdapter(
    private val posts: List<Post>
) : RecyclerView.Adapter<ProfilePostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val imageView = ImageView(parent.context)
        val size = parent.resources.displayMetrics.widthPixels / 3
        imageView.layoutParams = ViewGroup.LayoutParams(size, size)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setPadding(1, 1, 1, 1)
        return PostViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        if (post.imagesBase64.isNotEmpty()) {
            val decodedBytes = Base64.decode(post.imagesBase64[0], Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            holder.imageView.setImageBitmap(bitmap)
        }

        holder.imageView.setOnClickListener {
            val intent = Intent(holder.itemView.context, own_posts_feed::class.java)
            intent.putExtra("USER_ID", post.userId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = posts.size
}
