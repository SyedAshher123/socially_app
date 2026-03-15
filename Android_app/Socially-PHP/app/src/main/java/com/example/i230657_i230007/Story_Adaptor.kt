package com.example.i230657_i230007

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

class StoryAdapter(
    private val userStories: ArrayList<UserStories>,  // grouped stories per user
    private val onStoryClick: (UserStories) -> Unit,
    private val currentUserId: String
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storyImage: CircleImageView = itemView.findViewById(R.id.storyImage)
        val storyUsername: TextView = itemView.findViewById(R.id.storyLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.story_item_layout, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val userStory = userStories[position]
        holder.storyUsername.text = userStory.username

        // Display profile picture
        if (!userStory.profilePictureBase64.isNullOrEmpty()) {
            try {
                val cleaned = userStory.profilePictureBase64.replace("\n", "").replace("\r", "").trim()
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.storyImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.storyImage.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            holder.storyImage.setImageResource(R.drawable.placeholder_pfp)
        }

        // Default: gray border
        holder.storyImage.setBackgroundResource(R.drawable.story_border_viewed)

        // Check which stories are viewed
        checkViewedStatus(holder, userStory.stories.map { it.storyId })

        // Click → open story viewer
        holder.storyImage.setOnClickListener {
            onStoryClick(userStory)

            // Mark all stories as viewed
            userStory.stories.forEach { story ->
                markStoryViewed(holder.itemView, story.storyId)
            }

            // Immediately update UI
            holder.storyImage.setBackgroundResource(R.drawable.story_border_viewed)
        }
    }

    override fun getItemCount(): Int = userStories.size

    // ✅ Check if user has viewed any of the stories
    private fun checkViewedStatus(holder: StoryViewHolder, storyIds: List<String>) {
        val queue = Volley.newRequestQueue(holder.itemView.context)
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/check_viewed_stories.php?user_id=$currentUserId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val viewedArray = response.getJSONArray("viewed_story_ids")
                val viewedSet = mutableSetOf<String>()
                for (i in 0 until viewedArray.length()) {
                    viewedSet.add(viewedArray.getString(i))
                }

                // If any story is unviewed → purple border
                if (storyIds.any { !viewedSet.contains(it) }) {
                    holder.storyImage.setBackgroundResource(R.drawable.story_border_unviewed)
                } else {
                    holder.storyImage.setBackgroundResource(R.drawable.story_border_viewed)
                }
            },
            { error ->
                Toast.makeText(holder.itemView.context, "Failed to fetch viewed status", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    // ✅ Mark a single story as viewed
    private fun markStoryViewed(view: View, storyId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/mark_story_viewed.php"
        val body = JSONObject().apply {
            put("story_id", storyId)
            put("viewer_id", currentUserId)
        }

        val queue = Volley.newRequestQueue(view.context)
        val request = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                if (!response.getBoolean("success")) {
                    Toast.makeText(view.context, "Failed to mark viewed", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(view.context, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}
