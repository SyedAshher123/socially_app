package com.example.i230657_i230007

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

object VolleySingleton {
    private var instance: VolleySingleton? = null
    private lateinit var requestQueue: RequestQueue

    fun getInstance(context: Context): VolleySingleton {
        if (instance == null) {
            instance = VolleySingleton
            requestQueue = Volley.newRequestQueue(context.applicationContext)
        }
        return instance!!
    }

    fun addToRequestQueue(req: Request<*>) {
        requestQueue.add(req)
    }
}
