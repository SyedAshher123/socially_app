package com.example.i230657_i230007

data class Notification(
    var id: String? = null,
    var fromUserId: String = "",
    var fromUsername: String = "",
    var fromProfilePicture: String = "",
    var type: String = "follow_request",
    var status: String = "pending",
    var timestamp: Long = 0L
)




