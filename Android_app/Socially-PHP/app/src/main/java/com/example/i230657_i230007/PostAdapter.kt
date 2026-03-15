package com.example.i230657_i230007

import User
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

class PostAdapter(
    private val posts: ArrayList<Pair<Post, User>>,
    private val onUsernameClick: (String) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.postProfileImage)
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val caption: TextView = itemView.findViewById(R.id.postCaption)
        val likesCounter: TextView = itemView.findViewById(R.id.likesCounter)
        val postImagesPager: androidx.viewpager2.widget.ViewPager2 = itemView.findViewById(R.id.vpPostImages)
        val likeIcon: ImageView = itemView.findViewById(R.id.post1LikeIcon)
        val commentIcon: ImageView = itemView.findViewById(R.id.post1CommentIcon)
        val shareIcon: ImageView = itemView.findViewById(R.id.post1ShareIcon)
        val saveIcon: ImageView = itemView.findViewById(R.id.post1SaveIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val (post, user) = posts[position]

        // --- User info ---
        holder.username.text = user.username
        if (user.profilePictureUrl.isNotEmpty()) {
            try {
                val profileBytes = Base64.decode(user.profilePictureUrl, Base64.DEFAULT)
                val profileBitmap = BitmapFactory.decodeByteArray(profileBytes, 0, profileBytes.size)
                holder.profileImage.setImageBitmap(profileBitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.placeholder_pfp)
        }

        // --- Post images ---
        holder.postImagesPager.adapter = ImagePagerAdapter(post.imagesBase64)

        // --- Dots ---
        val dotsContainer = holder.itemView.findViewById<LinearLayout>(R.id.post1Dots)
        dotsContainer.removeAllViews()
        val dotList = mutableListOf<ImageView>()
        for (i in post.imagesBase64.indices) {
            val dot = ImageView(holder.itemView.context)
            val size = 6.dpToPx()
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = 4.dpToPx()
            dot.layoutParams = params
            dot.setImageResource(if (i == 0) R.drawable.brown_circle else R.drawable.white_circle)
            dotsContainer.addView(dot)
            dotList.add(dot)
        }
        holder.postImagesPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                dotList.forEachIndexed { index, imageView ->
                    imageView.setImageResource(if (index == position) R.drawable.brown_circle else R.drawable.white_circle)
                }
            }
        })

        // --- Caption ---
        val spannable = android.text.SpannableStringBuilder()
        spannable.append(user.username)
        spannable.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, user.username.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(object : android.text.style.ClickableSpan() {
            override fun onClick(widget: View) {
                onUsernameClick(user.username)
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.isUnderlineText = false
                ds.color = android.graphics.Color.BLACK
            }
        }, 0, user.username.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (post.caption.isNotEmpty()) spannable.append(" ").append(post.caption)
        holder.caption.text = spannable
        holder.caption.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        // --- Likes setup ---
        val prefs = holder.itemView.context.getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)
        val currentUserId = prefs.getString("user_id", "") ?: ""

        // Initial drawable
        holder.likeIcon.setImageResource(if (post.likedBy.containsKey(currentUserId)) R.drawable.red_like else R.drawable.like)
        holder.likesCounter.text = post.likes.toString()

        holder.likeIcon.setOnClickListener {
            val action = if (post.likedBy.containsKey(currentUserId)) "unlike" else "like"
            val url = "http://192.168.0.102/socially_web_api_endpoints_php/like_post.php"
            val body = JSONObject().apply {
                put("post_id", post.postId)
                put("user_id", currentUserId)
                put("action", action)
            }

            val queue = Volley.newRequestQueue(holder.itemView.context)
            val request = JsonObjectRequest(Request.Method.POST, url, body,
                { response ->
                    if (response.getBoolean("success")) {
                        val newLikes = response.getInt("likes")
                        post.likes = newLikes
                        if (action == "like") post.likedBy[currentUserId] = true else post.likedBy.remove(currentUserId)
                        holder.likesCounter.text = post.likes.toString()
                        holder.likeIcon.setImageResource(if (post.likedBy.containsKey(currentUserId)) R.drawable.red_like else R.drawable.like)
                    } else {
                        Toast.makeText(holder.itemView.context, "Failed to update like", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    Toast.makeText(holder.itemView.context, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                })
            queue.add(request)
        }

        // --- Click handlers ---
        holder.username.setOnClickListener { onUsernameClick(user.username) }
        holder.profileImage.setOnClickListener { holder.username.performClick() }
        holder.commentIcon.setOnClickListener {
            val intent = Intent(holder.itemView.context, comments_page::class.java)
            intent.putExtra("postId", post.postId)
            holder.itemView.context.startActivity(intent)
        }
        holder.shareIcon.setOnClickListener {
            val context = holder.itemView.context
            val firstImage = if (post.imagesBase64.isNotEmpty()) post.imagesBase64[0] else ""
            if (context is AppCompatActivity) {
                val dialog = SharePostDialogFragment.newInstance(
                    postId = post.postId,
                    userId = user.userId,
                    userProfilePicture = user.profilePictureUrl,
                    postCaption = post.caption,
                    postImage = firstImage,
                    timestamp = post.createdAt
                )
                dialog.show(context.supportFragmentManager, "SharePostDialog")
            }
        }
        holder.saveIcon.setOnClickListener { /* Placeholder */ }
    }

    override fun getItemCount(): Int = posts.size
}

fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
