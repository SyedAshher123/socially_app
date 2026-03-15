<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

// Get POSTed JSON data
$data = json_decode(file_get_contents("php://input"), true);

$currentUserId = $data['current_user_id'] ?? '';
$selectedUserId = $data['selected_user_id'] ?? '';

if (!$currentUserId || !$selectedUserId) {
    echo json_encode(["status" => "error", "message" => "Missing required fields"]);
    exit();
}

// Ensure consistent order to avoid duplicate chats
$user1 = $currentUserId < $selectedUserId ? $currentUserId : $selectedUserId;
$user2 = $currentUserId < $selectedUserId ? $selectedUserId : $currentUserId;

// Check if chat already exists
$check = $conn->prepare("SELECT chat_id FROM user_chats WHERE user1_id=? AND user2_id=? LIMIT 1");
$check->bind_param("ss", $user1, $user2);
$check->execute();
$check->store_result();

if ($check->num_rows > 0) {
    echo json_encode(["status" => "error", "message" => "Chat already exists"]);
    $check->close();
    exit();
}
$check->close();

// Insert new chat
$stmt = $conn->prepare("INSERT INTO user_chats (user1_id, user2_id) VALUES (?, ?)");
$stmt->bind_param("ss", $user1, $user2);

if ($stmt->execute()) {
    $chat_id = $conn->insert_id; // Use the auto-incremented INT chat_id

    // Insert initial placeholder message
    $initialMessage = "Tap to start chatting";
    $timestamp = time();

    $stmtMsg = $conn->prepare("INSERT INTO chat_messages (chat_id, sender_id, receiver_id, message_text, timestamp) VALUES (?, ?, ?, ?, ?)");
    $stmtMsg->bind_param("isssi", $chat_id, $currentUserId, $selectedUserId, $initialMessage, $timestamp);
    $stmtMsg->execute();
    $stmtMsg->close();

    echo json_encode(["status" => "success", "message" => "Chat added successfully"]);
} else {
    echo json_encode(["status" => "error", "message" => "Failed php to add chat: " . $conn->error]);
}

$stmt->close();
$conn->close();
?>
