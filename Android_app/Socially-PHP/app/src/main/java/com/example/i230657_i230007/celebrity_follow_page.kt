package com.example.i230657_i230007

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.android.volley.Response

class celebrity_follow_page : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvLink: TextView
    private lateinit var ivPfp: CircleImageView
    private lateinit var ivPfpBottom: CircleImageView
    private lateinit var recyclerViewPosts: RecyclerView
    private lateinit var postAdapter: ProfilePostAdapter
    private val postsList = mutableListOf<Post>()

    private lateinit var tvFollowerCount: TextView
    private lateinit var tvFollowingCount: TextView
    private lateinit var tvPostCount: TextView
    private lateinit var btnFollow: MaterialButton

    private var currentUserId: String = ""
    private var targetUserId: String = ""
    private var followStatus: String = "Follow" // Follow / Requested / Following

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.celebrity_follow_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // bind
        tvUsername = findViewById(R.id.tvUsername)
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvBio = findViewById(R.id.tvBio)
        tvLink = findViewById(R.id.tvLink)
        ivPfp = findViewById(R.id.pfp)
        ivPfpBottom = findViewById(R.id.bottom_nav_profile)
        tvFollowerCount = findViewById(R.id.tvFollowerCount)
        tvFollowingCount = findViewById(R.id.tvFollowingCount)
        tvPostCount = findViewById(R.id.tvPostCount)
        btnFollow = findViewById(R.id.btnFollow)
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts)

        recyclerViewPosts.layoutManager = GridLayoutManager(this, 3)
        postAdapter = ProfilePostAdapter(postsList)
        recyclerViewPosts.adapter = postAdapter

        // Get current user id from SharedPreferences (or Intent if you pass it)
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = prefs.getString("user_id", "") ?: ""

        // Target profile userId passed via Intent
        targetUserId = intent.getStringExtra("userId") ?: ""

        if (targetUserId.isEmpty()) {
            finish()
            return
        }

        // load
        loadUserProfile(targetUserId)
        loadUserPosts(targetUserId)
        checkFollowStatus()
        loadUserStats(targetUserId)

        btnFollow.setOnClickListener {
            when (followStatus) {
                "Follow" -> sendFollowRequest()
                "Requested" -> cancelFollowRequest()
                "Following" -> unfollow()
            }
        }

        tvFollowerCount.setOnClickListener {
            val intent = Intent(this, followers_page::class.java)
            intent.putExtra("userId", targetUserId)
            startActivity(intent)
        }

        tvFollowingCount.setOnClickListener {
            val intent = Intent(this, following_page::class.java)
            intent.putExtra("userId", targetUserId)
            startActivity(intent)
        }

        // nav clicks (same as before)
        findViewById<ImageView>(R.id.back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.bottom_nav_home).setOnClickListener {
            startActivity(Intent(this, home_feed::class.java))
        }
        findViewById<ImageView>(R.id.bottom_nav_search).setOnClickListener {
            startActivity(Intent(this, for_you_page::class.java))
        }
        findViewById<ImageView>(R.id.bottom_nav_create).setOnClickListener {
            startActivity(Intent(this, select_photo_page::class.java))
        }
        findViewById<ImageView>(R.id.bottom_nav_heart).setOnClickListener {
            startActivity(Intent(this, notis_page::class.java))
        }
        ivPfpBottom.setOnClickListener {
            startActivity(Intent(this, profile_page::class.java))
        }
    }

    private fun sendFollowRequest() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/send_follow_request.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    when (json.getString("status")) {
                        "success" -> {
                            followStatus = "Requested"
                            btnFollow.text = "Requested"
                        }
                        "requested" -> {
                            followStatus = "Requested"
                            btnFollow.text = "Requested"
                        }
                        "already_following" -> {
                            followStatus = "Following"
                            btnFollow.text = "Following"
                        }
                        else -> {
                            // show error toast optionally
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { /* handle volley error */ }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "current_user" to currentUserId,
                    "target_user" to targetUserId
                )
            }
        }
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    private fun cancelFollowRequest() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/cancel_request.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        followStatus = "Follow"
                        btnFollow.text = "Follow"
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener { /* handle */ }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "current_user" to currentUserId,
                    "target_user" to targetUserId
                )
            }
        }
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    private fun unfollow() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/unfollow_user.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        followStatus = "Follow"
                        btnFollow.text = "Follow"
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener { /* handle */ }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "current_user" to currentUserId,
                    "target_user" to targetUserId
                )
            }
        }
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    private fun checkFollowStatus() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/check_follow_status.php" +
                "?current_user=$currentUserId&target_user=$targetUserId"

        val req = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    followStatus = when (json.getString("follow_status")) {
                        "following" -> "Following"
                        "requested" -> "Requested"
                        else -> "Follow"
                    }
                    btnFollow.text = followStatus
                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener { /* handle */ })
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    // loadUserProfile, loadUserPosts, loadUserStats — keep your existing implementations
    private fun loadUserProfile(userId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_other_user_profile.php?user_id=$userId"
        val req = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val user = json.getJSONObject("user")
                        tvUsername.text = user.getString("username")
                        tvDisplayName.text = user.getString("display_name")
                        tvBio.text = user.optString("bio", "")
                        tvLink.text = user.optString("website", "")
                        val pfp = user.optString("profile_picture_url", "")
                        if (pfp.isNotEmpty()) {
                            val bytes = Base64.decode(pfp, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ivPfp.setImageBitmap(bmp)
                            ivPfpBottom.setImageBitmap(bmp)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener { /* handle */ })
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    private fun loadUserPosts(userId: String) {
        // reuse your existing get_user_posts endpoint call
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_user_posts.php?user_id=$userId"
        val req = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        postsList.clear()
                        val arr = json.getJSONArray("posts")
                        for (i in 0 until arr.length()) {
                            val p = arr.getJSONObject(i)
                            postsList.add(
                                Post(
                                    postId = p.getString("post_id"),
                                    userId = p.getString("user_id"),
                                    caption = p.optString("caption", ""),
                                    likes = p.optInt("likes", 0),
                                    imagesBase64 = listOf(p.getString("image")),
                                    createdAt = p.optLong("created_at", 0L)
                                )
                            )
                        }
                        postAdapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener { /* handle */ })
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    private fun loadUserStats(userId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_user_stats.php?user_id=$userId"
        val req = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    val stats = json.getJSONObject("stats")
                    tvFollowerCount.text = stats.getInt("follower_count").toString()
                    tvFollowingCount.text = stats.getInt("following_count").toString()
                    tvPostCount.text = stats.getInt("post_count").toString()
                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener { /* handle */ })
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }
}
