<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

$data = json_decode(file_get_contents("php://input"), true);
$u1 = $data['user1'] ?? '';
$u2 = $data['user2'] ?? '';

if (!$u1 || !$u2) {
    echo json_encode(["status"=>"error","message"=>"Missing user1 or user2"]);
    exit();
}

// order users to keep unique constraint stable
$user1 = ($u1 < $u2) ? $u1 : $u2;
$user2 = ($u1 < $u2) ? $u2 : $u1;

// check existing
$check = $conn->prepare("SELECT chat_id FROM user_chats WHERE user1_id=? AND user2_id=? LIMIT 1");
$check->bind_param("ss", $user1, $user2);
$check->execute();
$check->store_result();

if ($check->num_rows > 0) {
    $check->bind_result($chat_id);
    $check->fetch();
    $check->close();
    echo json_encode(["status"=>"success","chat_id"=>$chat_id,"created"=>false]);
    exit();
}
$check->close();

// create chat
$stmt = $conn->prepare("INSERT INTO user_chats (user1_id, user2_id) VALUES (?, ?)");
$stmt->bind_param("ss", $user1, $user2);
if ($stmt->execute()) {
    $chat_id = $conn->insert_id;
    // optional: insert placeholder message
    $initial = "Tap to start chatting";
    $ts = time();
    $stmtMsg = $conn->prepare("INSERT INTO messages (chat_id, sender_id, message_text, message_type, timestamp) VALUES (?, ?, ?, 'text', ?)");
    $stmtMsg->bind_param("issi", $chat_id, $u1, $initial, $ts);
    $stmtMsg->execute();
    $stmtMsg->close();

    echo json_encode(["status"=>"success","chat_id"=>$chat_id,"created"=>true]);
} else {
    echo json_encode(["status"=>"error","message"=>"Failed to create chat: ".$conn->error]);
}
$stmt->close();
$conn->close();
?>
