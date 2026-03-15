package com.example.i230657_i230007

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.android.volley.Response

class notifications_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowRequestAdapter
    private val requests = mutableListOf<Notification>() // Notification class extended below
    private lateinit var bottomNavHome: ImageView
    private lateinit var bottomNavCreate: ImageView
    private lateinit var bottomNavProfile: ImageView
    private lateinit var bottomNavSearch: ImageView
    private lateinit var backButton: ImageView

    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notifications_page)

        // get current user id from SharedPreferences (or pass via Intent)
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        currentUserId = prefs.getString("user_id", "") ?: ""
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.rvComments)
        adapter = FollowRequestAdapter(requests,
            onAccept = { notif -> handleAccept(notif) },
            onReject  = { notif -> handleReject(notif) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchNotifications()

        backButton = findViewById(R.id.back_button)
        backButton.setOnClickListener { finish() }

        setupBottomNavigation()
    }

    private fun fetchNotifications() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_notifications.php?user_id=$currentUserId"
        val req = StringRequest(Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        requests.clear()
                        val arr = json.getJSONArray("notifications")
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            val notif = Notification(
                                id = o.getInt("id").toString(),
                                fromUserId = o.getString("from_user"),
                                fromUsername = o.optString("from_username", ""),
                                fromProfilePicture = o.optString("from_profile_picture", ""),
                                type = o.optString("type", "follow_request"),
                                status = o.optString("status", "pending"),
                                timestamp = o.optLong("timestamp", 0L)
                            )
                            requests.add(notif)
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        // no notifications or error
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Failed to fetch notifications", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    private fun handleAccept(notif: Notification) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/accept_request.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        // refresh list
                        fetchNotifications()
                        Toast.makeText(this, "Accepted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error accepting", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Failed to accept", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "notification_id" to (notif.id ?: ""),
                    "current_user" to currentUserId
                )
            }
        }
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    private fun handleReject(notif: Notification) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/reject_request.php"
        val req = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        fetchNotifications()
                        Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error rejecting", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Failed to reject", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "notification_id" to (notif.id ?: ""),
                    "current_user" to currentUserId
                )
            }
        }
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    private fun setupBottomNavigation(){
        bottomNavHome = findViewById(R.id.bottom_nav_home)
        bottomNavCreate = findViewById(R.id.bottom_nav_create)
        bottomNavSearch = findViewById(R.id.bottom_nav_search)
        bottomNavProfile = findViewById(R.id.bottom_nav_profile)

        bottomNavHome.setOnClickListener { startActivity(Intent(this, home_feed::class.java)) }
        bottomNavCreate.setOnClickListener { startActivity(Intent(this, upload_page::class.java)) }
        bottomNavSearch.setOnClickListener { startActivity(Intent(this, for_you_page::class.java)) }
        bottomNavProfile.setOnClickListener { startActivity(Intent(this, profile_page::class.java)) }

        // load bottom nav profile pic from your users endpoint
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_user_profile.php?user_id=$currentUserId"
        val req = StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val user = json.getJSONObject("user")
                        val base64 = user.optString("profile_picture_url", "")
                        if (base64.isNotEmpty()) {
                            val bytes = Base64.decode(base64, Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            findViewById<ImageView>(R.id.bottom_nav_profile).setImageBitmap(bmp)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            Response.ErrorListener { /* ignore */ })
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }
}
