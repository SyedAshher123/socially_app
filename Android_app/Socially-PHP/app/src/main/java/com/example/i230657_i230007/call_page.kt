package com.example.i230657_i230007
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import de.hdodenhof.circleimageview.CircleImageView
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine


class call_page : AppCompatActivity() {

    lateinit var mMuteBtn: ImageView
    lateinit var EndCallBtn: ImageView

    lateinit var statusText: TextView

    lateinit var displayName: TextView

    lateinit var profileImage: CircleImageView



    private var mMuted = false

    private val APP_ID = "c73657650c704870b5b75abb8e831e37"

    private val CHANNEL = "myChannel"

    private val TOKEN = ""

    private var mRtcEngine: RtcEngine? = null

    lateinit var speakerBtn: ImageView
    private var isSpeakerOn = false


    private val mRtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                statusText.text = "Joined channel: $channel, uid: $uid"
                Log.d("AgoraCall", "✅ Joined channel: $channel, uid: $uid")
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                statusText.text = "User joined: $uid"
                Log.d("AgoraCall", "👤 Remote user joined: $uid")
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                statusText.text = "User left: $uid"
                Log.d("AgoraCall", "❌ Remote user left: $uid, reason: $reason")
                // End call after short delay
                statusText.postDelayed({ onCallEnded(null) }, 1500)
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            runOnUiThread {
                statusText.text = "Left channel"
                Log.d("AgoraCall", "📴 Left channel")
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                statusText.text = "Error: $err"
                Log.e("AgoraCall", "⚠️ Agora error: $err")
            }
        }

        override fun onConnectionLost() {
            runOnUiThread {
                statusText.text = "Connection lost"
                Log.e("AgoraCall", "⚠️ Connection lost")
            }
        }

        override fun onConnectionInterrupted() {
            runOnUiThread {
                statusText.text = "Connection interrupted"
                Log.e("AgoraCall", "⚠️ Connection interrupted")
            }
        }
    }


    private fun initializeAndJoinChannel() {
        try {
            mRtcEngine = RtcEngine.create(baseContext, APP_ID, mRtcEventHandler)
        } catch (e: Exception) {
            println("Exception: " + e.message)
        }

        // ✅ Important setup
        mRtcEngine?.setChannelProfile(io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION)
        mRtcEngine?.enableAudio()

        // Show calling status immediately when starting
        statusText.text = "Calling..."

        val uid = (1000..9999).random() // random unique UID per device
        mRtcEngine!!.joinChannel(TOKEN, CHANNEL, "", uid)
    }



    private val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    private val PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(permission),
                requestCode)
            return false
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_page)


        mMuteBtn = findViewById<ImageView>(R.id.mute_btn)

        EndCallBtn = findViewById<ImageView>(R.id.end_call_btn)

        statusText = findViewById<TextView>(R.id.status_text)

        displayName = findViewById<TextView>(R.id.placeholder_name)

        profileImage = findViewById<CircleImageView>(R.id.pfp)

        EndCallBtn.setOnClickListener {
            onCallEnded(it)
        }
        mMuteBtn.setOnClickListener {
            onLocalAudioMuteClicked(it)
        }
        // Get userId from intent
        val userId = intent.getStringExtra("userId")
        if (userId != null) {
            fetchUserProfile(userId)
        }

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)){
            initializeAndJoinChannel()
        }

        speakerBtn = findViewById(R.id.speaker_btn)

        speakerBtn.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            mRtcEngine?.setEnableSpeakerphone(isSpeakerOn)
        }


    }

    //onDestroy
    override fun onDestroy() {
        super.onDestroy()
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null

    }

    fun onCallEnded(view: View?){
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
        finish()
    }

    fun onLocalAudioMuteClicked(view: View) {
        mMuted = !mMuted
        mRtcEngine?.muteLocalAudioStream(mMuted)

        val res = if (mMuted) R.drawable.mic_muted else R.drawable.mic_icon
        mMuteBtn.setImageResource(res)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID_RECORD_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeAndJoinChannel()
        }
    }


    private fun fetchUserProfile(userId: String) {
        val url = "http:///192.168.0.102/socially_web_api_endpoints_php/get_user_profile.php?user_id=$userId"

        val requestQueue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {

                    val userObj = response.getJSONObject("user")
                    val firstName = userObj.optString("first_name", "")
                    val lastName = userObj.optString("last_name", "")
                    val fullName = "$firstName $lastName"
                    val profileBase64 = userObj.optString("profile_picture_url", "")

                    // Set display name
                    displayName.text = fullName

                    // Set profile image
                    if (profileBase64.isNotEmpty()) {
                        try {
                            val decodedBytes = Base64.decode(profileBase64, Base64.DEFAULT)
                            val bitmap =
                                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            profileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            profileImage.setImageResource(R.drawable.placeholder_pfp)
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.placeholder_pfp)
                    }
                }
            },
            { error ->
                Log.e("CallPage", "❌ Error fetching user profile: ${error.message}")
            }
        )

        requestQueue.add(request)
    }



}
