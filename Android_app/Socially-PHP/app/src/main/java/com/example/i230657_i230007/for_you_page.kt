package com.example.i230657_i230007

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject

class for_you_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ForYouPageAdapter
    private val postsList = ArrayList<Post>()
    private lateinit var CURRENT_USER_ID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.for_you_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔹 Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewForYou)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = ForYouPageAdapter(postsList)
        recyclerView.adapter = adapter

        loadAllPosts()

        // --- Bottom nav & search setup (already done by you) ---
        setupNavigation()
    }

    private fun loadAllPosts() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_all_posts.php"

        val request = object : StringRequest(
            Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val postsArray = json.getJSONArray("posts")

                        postsList.clear()

                        for (i in 0 until postsArray.length()) {
                            val p = postsArray.getJSONObject(i)

                            val imagesJsonArray = p.getJSONArray("imagesBase64")
                            val imagesList = ArrayList<String>()
                            for (j in 0 until imagesJsonArray.length()) {
                                imagesList.add(imagesJsonArray.getString(j))
                            }

                            val post = Post(
                                postId = p.getString("postId"),
                                userId = p.getString("userId"),
                                caption = p.getString("caption"),
                                likes = p.getInt("likes"),
                                imagesBase64 = imagesList,
                                createdAt = p.getLong("createdAt")
                            )

                            postsList.add(post)
                        }

                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {}

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }


    private fun setupNavigation() {
        val searchBarLayout = findViewById<LinearLayout>(R.id.searchBarLayout)
        searchBarLayout.setOnClickListener {
            startActivity(Intent(this, search_page::class.java))
        }
        findViewById<ImageView>(R.id.bottomNavHome).setOnClickListener {
            startActivity(Intent(this, home_feed::class.java))
        }
        findViewById<ImageView>(R.id.bottomNavCreate).setOnClickListener {
            startActivity(Intent(this, upload_page::class.java))
        }
        findViewById<ImageView>(R.id.bottomNavLikes).setOnClickListener {
            startActivity(Intent(this, notifications_page::class.java))
        }


        val bottomNavProfile = findViewById<ImageView>(R.id.bottomNavProfile)

        bottomNavProfile.setOnClickListener {
            startActivity(Intent(this, profile_page::class.java))
        }


    }

    private fun loadUserProfileImage(bottomNavProfile: ImageView) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_user_profile.php?user_id=$CURRENT_USER_ID"

        val request = object : StringRequest(
            Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val userObj = json.getJSONObject("user")

                        val base64 = userObj.getString("profile_picture_url")

                        if (!base64.isNullOrEmpty()) {
                            val decoded = Base64.decode(base64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)

                            bottomNavProfile.setImageBitmap(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(this, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {}

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

}
