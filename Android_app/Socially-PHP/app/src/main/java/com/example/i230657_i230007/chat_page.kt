package com.example.i230657_i230007

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.content.Context
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.yourapp.adapters.MessageAdapter
import com.yourapp.models.Message
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import android.database.ContentObserver
import android.os.Build
import android.util.Log
import android.Manifest




class chat_page : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    private lateinit var screenshotObserver: ContentObserver

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    private lateinit var btnSend: ImageView
    private lateinit var btnAttach: ImageView
    private lateinit var inputMessage: EditText
    private lateinit var profileImage: CircleImageView
    private lateinit var txtUsername: TextView
    private lateinit var btnVanish: ImageView

    private lateinit var cacheDB: ChatCacheDB

    private lateinit var btnAudioCall: ImageView
    private lateinit var btnVideoCall: ImageView

    private var receiverId: String? = null
    private var chatId: Int? = null
    private var vanishMode = false
    private val PICK_IMAGE_REQUEST = 101

    private val baseUrl = "http://192.168.0.102/socially_web_api_endpoints_php/"

    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 3000L

    private val fetchRunnable = object : Runnable {
        override fun run() {
            if (chatId != null) fetchMessages()
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.chat_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_root_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        receiverId = intent.getStringExtra("receiverId")
        if (receiverId == null) {
            Toast.makeText(this, "Receiver ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recycler_view_messages)
        btnSend = findViewById(R.id.btn_send)
        inputMessage = findViewById(R.id.input_message)
        profileImage = findViewById(R.id.img_profile_picture)
        txtUsername = findViewById(R.id.txt_username)
        btnAttach = findViewById(R.id.btn_attach)
        btnVanish = findViewById(R.id.btnVanish)

        btnAudioCall = findViewById(R.id.call)
        btnVideoCall = findViewById(R.id.video_call)


        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager

        val currentUser = getCurrentUserId() ?: ""
        messageAdapter = MessageAdapter(messageList, currentUser) { message ->
            showMessageOptionsDialog(message)
        }
        recyclerView.adapter = messageAdapter

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener { sendMessage() }
        btnAttach.setOnClickListener { openGallery() }
        btnAudioCall.setOnClickListener {
            val intent = Intent(this, call_page::class.java)
            intent.putExtra("userId", receiverId)
            startActivity(intent)
        }

        btnVideoCall.setOnClickListener {
            val intent = Intent(this, video_call_page::class.java)
            intent.putExtra("userId", receiverId)
            startActivity(intent)

        }


        btnVanish.setOnClickListener {
            vanishMode = !vanishMode
            Toast.makeText(this, if (vanishMode) "Vanish Mode ON" else "Vanish Mode OFF", Toast.LENGTH_SHORT).show()
        }

        loadReceiverProfile(receiverId!!)
        cacheDB = ChatCacheDB(this)
        ensureChatIdAndLoadMessages()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 2000)
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2000)
        }





    }

    override fun onResume() {
        super.onResume()
        handler.post(fetchRunnable)
        startScreenshotObserver()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(fetchRunnable)
        contentResolver.unregisterContentObserver(screenshotObserver)
        if (vanishMode) deleteVanishMessages()
    }

    private fun getCurrentUserId(): String? {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        return prefs.getString("user_id", null)
    }

    private fun loadReceiverProfile(userId: String) {
        val url = "$baseUrl/fetch_current_user.php?user_id=$userId"
        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.GET, url, null,
            { resp ->
                if (resp.optString("status") == "success") {
                    val user = resp.optJSONObject("user")
                    txtUsername.text =
                        user?.optString("display_name")?.ifEmpty { user.optString("username") } ?: "Unknown"
                    decodeProfileImage(user?.optString("profile_picture_url"))
                }
            },
            { err -> Toast.makeText(this, "Profile load error: ${err.message}", Toast.LENGTH_SHORT).show() }
        )
        rq.add(req)
    }

    private fun decodeProfileImage(base64String: String?) {
        if (!base64String.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                profileImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } catch (_: Exception) {}
        } else {
            profileImage.setImageResource(R.drawable.placeholder_pfp)
        }
    }

    private fun ensureChatIdAndLoadMessages() {
        val currentUser = getCurrentUserId() ?: return
        val prefs = getSharedPreferences("chat_session", MODE_PRIVATE)

        // Try to load chatId from local storage first
        chatId = prefs.getInt("chat_$receiverId", -1).takeIf { it != -1 }

        if (!isOnline()) {
            // Offline → load cached messages if chatId exists
            if (chatId != null) {
                Toast.makeText(this, "No internet → Loading offline messages", Toast.LENGTH_SHORT).show()
                loadOfflineMessages()
            } else {
                Toast.makeText(this, "No internet & no cached chat found", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Online → get or create chat
        val payload = JSONObject()
        payload.put("user1", currentUser)
        payload.put("user2", receiverId)

        val url = "$baseUrl/get_or_create_chat.php"
        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, payload,
            { resp ->
                if (resp.optString("status") == "success") {
                    chatId = resp.optInt("chat_id")

                    // Save chatId locally
                    prefs.edit().putInt("chat_$receiverId", chatId!!).apply()

                    // Fetch messages from server
                    fetchMessages()
                }
            },
            {
                // Error → load cached messages if chatId exists
                if (chatId != null) {
                    Toast.makeText(this, "Failed to connect → Loading offline messages", Toast.LENGTH_SHORT).show()
                    loadOfflineMessages()
                } else {
                    Toast.makeText(this, "Failed to connect & no cached chat", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rq.add(req)
    }


    private fun fetchMessages() {
        if (chatId == null) return

        if (!isOnline()) {
            Toast.makeText(this, "No internet → Loading offline messages", Toast.LENGTH_SHORT).show()
            loadOfflineMessages()
            return
        }

        val payload = JSONObject()
        payload.put("chat_id", chatId)
        payload.put("vanish_mode", if (vanishMode) 1 else 0)

        val url = "$baseUrl/fetch_messages.php"
        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, payload,
            { resp ->
                if (resp.optString("status") == "success") {
                    val arr = resp.optJSONArray("messages") ?: JSONArray()
                    val newList = mutableListOf<Message>()

                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)

                        val m = Message(
                            senderId = o.optString("sender_id"),
                            receiverId = receiverId ?: "",
                            messageText = o.optString("message_text"),
                            imageUrl = o.optString("image_base64"),
                            timestamp = o.optLong("timestamp"),
                            type = o.optString("message_type"),
                            messageId = o.optString("message_id"),
                            isVanish = o.optInt("is_vanish") == 1
                        )

                        newList.add(m)

                        // Save to local cache
                        cacheDB.saveMessage(
                            msgId = m.messageId ?: System.currentTimeMillis().toString(),
                            chatId = chatId!!,
                            sender = m.senderId,
                            text = m.messageText,
                            img = m.imageUrl,
                            type = m.type,
                            timestamp = m.timestamp,
                            vanish = if (m.isVanish) 1 else 0
                        )
                    }

                    // Update UI
                    messageList.clear()
                    messageList.addAll(newList)
                    messageAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messageList.size - 1)
                }
            },
            {
                // Volley error → load offline messages
                Toast.makeText(this, "No internet → Loading offline messages", Toast.LENGTH_SHORT).show()
                loadOfflineMessages()
            }
        )
        rq.add(req)
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val net = cm.activeNetworkInfo
        return net != null && net.isConnected
    }


    @SuppressLint("Range")
    private fun loadOfflineMessages() {
        if (chatId == null) return

        val cursor = cacheDB.getChatMessages(chatId!!)

        val offList = mutableListOf<Message>()

        while (cursor.moveToNext()) {
            val m = Message(
                senderId = cursor.getString(cursor.getColumnIndex(ChatCacheDB.COL_SENDER)),
                receiverId = receiverId ?: "",
                messageText = cursor.getString(cursor.getColumnIndex(ChatCacheDB.COL_TEXT)),
                imageUrl = cursor.getString(cursor.getColumnIndex(ChatCacheDB.COL_IMAGE)),
                timestamp = cursor.getLong(cursor.getColumnIndex(ChatCacheDB.COL_TIMESTAMP)),
                type = cursor.getString(cursor.getColumnIndex(ChatCacheDB.COL_TYPE)),
                messageId = cursor.getString(cursor.getColumnIndex(ChatCacheDB.COL_ID)),
                isVanish = cursor.getInt(cursor.getColumnIndex(ChatCacheDB.COL_VANISH)) == 1
            )
            offList.add(m)
        }

        cursor.close()

        // Sort messages by timestamp (oldest → newest)
        offList.sortBy { it.timestamp }

        // Update UI
        messageList.clear()
        messageList.addAll(offList)
        messageAdapter.notifyDataSetChanged()

        // Scroll to last message
        if (messageList.isNotEmpty()) {
            recyclerView.scrollToPosition(messageList.size - 1)
        }
    }


    private fun sendMessage() {
        val text = inputMessage.text.toString().trim()
        val sender = getCurrentUserId() ?: return
        val cid = chatId ?: return

        if (text.isEmpty()) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = JSONObject()
        payload.put("chat_id", cid)
        payload.put("sender_id", sender)
        payload.put("message_text", text)
        payload.put("message_type",  "text")
        payload.put("timestamp", System.currentTimeMillis())
        payload.put("is_vanish", if (vanishMode) 1 else 0)

        val url = "$baseUrl/send_message.php"
        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, payload,
            { resp ->
                if (resp.optString("status") == "success") {
                    val m = Message(
                        senderId = sender,
                        receiverId = receiverId!!,
                        messageText = text,
                        imageUrl = "",
                        timestamp = payload.optLong("timestamp"),
                        type = if (vanishMode) "vanish" else "text",
                        messageId = resp.optString("message_id"),
                        isVanish = vanishMode
                    )
                    messageList.add(m)
                    messageAdapter.notifyItemInserted(messageList.size - 1)
                    recyclerView.scrollToPosition(messageList.size - 1)
                    inputMessage.text.clear()
                }
            },
            { err -> Toast.makeText(this, "Send failed: ${err.message}", Toast.LENGTH_SHORT).show() }
        )
        rq.add(req)
        // After message is saved successfully
        sendChatPushNotification(text)
    }

    private fun sendImageMessage(imageBase64: String) {
        val sender = getCurrentUserId() ?: return
        val cid = chatId ?: return

        val payload = JSONObject()
        payload.put("chat_id", cid)
        payload.put("sender_id", sender)
        payload.put("image_base64", imageBase64)
        payload.put("message_type",  "image")
        payload.put("timestamp", System.currentTimeMillis())
        payload.put("is_vanish", if (vanishMode) 1 else 0)

        val url = "$baseUrl/send_message.php"
        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, payload,
            { resp ->
                if (resp.optString("status") == "success") {
                    val m = Message(
                        senderId = sender,
                        receiverId = receiverId!!,
                        messageText = "",
                        imageUrl = imageBase64,
                        timestamp = payload.optLong("timestamp"),
                        type = if (vanishMode) "vanish" else "image",
                        messageId = resp.optString("message_id"),
                        isVanish = vanishMode
                    )
                    messageList.add(m)
                    messageAdapter.notifyItemInserted(messageList.size - 1)
                    recyclerView.scrollToPosition(messageList.size - 1)
                }
            },
            { err -> Toast.makeText(this, "Image upload error: ${err.message}", Toast.LENGTH_SHORT).show() }
        )
        rq.add(req)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val base64 = convertImageToBase64(data.data!!)
            sendImageMessage(base64)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun convertImageToBase64(uri: Uri): String {
        val input = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
    }

    private fun showMessageOptionsDialog(message: Message) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Message Options")
        builder.setItems(arrayOf("Edit", "Delete", "Cancel")) { dialog, which ->
            val diff = System.currentTimeMillis() - message.timestamp
            when (which) {
                0 -> if (diff <= 5 * 60 * 1000) showEditMessageDialog(message)
                else Toast.makeText(this, "Edit time expired", Toast.LENGTH_SHORT).show()
                1 -> if (diff <= 5 * 60 * 1000) deleteMessageForBoth(message)
                else Toast.makeText(this, "Delete time expired", Toast.LENGTH_SHORT).show()
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun showEditMessageDialog(message: Message) {
        val input = EditText(this)
        input.setText(message.messageText)
        AlertDialog.Builder(this)
            .setTitle("Edit message")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) updateMessageForBoth(message, newText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateMessageForBoth(message: Message, newText: String) {
        val sender = getCurrentUserId() ?: return
        val payload = JSONObject()
        payload.put("message_id", message.messageId)
        payload.put("sender_id", sender)
        payload.put("new_text", newText)
        val url = "$baseUrl/edit_message.php"
        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, payload,
            {
                val idx = messageList.indexOfFirst { it.messageId == message.messageId }
                if (idx >= 0) {
                    messageList[idx].messageText = newText
                    messageAdapter.notifyItemChanged(idx)
                }
            },
            { err -> Toast.makeText(this, "Edit error: ${err.message}", Toast.LENGTH_SHORT).show() }
        )
        rq.add(req)
    }

    private fun deleteMessageForBoth(message: Message) {
        val sender = getCurrentUserId() ?: return
        val payload = JSONObject()
        payload.put("message_id", message.messageId)
        payload.put("sender_id", sender)
        val url = "$baseUrl/delete_message.php"
        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, payload,
            {
                val idx = messageList.indexOfFirst { it.messageId == message.messageId }
                if (idx >= 0) {
                    messageList[idx].messageText = "Message deleted"
                    messageAdapter.notifyItemChanged(idx)
                }
            },
            { err -> Toast.makeText(this, "Delete error: ${err.message}", Toast.LENGTH_SHORT).show() }
        )
        rq.add(req)
    }

    private fun deleteVanishMessages() {
        val sender = getCurrentUserId() ?: return
        val payload = JSONObject()
        payload.put("chat_id", chatId)
        payload.put("sender_id", sender)
        val url = "$baseUrl/delete_vanish_messages.php"
        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, payload, { }, { })
        rq.add(req)
    }

    private fun sendChatPushNotification(messageText: String) {
        val senderId = getCurrentUserId() ?: return
        val receiver = receiverId ?: return

        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_fcm_and_send_notification.php"

        val payload = JSONObject()
        payload.put("sender_id", senderId)
        payload.put("receiver_id", receiver)
        payload.put("message_text", messageText)

        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, payload,
            {
                // Optional: Log success
            },
            { err ->
                Toast.makeText(this, "Push error: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        )
        rq.add(req)
    }

    private fun startScreenshotObserver() {
        val handler = Handler(Looper.getMainLooper())
        screenshotObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                uri?.let { checkIfScreenshot(it) }
            }
        }
        

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver
        )
    }

    private fun checkIfScreenshot(uri: Uri) {
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val relativePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH))

                if (name.contains("screenshot", true) || relativePath?.contains("Screenshots", true) == true) {

                    Toast.makeText(this, "Screenshot detected!", Toast.LENGTH_SHORT).show()
                    Log.d("ScreenshotObserver", "Screenshot: $name | Path: $relativePath")

                    notifyReceiverScreenshot()
                }
            }
        }
    }


    private fun notifyReceiverScreenshot() {
        val senderId = getCurrentUserId() ?: return
        val receiver = receiverId ?: return

        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_fcm_and_send_notification.php"
        val payload = JSONObject()
        payload.put("sender_id", senderId)
        payload.put("receiver_id", receiver)
        payload.put("message_text", "$senderId took a screenshot of the chat") // customize

        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, payload,
            { /* Optional success log */ },
            { err -> Toast.makeText(this, "Screenshot push error: ${err.message}", Toast.LENGTH_SHORT).show() }
        )
        rq.add(req)
        Log.d("ScreenshotObserver", "Screenshot detected! Sending FCM...")

    }


}
