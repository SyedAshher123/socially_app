package com.example.i230657_i230007

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView

class profile_page : AppCompatActivity() {

    private lateinit var rvProfilePosts: RecyclerView
    private val postsList = ArrayList<Post>()
    private lateinit var postsAdapter: ProfilePostAdapter

    // Profile UI
    private lateinit var usernameText: TextView
    private lateinit var nameText: TextView
    private lateinit var bioText: TextView
    private lateinit var profilePic: CircleImageView

    // Stats UI
    private lateinit var postCountText: TextView
    private lateinit var followerCountText: TextView
    private lateinit var followingCountText: TextView

    private lateinit var iconMenu: ImageView

    private lateinit var queue: RequestQueue
    private lateinit var userId: String // dynamically loaded logged-in user ID

    // Bottom Nav UI
    private lateinit var bottomNavHome: ImageView
    private lateinit var bottomNavSearch: ImageView
    private lateinit var bottomNavCreate: ImageView
    private lateinit var bottomNavHeart: ImageView
    private lateinit var bottomNavProfileLayout: LinearLayout
    private lateinit var bottomNavProfilePic: CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profile_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_main_profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        queue = Volley.newRequestQueue(this)
        userId = getCurrentUserId()

        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- Init Profile Views ---
        usernameText = findViewById(R.id.placeholder_username)
        nameText = findViewById(R.id.placeholder_Name)
        bioText = findViewById(R.id.placeholder_bio)
        profilePic = findViewById(R.id.image_profile_pic)

        // --- Init Stats Views ---
        postCountText = findViewById(R.id.text_posts_count)
        followerCountText = findViewById(R.id.text_followers_count)
        followingCountText = findViewById(R.id.text_following_count)

        followerCountText.setOnClickListener {
            val intent = Intent(this, followers_page::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        followingCountText.setOnClickListener {
            val intent = Intent(this, following_page::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        // --- Buttons & Navigation ---
        findViewById<MaterialButton>(R.id.btn_edit_profile).setOnClickListener {
            startActivity(Intent(this, edit_profile_page::class.java))
        }

        iconMenu = findViewById(R.id.icon_menu)
        iconMenu.setOnClickListener {
            startActivity(Intent(this, logout_page::class.java))
            finish()
        }

        // --- RecyclerView ---
        rvProfilePosts = findViewById(R.id.rvProfilePosts)
        rvProfilePosts.layoutManager = GridLayoutManager(this, 3)
        postsAdapter = ProfilePostAdapter(postsList)
        rvProfilePosts.adapter = postsAdapter

        // --- Load data from PHP endpoints ---
        loadUserProfile()
        loadUserStats()
        loadUserPosts()

        // --- Setup Bottom Navigation ---
        setupBottomNav()
    }

    // --- Get logged-in user ID from SharedPreferences ---
    private fun getCurrentUserId(): String {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        return prefs.getString("user_id", "") ?: ""
    }

    private fun loadUserProfile() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_other_user_profile.php?user_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val user = response.getJSONObject("user")
                    usernameText.text = user.getString("username")
                    nameText.text = user.getString("display_name")
                    bioText.text = user.optString("bio", "")
                    val profilePicBase64 = user.optString("profile_picture_url", "")
                    if (profilePicBase64.isNotEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(profilePicBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            profilePic.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            },
            { error ->
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )
        queue.add(request)
    }

    private fun loadUserStats() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_user_stats.php?user_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val stats = response.getJSONObject("stats")
                    postCountText.text = stats.getInt("post_count").toString()
                    followerCountText.text = stats.getInt("follower_count").toString()
                    followingCountText.text = stats.getInt("following_count").toString()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to load stats", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )
        queue.add(request)
    }

    private fun loadUserPosts() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_profile_page_posts.php?user_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val postsJsonArray = response.getJSONArray("posts")
                    postsList.clear()

                    for (i in 0 until postsJsonArray.length()) {
                        val postObj = postsJsonArray.getJSONObject(i)
                        val imagesArray = ArrayList<String>()
                        val imagesJson = postObj.getJSONArray("images")
                        for (j in 0 until imagesJson.length()) {
                            imagesArray.add(imagesJson.getString(j))
                        }

                        val post = Post(
                            postId = postObj.getString("post_id"),
                            userId = postObj.getString("user_id"),
                            caption = postObj.getString("caption"),
                            likes = postObj.getInt("likes"),
                            imagesBase64 = imagesArray,
                            createdAt = postObj.getLong("created_at")
                        )
                        postsList.add(post)
                    }

                    // Sort newest first
                    postsList.sortByDescending { it.createdAt }
                    postsAdapter.notifyDataSetChanged()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )
        queue.add(request)
    }

    // --- Setup Bottom Navigation ---
    private fun setupBottomNav() {
        bottomNavHome = findViewById(R.id.bottom_nav_home)
        bottomNavSearch = findViewById(R.id.bottom_nav_search)
        bottomNavCreate = findViewById(R.id.bottom_nav_create)
        bottomNavHeart = findViewById(R.id.bottom_nav_heart)
        bottomNavProfileLayout = findViewById(R.id.layout_bottom_nav_profile)
        bottomNavProfilePic = findViewById(R.id.image_bottom_nav_profile)

        // Highlight current page (profile)
        bottomNavProfileLayout.alpha = 1f
        bottomNavHome.alpha = 0.5f
        bottomNavSearch.alpha = 0.5f
        bottomNavCreate.alpha = 0.5f
        bottomNavHeart.alpha = 0.5f

        // Load profile pic in bottom nav (from SharedPreferences if available)
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val profilePicBase64 = prefs.getString("profile_pic", "")
        if (!profilePicBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(profilePicBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                bottomNavProfilePic.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        bottomNavHome.setOnClickListener {
            startActivity(Intent(this, home_feed::class.java))
            finish()
        }

        bottomNavSearch.setOnClickListener {
            startActivity(Intent(this, for_you_page::class.java))
            finish()
        }

        bottomNavCreate.setOnClickListener {
            startActivity(Intent(this, upload_page::class.java))
        }

        bottomNavHeart.setOnClickListener {
            startActivity(Intent(this, notifications_page::class.java))
            finish()
        }

        bottomNavProfileLayout.setOnClickListener {
            // Already on profile, scroll to top
            findViewById<android.widget.ScrollView>(R.id.scroll_profile_content).smoothScrollTo(0, 0)
        }
    }
}
