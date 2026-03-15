<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");
$data = json_decode(file_get_contents("php://input"), true);
$caller = $data['caller_id'] ?? '';
$callee = $data['callee_id'] ?? '';
$type = $data['type'] ?? 'voice';
if (!$caller || !$callee) { echo json_encode(["status"=>"error","message"=>"Missing"]); exit(); }
$call_key = $caller . "_" . $callee . ($type=='video' ? "_video" : "");
$now = time();
$stmt = $conn->prepare("INSERT INTO calls (call_key, caller_id, callee_id, call_type, status, started_at, updated_at) VALUES (?, ?, ?, ?, 'calling', ?, ?) ON DUPLICATE KEY UPDATE status='calling', updated_at=?");
$stmt->bind_param("sssiiii", $call_key, $caller, $callee, $type, $now, $now, $now);
if ($stmt->execute()) echo json_encode(["status"=>"success","call_key"=>$call_key]); else echo json_encode(["status"=>"error","message"=>$conn->error]);
$stmt->close();
$conn->close();
?>
