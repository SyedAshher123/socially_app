<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

$currentUserId = $_GET['current_user_id'] ?? '';

if (!$currentUserId) {
    echo json_encode(["status" => "error", "message" => "Missing current_user_id"]);
    exit();
}

// Fetch all users except current user
$stmt = $conn->prepare("SELECT user_id, username, email, profile_picture_url FROM users WHERE user_id != ?");
$stmt->bind_param("s", $currentUserId);
$stmt->execute();
$result = $stmt->get_result();

$users = [];
while ($row = $result->fetch_assoc()) {
    $users[] = $row;
}

echo json_encode(["status" => "success", "users" => $users]);

$stmt->close();
$conn->close();
?>
