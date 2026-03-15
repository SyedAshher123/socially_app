package com.example.i230657_i230007

import User
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

class search_page : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchAdapter
    private lateinit var clearTextView: TextView

    private var allUsers = mutableListOf<User>()

    private lateinit var allFilter: TextView
    private lateinit var followerFilter: TextView
    private lateinit var followingFilter: TextView

    private var currentFilter = "all"

    private var currentUserId: String? = null
    private val followersList = mutableSetOf<String>()
    private val followingList = mutableSetOf<String>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_page)

        // --- UI references ---
        searchEditText = findViewById(R.id.search_bar)
        recyclerView = findViewById(R.id.rvSearchResults)
        clearTextView = findViewById(R.id.tvClear)
        allFilter = findViewById(R.id.all_filter)
        followerFilter = findViewById(R.id.follower_filter)
        followingFilter = findViewById(R.id.following_filter)

        // --- Adapter & RecyclerView ---
        adapter = SearchAdapter(this, allUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // --- Get current user ID from SharedPreferences ---
        val prefs = getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)
        currentUserId = prefs.getString("user_id", null)

        // --- Load data from PHP backend ---
        loadAllUsers()
        loadFollowers()
        loadFollowing()

        // --- Clear search text ---
        clearTextView.setOnClickListener { searchEditText.text.clear() }

        // --- Handle end drawable click for clearing search ---
        searchEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = searchEditText.compoundDrawablesRelative[2]
                if (drawableEnd != null &&
                    event.rawX >= (searchEditText.right - drawableEnd.bounds.width() - searchEditText.paddingEnd)
                ) {
                    searchEditText.text.clear()
                    searchEditText.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // --- Text change listener for search ---
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterUsers(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // --- Filter buttons ---
        allFilter.setOnClickListener {
            currentFilter = "all"
            updateFilterUI()
            filterUsers(searchEditText.text.toString())
            recyclerView.scrollToPosition(0)
        }

        followerFilter.setOnClickListener {
            currentFilter = "followers"
            updateFilterUI()
            filterUsers(searchEditText.text.toString())
            recyclerView.scrollToPosition(0)
        }

        followingFilter.setOnClickListener {
            currentFilter = "following"
            updateFilterUI()
            filterUsers(searchEditText.text.toString())
            recyclerView.scrollToPosition(0)
        }
    }

    private fun updateFilterUI() {
        allFilter.setTextColor(if (currentFilter == "all") getColor(R.color.brown) else getColor(android.R.color.darker_gray))
        followerFilter.setTextColor(if (currentFilter == "followers") getColor(R.color.brown) else getColor(android.R.color.darker_gray))
        followingFilter.setTextColor(if (currentFilter == "following") getColor(R.color.brown) else getColor(android.R.color.darker_gray))
    }

    // --- Load all users from PHP ---
    private fun loadAllUsers() {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_all_users.php"
        val req = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val arr = json.getJSONArray("users")
                        allUsers.clear()
                        for (i in 0 until arr.length()) {
                            val u = arr.getJSONObject(i)
                            val user = User(
                                userId = u.getString("user_id"),
                                username = u.getString("username"),
                                displayName = u.getString("display_name"),
                                profilePictureUrl = u.getString("profile_picture_url")
                            )
                            allUsers.add(user)
                        }
                        filterUsers(searchEditText.text.toString())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing users", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error fetching users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    // --- Load followers ---
    private fun loadFollowers() {
        if (currentUserId == null) return
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_followers.php?user_id=$currentUserId"
        val req = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val arr = json.getJSONArray("followers")
                        followersList.clear()
                        for (i in 0 until arr.length()) {
                            followersList.add(arr.getString(i))
                        }
                    }
                } catch (_: Exception) {}
            },
            { _: Throwable? -> }
        )
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    // --- Load following ---
    private fun loadFollowing() {
        if (currentUserId == null) return
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_following.php?user_id=$currentUserId"
        val req = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val arr = json.getJSONArray("following")
                        followingList.clear()
                        for (i in 0 until arr.length()) {
                            followingList.add(arr.getString(i))
                        }
                    }
                } catch (_: Exception) {}
            },
            { _: Throwable? -> }
        )
        VolleySingleton.getInstance(this).addToRequestQueue(req)
    }

    // --- Filter logic ---
    private fun filterUsers(query: String) {
        var filteredList = allUsers.filter {
            it.username.contains(query, ignoreCase = true) ||
                    it.displayName.contains(query, ignoreCase = true)
        }

        when (currentFilter) {
            "followers" -> filteredList = filteredList.filter { followersList.contains(it.userId) }
            "following" -> filteredList = filteredList.filter { followingList.contains(it.userId) }
        }

        adapter.updateList(filteredList)
    }
}
