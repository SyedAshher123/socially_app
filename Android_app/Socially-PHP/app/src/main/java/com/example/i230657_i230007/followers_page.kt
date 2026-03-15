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

class followers_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val followersList = mutableListOf<User>()
    private lateinit var adapter: FollowersAdapter
    private lateinit var targetUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.followers_page)

        recyclerView = findViewById(R.id.Followers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FollowersAdapter(followersList)
        recyclerView.adapter = adapter

        targetUserId = intent?.getStringExtra("userId") ?: ""

        if (targetUserId.isEmpty()) {
            Toast.makeText(this, "Target user not acquired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Back Button
        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            finish()
        }

        loadFollowers()
    }

    private fun loadFollowers() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_followers_page.php?user_id=$targetUserId"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("FollowersPage", "Raw response: $response") // Log entire response

                followersList.clear()

                for (i in 0 until response.length()) {
                    val obj = response.getJSONObject(i)

                    val username = obj.optString("username", "Unknown")
                    val displayName = obj.optString("display_name", "")
                    Log.d("FollowersPage", "Follower #$i -> username: $username, displayName: $displayName") // Log each follower

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

                    followersList.add(user)
                }

                adapter.notifyDataSetChanged()
            },
            { error ->
                Toast.makeText(this, "Failed to load followers", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }



}
