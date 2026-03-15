package com.example.i230657_i230007

data class Post(
    val postId: String,
    val userId: String,
    val caption: String,
    var likes: Int,
    val imagesBase64: List<String>,
    val createdAt: Long,
    var likedBy: MutableMap<String, Boolean> = mutableMapOf()
)



