<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");
$data = json_decode(file_get_contents("php://input"), true);
$call_key = $data['call_key'] ?? '';
if (!$call_key) { echo json_encode(["status"=>"error","message"=>"Missing"]); exit(); }
$stmt = $conn->prepare("DELETE FROM calls WHERE call_key = ?");
$stmt->bind_param("s", $call_key);
if ($stmt->execute()) echo json_encode(["status"=>"success"]); else echo json_encode(["status"=>"error","message"=>$conn->error]);
$stmt->close();
$conn->close();
?>
