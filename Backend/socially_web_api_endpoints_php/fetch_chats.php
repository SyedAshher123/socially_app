<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

// Get POSTed JSON data
$data = json_decode(file_get_contents("php://input"), true);
$currentUserId = $data['current_user_id'] ?? '';

if (empty($currentUserId)) {
    echo json_encode(["status" => "error", "message" => "Missing current_user_id"]);
    exit();
}

// Get all chats of current user
$stmt = $conn->prepare("
    SELECT uc.chat_id, 
           CASE WHEN uc.user1_id = ? THEN uc.user2_id ELSE uc.user1_id END AS partner_id
    FROM user_chats uc
    WHERE uc.user1_id = ? OR uc.user2_id = ?
");
$stmt->bind_param("sss", $currentUserId, $currentUserId, $currentUserId);
$stmt->execute();
$result = $stmt->get_result();

$chats = [];

while ($row = $result->fetch_assoc()) {
    $partnerId = $row['partner_id'];
    $chatId = $row['chat_id'];

    // Fetch partner info
    $stmtUser = $conn->prepare("SELECT username, display_name, profile_picture_url FROM users WHERE user_id=?");
    $stmtUser->bind_param("s", $partnerId);
    $stmtUser->execute();
    $userRes = $stmtUser->get_result()->fetch_assoc();
    $stmtUser->close();

    // Fetch last message
    $stmtMsg = $conn->prepare("SELECT message_text, message_type, timestamp FROM chat_messages WHERE chat_id=? ORDER BY timestamp DESC LIMIT 1");
    $stmtMsg->bind_param("i", $chatId);
    $stmtMsg->execute();
    $msgRes = $stmtMsg->get_result()->fetch_assoc();
    $stmtMsg->close();

    $lastMessage = isset($msgRes['message_type']) && $msgRes['message_type'] == 'image' ? "Photo" : ($msgRes['message_text'] ?? "Tap to start chatting");
    $lastMessageTime = $msgRes['timestamp'] ?? "";

    $chats[] = [
        "chat_id" => $chatId,
        "user_id" => $partnerId,
        "username" => $userRes['username'] ?? "",
        "display_name" => $userRes['display_name'] ?? "",
        "profile_picture_url" => $userRes['profile_picture_url'] ?? "",
        "last_message" => $lastMessage,
        "last_message_time" => $lastMessageTime
    ];
}

echo json_encode(["status" => "success", "chats" => $chats]);

$stmt->close();
$conn->close();
?>
