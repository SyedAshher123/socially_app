package com.example.i230657_i230007

import User
import UserStats
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley

class following_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val followingList = mutableListOf<User>()
    private lateinit var adapter: FollowingAdapter

    private var targetUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.following_page)

        recyclerView = findViewById(R.id.Following)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FollowingAdapter(followingList)
        recyclerView.adapter = adapter

        // Get target userId from intent
        targetUserId = intent.getStringExtra("userId")
        Log.d("FollowingPage", "Target userId: $targetUserId")

        if (targetUserId.isNullOrEmpty()) {
            Toast.makeText(this, "No userId passed", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Back button
        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            finish()
        }

        loadFollowing()
    }

    private fun loadFollowing() {
        val url =
            "http://192.168.0.102/socially_web_api_endpoints_php/get_following_page.php?user_id=$targetUserId"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("FollowingPage", "Raw response: $response")

                followingList.clear()

                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)
                    val username = obj.optString("username", "Unknown")
                    val displayName = obj.optString("display_name", "")
                    Log.d(
                        "FollowingPage",
                        "Following #$i -> username: $username, displayName: $displayName"
                    )

                    val user = User(
                        userId = obj.optString("user_id", ""),
                        email = "",
                        username = username,
                        displayName = displayName,
                        firstName = "",
                        lastName = "",
                        dateOfBirth = "",
                        phoneNumber = "",
                        bio = obj.optString("bio", ""),
                        profilePictureUrl = obj.optString("profile_picture_url", ""),
                        gender = "",
                        website = "",
                        accountPrivate = false,
                        createdAt = System.currentTimeMillis(),
                        isOnline = true,
                        lastSeen = System.currentTimeMillis(),
                        fcmToken = "",
                        stats = UserStats(),
                        followers = emptyMap(),
                        following = emptyMap()
                    )

                    followingList.add(user)
                }

                adapter.notifyDataSetChanged()
            },
            { error ->
                Toast.makeText(this, "Failed to load following", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}
