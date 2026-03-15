package com.example.i230657_i230007

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.yourapp.adapters.ChatAdapter
import com.yourapp.models.ChatPreview
import org.json.JSONArray
import org.json.JSONObject

class all_chats_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private var chatList = mutableListOf<ChatPreview>()

    private lateinit var backButton: ImageView
    private lateinit var newChatButton: ImageView
    private lateinit var usernamePreview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.all_chats_page)

        recyclerView = findViewById(R.id.chat_list_recycler)
        backButton = findViewById(R.id.back_button)
        newChatButton = findViewById(R.id.new_chat_button)
        usernamePreview = findViewById(R.id.username_text)

        setupRecyclerView()

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val currentUserId = prefs.getString("user_id", null)

        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchCurrentUsername(currentUserId)
        fetchChats(currentUserId)

        fetchOnlineFollowers(currentUserId)


        backButton.setOnClickListener {
            startActivity(Intent(this, home_feed::class.java))
            finish()
        }

        newChatButton.setOnClickListener {
            startActivity(Intent(this, add_chats_page::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.search_bar).setOnClickListener {
            startActivity(Intent(this, search_page::class.java))
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(chatList) { selectedChat ->
            val intent = Intent(this, chat_page::class.java)
            intent.putExtra("receiverId", selectedChat.userId)
            intent.putExtra("receiverUsername", selectedChat.username)
            intent.putExtra("receiverProfileBase64", selectedChat.profileImageBase64)
            startActivity(intent)
        }
        recyclerView.adapter = chatAdapter
    }

    private fun fetchCurrentUsername(currentUserId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/fetch_current_user.php?user_id=$currentUserId"

        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val username = response.getJSONObject("user").getString("username")
                    usernamePreview.text = username
                }
            },
            { error ->
                Toast.makeText(this, "Error fetching username: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        requestQueue.add(jsonObjectRequest)
    }

    private fun fetchChats(currentUserId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/fetch_chats.php"
        val body = JSONObject()
        body.put("current_user_id", currentUserId)

        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                chatList.clear()
                if (response.getString("status") == "success") {
                    val chatsArray: JSONArray = response.getJSONArray("chats")
                    for (i in 0 until chatsArray.length()) {
                        val chatObj = chatsArray.getJSONObject(i)
                        val chat = ChatPreview(
                            userId = chatObj.getString("user_id"),
                            username = chatObj.getString("username"),
                            displayName = chatObj.getString("display_name"),
                            profileImageBase64 = chatObj.getString("profile_picture_url"), // map correctly
                            lastMessage = chatObj.optString("last_message", "Tap to start chatting"),
                            lastMessageTime = chatObj.optString("last_message_time", "")
                        )
                        chatList.add(chat)
                    }
                    chatAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "No chats found", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error fetching chats: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        requestQueue.add(jsonObjectRequest)
    }

    private fun fetchOnlineFollowers(currentUserId: String) {

        val url = "http://192.168.0.102/socially_web_api_endpoints_php/fetch_online_followers.php?current_user_id=$currentUserId"

        val requestQueue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {

                    val arr = response.getJSONArray("online_followers")
                    val list = mutableListOf<OnlineFollower>()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            OnlineFollower(
                                userId = obj.getString("user_id"),
                                username = obj.getString("username"),
                                profilePicBase64 = obj.optString("profile_picture_url", "")
                            )
                        )
                    }

                    val adapter = OnlineFollowersAdapter(list) { user ->
                        // On click → open chat
                        val intent = Intent(this, chat_page::class.java)
                        intent.putExtra("receiverId", user.userId)
                        intent.putExtra("receiverUsername", user.username)
                        intent.putExtra("receiverProfileBase64", user.profilePicBase64)
                        startActivity(intent)
                    }

                    findViewById<RecyclerView>(R.id.recyclerViewStatus).apply {
                        layoutManager = LinearLayoutManager(this@all_chats_page, LinearLayoutManager.HORIZONTAL, false)
                        this.adapter = adapter
                    }
                }
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        requestQueue.add(request)
    }

}
