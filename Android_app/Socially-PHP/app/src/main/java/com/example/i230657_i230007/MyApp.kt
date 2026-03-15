package com.example.i230657_i230007

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->

                when (event) {

                    Lifecycle.Event.ON_START -> {
                        // App enters foreground
                        markOnline()
                    }

                    Lifecycle.Event.ON_STOP -> {
                        // App goes to background
                        markOffline()
                    }

                    else -> { /* ignore all other lifecycle events */ }
                }
            }
        )
    }

    private fun markOnline() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null) ?: return

        val url = "http://192.168.0.102/socially_web_api_endpoints_php/set_user_online.php"
        val body = JSONObject().apply { put("user_id", userId) }

        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { Log.d("APP_LIFECYCLE", "User online") },
            { Log.e("APP_LIFECYCLE", "Failed to set online") }
        )

        Volley.newRequestQueue(this).add(req)
    }

    private fun markOffline() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null) ?: return

        val url = "http://192.168.0.102/socially_web_api_endpoints_php/set_user_offline.php"
        val body = JSONObject().apply { put("user_id", userId) }

        val req = JsonObjectRequest(Request.Method.POST, url, body,
            { Log.d("APP_LIFECYCLE", "User offline") },
            { Log.e("APP_LIFECYCLE", "Failed to set offline") }
        )

        Volley.newRequestQueue(this).add(req)
    }
}
