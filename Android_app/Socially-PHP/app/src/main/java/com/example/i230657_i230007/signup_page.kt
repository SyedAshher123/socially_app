package com.example.i230657_i230007

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.firebase.messaging.FirebaseMessaging
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Calendar

class signup_page : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var createAccountBtn: MaterialButton
    private lateinit var backBtn: ImageView
    private lateinit var profileImageView: CircleImageView

    private var selectedDate = ""
    private var profileImageBase64: String? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signup_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupMainContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI
        backBtn = findViewById(R.id.signupBackBtn)
        usernameInput = findViewById(R.id.Username_input)
        firstNameInput = findViewById(R.id.FirstName_input)
        lastNameInput = findViewById(R.id.LastName_input)
        dobInput = findViewById(R.id.Dob_input)
        emailInput = findViewById(R.id.Email_input)
        passwordInput = findViewById(R.id.Password_input)
        createAccountBtn = findViewById(R.id.createAccountButton)
        profileImageView = findViewById(R.id.signupProfileImage)

        dobInput.inputType = InputType.TYPE_NULL
        dobInput.isFocusable = false
        dobInput.setOnClickListener { showDatePicker() }

        backBtn.setOnClickListener {
            startActivity(Intent(this, login_page::class.java))
            finish()
        }

        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        createAccountBtn.setOnClickListener {
            validateAndSignup()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                profileImageView.setImageBitmap(bitmap)

                // convert bitmap to Base64
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                profileImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, y, m, d ->
                selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                dobInput.setText(selectedDate)
            },
            year - 18, month, day
        )
        datePicker.show()
    }

    private fun validateAndSignup() {
        val username = usernameInput.text.toString().trim()
        val firstName = firstNameInput.text.toString().trim()
        val lastName = lastNameInput.text.toString().trim()
        val dob = dobInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (username.isEmpty() || username.length < 3) {
            usernameInput.error = "Invalid username"
            return
        }
        if (firstName.isEmpty()) {
            firstNameInput.error = "Enter first name"
            return
        }
        if (lastName.isEmpty()) {
            lastNameInput.error = "Enter last name"
            return
        }
        if (dob.isEmpty()) {
            dobInput.error = "Select DOB"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Invalid email"
            return
        }
        if (password.length < 6) {
            passwordInput.error = "At least 6 characters"
            return
        }

        createAccountBtn.isEnabled = false
        createAccountBtn.text = "Creating..."

        val url = "http://192.168.0.102/socially_web_api_endpoints_php/signup.php"
        val body = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
            put("first_name", firstName)
            put("last_name", lastName)
            put("date_of_birth", dob)
            put("phone_number", "")
            put("bio", "")
            put("website", "")
            put("gender", "other")
            put("profile_picture_url", profileImageBase64 ?: "") // Base64
            put("account_private", false)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, body,
            { response ->
                if (response.getString("status") == "success") {
                    val user = response.optJSONObject("user") ?: JSONObject().apply {
                        put("user_id", response.getString("user_id"))
                        put("username", username)
                        put("email", email)
                    }

                    // Save session
                    val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                    prefs.edit().apply {
                        putBoolean("isLoggedIn", true)
                        putString("user_id", user.getString("user_id"))
                        putString("username", user.getString("username"))
                        putString("email", user.getString("email"))
                        apply()
                    }

                    fetchAndSendFcmToken(user.getString("user_id"))

                    Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, switch_accounts_page::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, response.getString("message"), Toast.LENGTH_LONG).show()
                }
                createAccountBtn.isEnabled = true
                createAccountBtn.text = "Create Account"
            },
            { error ->
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
                createAccountBtn.isEnabled = true
                createAccountBtn.text = "Create Account"
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
}
