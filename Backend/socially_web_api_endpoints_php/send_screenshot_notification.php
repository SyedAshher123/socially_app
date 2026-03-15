<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

$data = json_decode(file_get_contents("php://input"), true);
$sender = $data['sender_id'] ?? '';
$receiver = $data['receiver_id'] ?? '';

if (!$sender || !$receiver) { echo json_encode(["status"=>"error","message"=>"Missing"]); exit(); }

$msg = "took a screenshot of your chat";
$ts = time();
$stmt = $conn->prepare("INSERT INTO notifications (user_id, from_user_id, type, message, created_at, is_read) VALUES (?, ?, 'screenshot', ?, ?, 0)");
$stmt->bind_param("sssi", $receiver, $sender, $msg, $ts);
if ($stmt->execute()) echo json_encode(["status"=>"success","message"=>"Notification created"]);
else echo json_encode(["status"=>"error","message"=>$conn->error]);
$stmt->close();
$conn->close();
?>
