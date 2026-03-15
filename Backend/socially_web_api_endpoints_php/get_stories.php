<?php
header('Content-Type: application/json');
require 'db_connect.php';

error_reporting(0);
ini_set('display_errors', 0);

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

include 'db_connect.php';

// Get all users who have stories
$sql = "SELECT u.user_id, u.username, u.profile_picture_url, s.story_id, s.story_image, s.created_at
        FROM stories s
        JOIN users u ON s.user_id = u.user_id
        ORDER BY s.created_at DESC";

$result = $conn->query($sql);

$users = [];

if ($result->num_rows > 0) {
    $tempUsers = [];
    while ($row = $result->fetch_assoc()) {
        $uid = $row['user_id'];
        if (!isset($tempUsers[$uid])) {
            $tempUsers[$uid] = [
                "user_id" => $uid,
                "username" => $row['username'],
                "profile_picture" => $row['profile_picture_url'],
                "stories" => []
            ];
        }
        $tempUsers[$uid]["stories"][] = [
            "story_id" => $row['story_id'],
            "story_image" => $row['story_image'],
            "created_at" => $row['created_at']
        ];
    }

    $users = array_values($tempUsers);
}

echo json_encode(["users" => $users]);
$conn->close();
?>
