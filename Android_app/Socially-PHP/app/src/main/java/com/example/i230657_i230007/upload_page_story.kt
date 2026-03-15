package com.example.i230657_i230007

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class upload_page_story : AppCompatActivity() {

    private lateinit var imagePickerContainer: FrameLayout
    private lateinit var selectedImageView: ImageView
    private lateinit var selectPromptText: TextView
    private lateinit var uploadButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001
    private val CAPTURE_IMAGE_REQUEST = 1002
    private val CAMERA_PERMISSION_CODE = 2001

    private lateinit var currentPhotoPath: String


    private lateinit var postTab: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.upload_page_story)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        imagePickerContainer = findViewById(R.id.imagePickerContainer)
        selectedImageView = findViewById(R.id.iv_selectedImage)
        selectPromptText = findViewById(R.id.tv_selectPrompt)
        uploadButton = findViewById(R.id.btn_upload)
        cancelButton = findViewById(R.id.cancelButton)
        postTab = findViewById(R.id.postTabText)


        imagePickerContainer.setOnClickListener { showImageSourceDialog() }

        uploadButton.setOnClickListener {
            val currentUser = getSharedPreferences("user_session", MODE_PRIVATE).getString("user_id", "")
            if (selectedImageUri != null) {

                if (currentUser.isNullOrEmpty()) {
                    Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                try {
                    val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    val storyImageBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)

                    val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val body = JSONObject().apply {
                        put("user_id", currentUser)
                        put("story_image", storyImageBase64)
                        put("created_at", createdAt)
                    }

                    val url = "http://192.168.0.102/socially_web_api_endpoints_php/upload_story.php"
                    val queue = Volley.newRequestQueue(this)

                    val request = JsonObjectRequest(
                        Request.Method.POST, url, body,
                        { response ->
                            if (response.getBoolean("success")) {
                                Toast.makeText(this, "Post uploaded successfully!", Toast.LENGTH_SHORT)
                                    .show()
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Failed: ${response.getString("message")}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        { error ->
                            Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT)
                                .show()
                        })

                    queue.add(request)

                    request.retryPolicy = DefaultRetryPolicy(
                        10000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    )


                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()


                }



            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }



        cancelButton.setOnClickListener {
            finish()
        }

        postTab.setOnClickListener {
            //go to upload page
            val intent = Intent(this, upload_page::class.java)
            startActivity(intent)
        }
    }


    // ✅ Show dialog to choose between camera and gallery
    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image Source")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkCameraPermission()
                1 -> openImagePicker()
            }
        }
        builder.show()
    }

    // ✅ Check camera permission
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            openCamera()
        }
    }

    // ✅ Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ Open camera to take photo
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            }

            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    photoFile
                )
                selectedImageUri = photoURI
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, CAPTURE_IMAGE_REQUEST)
            }
        }
    }

    // ✅ Create temp image file
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    // ✅ Open gallery picker
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // ✅ Handle image results from both sources
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedImageUri = data?.data
                    selectedImageView.setImageURI(selectedImageUri)
                }

                CAPTURE_IMAGE_REQUEST -> {
                    val file = File(currentPhotoPath)
                    selectedImageUri = Uri.fromFile(file)
                    selectedImageView.setImageURI(selectedImageUri)
                }
            }
            selectedImageView.visibility = ImageView.VISIBLE
            selectPromptText.visibility = TextView.GONE
        }
    }

}
