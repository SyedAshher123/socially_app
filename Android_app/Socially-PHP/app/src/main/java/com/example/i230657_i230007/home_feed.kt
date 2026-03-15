package com.example.i230657_i230007

import User
import UserStats
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class home_feed : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private val postsList = ArrayList<Pair<Post, User>>()

    private lateinit var bottomNavProfile: ImageView

    private lateinit var storyRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private val userStoriesList = ArrayList<UserStories>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home_feed)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_home_feed)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getString("user_id", "") ?: ""

        // --- Check login using SharedPreferences ---
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        if (!isLoggedIn) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, login_page::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // --- Posts RecyclerView ---
        recyclerView = findViewById(R.id.recyclerViewPosts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PostAdapter(postsList) { username ->
            val intent = Intent(this, celebrity_follow_page::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }
        recyclerView.adapter = adapter


        storyRecyclerView = findViewById(R.id.recyclerViewStories)
        storyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        storyAdapter = StoryAdapter(userStoriesList, { selectedUserStory ->
            val intent = Intent(this, story_page::class.java)
            intent.putExtra("user_id", selectedUserStory.userId)
            startActivity(intent)
        }, currentUserId) // <-- pass currentUserId here

        storyRecyclerView.adapter = storyAdapter

        // --- Fetch stories from backend ---
        fetchStories { stories ->
            userStoriesList.clear()
            userStoriesList.addAll(stories)
            storyAdapter.notifyDataSetChanged()
        }




        // --- Bottom nav profile image ---
        bottomNavProfile = findViewById(R.id.bottomNavProfile)
        loadProfileImage(currentUserId)

        // --- Load posts ---
        fetchPosts(currentUserId)

        setupBottomNav()
        setupCameraMessage()
    }

    override fun onResume() {
        super.onResume()

        // Fetch posts again when returning to this activity
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getString("user_id", "") ?: ""

        fetchPosts(currentUserId)
    }

    private fun fetchStories(onComplete: (ArrayList<UserStories>) -> Unit) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_stories.php"
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val usersStoriesList = ArrayList<UserStories>()
                val usersArray = response.getJSONArray("users")
                for (i in 0 until usersArray.length()) {
                    val userObj = usersArray.getJSONObject(i)
                    val storiesArray = userObj.getJSONArray("stories")
                    val storiesList = mutableListOf<Story>()
                    for (j in 0 until storiesArray.length()) {
                        val s = storiesArray.getJSONObject(j)
                        storiesList.add(
                            Story(
                                storyId = s.getString("story_id"),
                                storyImageBase64 = s.getString("story_image"),
                                createdAt = s.getLong("created_at")
                            )
                        )
                    }

                    usersStoriesList.add(
                        UserStories(
                            userId = userObj.getString("user_id"),
                            username = userObj.getString("username"),
                            profilePictureBase64 = userObj.getString("profile_picture"),
                            stories = storiesList
                        )
                    )
                }
                onComplete(usersStoriesList)
            },
            { error ->
                Toast.makeText(this, "Failed to fetch stories: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }


    private fun loadProfileImage(userId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_user.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val userJson = response.getJSONObject("user")
                    val base64String = userJson.optString("profile_picture_url")
                    if (base64String.isNotEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            bottomNavProfile.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            },
            { error ->
                Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }


    private fun fetchPosts(currentUserId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_posts.php?user_id=$currentUserId"
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                postsList.clear()
                if (response.getString("status") == "success") {
                    val postsArray = response.getJSONArray("posts")
                    for (i in 0 until postsArray.length()) {
                        val postObj = postsArray.getJSONObject(i)
                        val post = Post(
                            postId = postObj.getJSONObject("post").getString("postId"),
                            userId = postObj.getJSONObject("post").getString("userId"),
                            caption = postObj.getJSONObject("post").getString("caption"),
                            likes = postObj.getJSONObject("post").getInt("likes"),
                            imagesBase64 = postObj.getJSONObject("post").getJSONArray("imagesBase64").let { arr ->
                                List(arr.length()) { arr.getString(it) }
                            },
                            createdAt = postObj.getJSONObject("post").getLong("createdAt")
                        )

                        val userObj = postObj.getJSONObject("user")
                        val user = User(
                            userId = userObj.getString("userId"),
                            username = userObj.getString("username"),
                            displayName = userObj.getString("displayName"),
                            profilePictureUrl = userObj.getString("profilePictureUrl")
                        )

                        postsList.add(Pair(post, user))
                    }
                    postsList.sortByDescending { it.first.createdAt }
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to fetch posts", Toast.LENGTH_SHORT).show()
            })
        queue.add(request)
    }



    private fun setupBottomNav() {
        findViewById<ImageView>(R.id.bottomNavSearch).setOnClickListener {
            startActivity(Intent(this, for_you_page::class.java))
        }
        findViewById<ImageView>(R.id.bottomNavCreate).setOnClickListener {
            startActivity(Intent(this, upload_page::class.java))
        }
        findViewById<ImageView>(R.id.bottomNavLikes).setOnClickListener {
            startActivity(Intent(this, notifications_page::class.java))
        }
        bottomNavProfile.setOnClickListener {
            startActivity(Intent(this, profile_page::class.java))
        }
    }

    private fun setupCameraMessage() {
        findViewById<ImageView>(R.id.iv_camera).setOnClickListener {
            startActivity(Intent(this, upload_page::class.java))
        }
        findViewById<ImageView>(R.id.iv_message).setOnClickListener {
            startActivity(Intent(this, all_chats_page::class.java))
        }
    }
}
