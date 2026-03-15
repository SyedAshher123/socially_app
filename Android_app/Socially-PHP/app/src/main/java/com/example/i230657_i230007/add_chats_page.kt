package com.example.i230657_i230007

import User
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class add_chats_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userList: MutableList<User>
    private lateinit var userAdapter: UserAdapter
    private lateinit var backButton: ImageView

    // Replace with your XAMPP server IP / localhost
    private val fetchUsersUrl = "http://192.168.0.102/socially_web_api_endpoints_php/fetch_users.php"
    private val addChatUrl = "http://192.168.0.102/socially_web_api_endpoints_php/add_chat.php"

    // Simulate current user ID (replace with session / shared preferences)
    private val currentUserId: String by lazy {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.getString("user_id", "") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_chats_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        userList = mutableListOf()
        userAdapter = UserAdapter(userList) { selectedUser ->
            addUserToChatList(selectedUser)
        }
        recyclerView.adapter = userAdapter

        backButton = findViewById(R.id.back_button_add_chat)
        backButton.setOnClickListener {
            val intent = Intent(this, all_chats_page::class.java)
            startActivity(intent)
            finish()
        }

        fetchAllUsers()
    }

    private fun fetchAllUsers() {
        val urlWithId = "http://192.168.0.102/socially_web_api_endpoints_php/fetch_users.php?current_user_id=$currentUserId"
        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, urlWithId, null,
            { response ->
                userList.clear()
                val usersArray = response.optJSONArray("users")
                if (usersArray != null) {
                    for (i in 0 until usersArray.length()) {
                        val obj = usersArray.getJSONObject(i)
                        val user = User(
                            userId = obj.getString("user_id"),
                            username = obj.getString("username"),
                            email = obj.getString("email")
                        )
                        userList.add(user)
                    }
                }

                if (userList.isEmpty()) {
                    Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show()
                }
                userAdapter.notifyDataSetChanged()
            },
            { error ->
                Toast.makeText(this, "Failed to fetch users: ${error.message}", Toast.LENGTH_LONG).show()
            })
        queue.add(request)
    }

    private fun addUserToChatList(selectedUser: User) {
        val queue = Volley.newRequestQueue(this)
        val body = JSONObject().apply {
            put("current_user_id", currentUserId)
            put("selected_user_id", selectedUser.userId)
        }

        val request = JsonObjectRequest(Request.Method.POST, addChatUrl, body,
            { response ->
                if (response.optString("status") == "success") {
                    Toast.makeText(this, "${selectedUser.username} added to chats", Toast.LENGTH_SHORT).show()
                    // Go back to all_chats_page
                    val intent = Intent(this, all_chats_page::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, response.optString("message"), Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Failed kt to add chat: ${error.message}", Toast.LENGTH_LONG).show()
            })
        queue.add(request)
    }
}
