package com.example.i230657_i230007

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

class launch_page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.launch_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.launch_page)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser

            val intent = if (currentUser != null) {
                val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                val appUserId = prefs.getString("user_id", null) // if you sync this earlier
                if (appUserId != null) {
                    setUserOnline(appUserId)
                } else {
                    // if you don't yet have app prefs, you might fetch user_id from your backend,
                    // but in many flows prefs are set at login
                }
                // User is already logged in
                Intent(this, home_feed::class.java)
            } else {
                // No user logged in
                Intent(this, login_page::class.java)
            }
            startActivity(intent)
            finish()
        }, 5000)
    }

    // call-volley helper to set online/offline
    private fun setUserOnline(userId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/set_user_online.php"
        val body = JSONObject().apply { put("user_id", userId) }

        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(
            Request.Method.POST, url, body,
            { resp -> /* optional success handling */ },
            { err -> /* optional error logging */ }
        )
        rq.add(req)
    }

    private fun setUserOffline(userId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/set_user_offline.php"
        val body = JSONObject().apply { put("user_id", userId) }

        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { resp -> /* optional success handling */ },
            { err -> /* optional error logging */ }
        )
        rq.add(req)
    }

}
