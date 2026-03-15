package com.example.i230657_i230007

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

class story_page : AppCompatActivity() {

    private lateinit var storyUserPfp: CircleImageView
    private lateinit var storyUsername: TextView
    private lateinit var storyMainImage: ImageView
    private lateinit var topBar: LinearLayout
    private lateinit var progressActiveView: View
    private lateinit var progressRemainingView: View
    private val handler = Handler(Looper.getMainLooper())
    private var storyDuration = 10_000L // 10 seconds per story

    private var currentStoryIndex = 0
    private var userStoriesList: MutableList<Story> = mutableListOf()
    private var userId: String? = null

    private var isAnimatingStory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.story_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainStoryLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        storyUserPfp = findViewById(R.id.storyUserPfp)
        storyUsername = findViewById(R.id.storyUsername)
        storyMainImage = findViewById(R.id.storyImageFull)
        topBar = findViewById(R.id.topBar)
        progressActiveView = findViewById(R.id.storyProgressActive1)
        progressRemainingView = findViewById(R.id.storyProgressRemaining1)

        val closeButton: ImageView = findViewById(R.id.closeButton)
        val cameraIcon: ImageView = findViewById(R.id.cameraIcon)
        val shareIcon: ImageView = findViewById(R.id.shareIcon)


        userId = intent.getStringExtra("user_id")
        if (userId != null) {
            fetchUserStories(userId!!)
        } else {
            Toast.makeText(this, userId + " not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        storyUserPfp.setOnClickListener {
            startActivity(Intent(this, celebrity_follow_page::class.java))
        }
        storyUsername.setOnClickListener {
            startActivity(Intent(this, celebrity_follow_page::class.java))
        }
        closeButton.setOnClickListener {
            closeStory()
        }
        cameraIcon.setOnClickListener {
            startActivity(Intent(this, camera_page::class.java))
        }
        shareIcon.setOnClickListener {
            startActivity(Intent(this, all_chats_page::class.java))
        }
    }

    private fun fetchUserStories(userId: String) {
        val url = "http://192.168.0.102/socially_web_api_endpoints_php/get_user_stories.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getString("status") == "success") {
                    val userObject = response.getJSONObject("user")
                    val storiesArray = response.getJSONArray("stories")

                    if (storiesArray.length() > 0) {
                        userStoriesList.clear()
                        for (i in 0 until storiesArray.length()) {
                            val s = storiesArray.getJSONObject(i)
                            userStoriesList.add(
                                Story(
                                    storyId = s.getString("story_id"),
                                    storyImageBase64 = s.getString("story_image"),
                                    createdAt = s.getLong("created_at")
                                )
                            )
                        }

                        val username = userObject.getString("username")
                        val pfpBase64 = userObject.getString("profile_picture")
                        storyUsername.text = username
                        setProfilePicture(pfpBase64)

                        displayStory(userStoriesList[currentStoryIndex])
                    } else {
                        Toast.makeText(this, "No stories found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, response.optString("message", "Failed to load stories"), Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to load story: ${error.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        queue.add(request)
    }



    private fun displayStory(story: Story) {
        if (isAnimatingStory) return
        isAnimatingStory = true

        // Fade-out animation for previous story
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.duration = 300
        fadeOut.fillAfter = true

        // Fade-in animation for next story
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 400
        fadeIn.fillAfter = true

        fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation) {}
            override fun onAnimationRepeat(animation: android.view.animation.Animation) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation) {
                showDecodedImage(story)
                storyMainImage.startAnimation(fadeIn)
                startProgressBarAnimation()
                isAnimatingStory = false
            }
        })

        storyMainImage.startAnimation(fadeOut)

        // Mark story as viewed
        markStoryAsViewed(story.storyId)

        // Fade-in top bar
        topBar.alpha = 0f
        topBar.visibility = View.VISIBLE
        topBar.animate().alpha(1f).setDuration(400).start()
    }

    private fun showDecodedImage(story: Story) {
        if (!story.storyImageBase64.isNullOrEmpty()) {
            try {
                val cleaned = story.storyImageBase64.replace("\n", "").replace("\r", "").trim()
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                storyMainImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                storyMainImage.setImageResource(android.R.color.black)
            }
        } else {
            storyMainImage.setImageResource(android.R.color.black)
        }
    }

    private fun markStoryAsViewed(storyId: String) {

        val currentUserId = intent.getStringExtra("user_id")
        if (currentUserId.isNullOrEmpty() || userId.isNullOrEmpty()) return

        val url = "http://192.168.0.102/socially_web_api_endpoints_php/mark_story_viewed.php"
        val body = JSONObject().apply {
            put("story_id", storyId)
            put("viewer_id", currentUserId)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                val success = response.optBoolean("success", false)
                if (!success) {
                    Toast.makeText(this, "Failed to mark story as viewed", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    private fun setProfilePicture(base64String: String?) {
        if (!base64String.isNullOrEmpty()) {
            try {
                val cleaned = base64String.replace("\n", "").replace("\r", "").trim()
                val bytes = Base64.decode(cleaned, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                storyUserPfp.setImageBitmap(bitmap)
            } catch (e: Exception) {
                storyUserPfp.setImageResource(R.drawable.placeholder_pfp)
            }
        } else {
            storyUserPfp.setImageResource(R.drawable.placeholder_pfp)
        }
    }

    private fun startProgressBarAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = storyDuration
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            val params = progressActiveView.layoutParams as LinearLayout.LayoutParams
            params.weight = progress
            progressActiveView.layoutParams = params

            val remainingParams = progressRemainingView.layoutParams as LinearLayout.LayoutParams
            remainingParams.weight = 1 - progress
            progressRemainingView.layoutParams = remainingParams
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                handler.post {
                    currentStoryIndex++
                    if (currentStoryIndex < userStoriesList.size) {
                        resetProgressBar()
                        displayStory(userStoriesList[currentStoryIndex])
                    } else {
                        closeStory()
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        animator.start()
    }

    private fun resetProgressBar() {
        val params = progressActiveView.layoutParams as LinearLayout.LayoutParams
        params.weight = 0f
        progressActiveView.layoutParams = params

        val remainingParams = progressRemainingView.layoutParams as LinearLayout.LayoutParams
        remainingParams.weight = 1f
        progressRemainingView.layoutParams = remainingParams
    }

    private fun closeStory() {
        startActivity(Intent(this, home_feed::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
