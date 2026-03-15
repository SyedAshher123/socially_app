<?php
header('Content-Type: application/json');
require 'db_connect.php';

if (!isset($_GET['user_id'])) {
    echo json_encode(["status" => "error", "message" => "Missing user_id"]);
    exit;
}

$user_id = $_GET['user_id'];

// 1️⃣ Get list of following + yourself
$following = [$user_id];
$followQuery = $conn->prepare("SELECT following_id FROM followers WHERE follower_id = ?");
$followQuery->bind_param("s", $user_id);
$followQuery->execute();
$followResult = $followQuery->get_result();

while ($row = $followResult->fetch_assoc()) {
    $following[] = $row['following_id'];
}

// Create a comma-separated string for SQL IN()
$placeholders = implode(',', array_fill(0, count($following), '?'));
$types = str_repeat('s', count($following));

// 2️⃣ Fetch posts + user info + images
$sql = "
SELECT 
    p.post_id, p.user_id, p.caption, p.likes, p.created_at,
    u.username, u.display_name, u.profile_picture_url,
    GROUP_CONCAT(pi.image_base64) AS images
FROM posts p
JOIN users u ON p.user_id = u.user_id
LEFT JOIN post_images pi ON p.post_id = pi.post_id
WHERE p.user_id IN ($placeholders)
GROUP BY p.post_id
ORDER BY p.created_at DESC
";

$stmt = $conn->prepare($sql);
$stmt->bind_param($types, ...$following);
$stmt->execute();
$result = $stmt->get_result();

$posts = [];

while ($row = $result->fetch_assoc()) {
    $imagesArray = $row['images'] ? explode(',', $row['images']) : [];

    $post = [
        "postId" => $row['post_id'],
        "userId" => $row['user_id'],
        "caption" => $row['caption'],
        "likes" => intval($row['likes']),
        "imagesBase64" => $imagesArray,
        "createdAt" => intval($row['created_at'])
    ];

    $user = [
        "userId" => $row['user_id'],
        "username" => $row['username'],
        "displayName" => $row['display_name'],
        "profilePictureUrl" => $row['profile_picture_url']
    ];

    $posts[] = ["post" => $post, "user" => $user];
}

echo json_encode(["status" => "success", "posts" => $posts]);
$conn->close();
?>
