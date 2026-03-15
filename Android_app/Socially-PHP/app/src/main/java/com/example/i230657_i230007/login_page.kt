package com.example.i230657_i230007

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.google.firebase.messaging.FirebaseMessaging


class login_page : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val usernameField = findViewById<EditText>(R.id.usernameField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val signUpText = findViewById<TextView>(R.id.signUpText)



        loginButton.setOnClickListener {
            val emailOrUsername = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (emailOrUsername.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(emailOrUsername, password)

        }

        signUpText.setOnClickListener {
            startActivity(Intent(this, signup_page::class.java))
        }
    }

    private fun performLogin(emailOrUsername: String, password: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/login.php"

        val body = JSONObject().apply {
            put("email", emailOrUsername)
            put("password", password)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                if (response.getString("status") == "success") {


                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                    val user = response.getJSONObject("user")
                    val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                    prefs.edit().apply {
                        putBoolean("isLoggedIn", true)
                        putString("user_id", user.getString("user_id"))
                        putString("username", user.getString("username"))
                        putString("email", user.getString("email"))
                        apply()
                    }

                    fetchAndSendFcmToken(user.getString("user_id"))
                    // after saving SharedPreferences (inside performLogin success)
                    setUserOnline(user.getString("user_id"))


                    val intent = Intent(this, switch_accounts_page::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)




                } else {
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
            })
        queue.add(request)
    }

    private fun fetchAndSendFcmToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            sendFcmTokenToServer(userId, token)
        }
    }

    private fun sendFcmTokenToServer(userId: String, token: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/save_fcm_token.php"
        val body = JSONObject().apply {
            put("user_id", userId)
            put("fcm_token", token)
        }

        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                Log.d("FCM", "Token saved: ${response.toString()}")
            },
            { error -> Log.e("FCM", "Failed to save token: ${error.message}") }
        )
        rq.add(req)
    }

    // call-volley helper to set online/offline
    private fun setUserOnline(userId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/set_user_online.php"
        val body = JSONObject().apply { put("user_id", userId) }

        val rq = Volley.newRequestQueue(this)
        val req = JsonObjectRequest(Request.Method.POST, url, body,
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
