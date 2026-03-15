<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

$currentUserId = $_GET['current_user_id'] ?? "";

if(empty($currentUserId)){
    echo json_encode(["status"=>"error","message"=>"Missing current_user_id"]);
    exit();
}

// Assuming "following" info is stored in a table `followers` (follower_id, following_id)
$stmt = $conn->prepare("
    SELECT u.user_id, u.username, u.display_name, u.profile_picture_url, u.is_online
    FROM users u
    JOIN followers f ON f.following_id = u.user_id
    WHERE f.follower_id = ?
");
$stmt->bind_param("s", $currentUserId);
$stmt->execute();
$result = $stmt->get_result();

$statuses = [];
while($row = $result->fetch_assoc()){
    $statuses[] = $row;
}

echo json_encode(["status"=>"success","users"=>$statuses]);
$stmt->close();
$conn->close();
?>
