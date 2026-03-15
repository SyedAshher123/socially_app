<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

$currentUserId = $_GET['user_id'] ?? '';

if (!$currentUserId) {
    echo json_encode(["status" => "error", "message" => "Missing user_id"]);
    exit();
}

// Fetch current user info
$stmt = $conn->prepare("SELECT username, profile_picture_url FROM users WHERE user_id = ?");
$stmt->bind_param("s", $currentUserId);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    echo json_encode(["status" => "success", "user" => $row]);
} else {
    echo json_encode(["status" => "error", "message" => "User not found"]);
}

$stmt->close();
$conn->close();
?>
