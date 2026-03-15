<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

$userId = $_GET["user_id"] ?? "";
if (empty($userId)) {
    echo json_encode(["status"=>"error","message"=>"Missing user_id"]);
    exit();
}

$stmt = $conn->prepare("SELECT * FROM users WHERE user_id=? LIMIT 1");
$stmt->bind_param("s", $userId);
$stmt->execute();
$result = $stmt->get_result();
if ($user = $result->fetch_assoc()) {
    unset($user["password"]);
    echo json_encode(["status"=>"success","user"=>$user]);
} else {
    echo json_encode(["status"=>"error","message"=>"User not found"]);
}
$stmt->close();
$conn->close();
?>
