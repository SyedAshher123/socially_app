<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");

include 'db_connect.php';

$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data['post_id']) || !isset($data['user_id']) || !isset($data['action'])) {
    echo json_encode(["success" => false, "message" => "Missing fields"]);
    exit;
}

$post_id = $data['post_id'];
$user_id = $data['user_id'];
$action = $data['action'];

if ($action === "like") {
    $stmt = $conn->prepare("INSERT IGNORE INTO post_likes (post_id, user_id) VALUES (?, ?)");
    $stmt->bind_param("ss", $post_id, $user_id);
    $stmt->execute();
} else {
    $stmt = $conn->prepare("DELETE FROM post_likes WHERE post_id = ? AND user_id = ?");
    $stmt->bind_param("ss", $post_id, $user_id);
    $stmt->execute();
}

// Update total likes
$result = $conn->query("SELECT COUNT(*) as like_count FROM post_likes WHERE post_id='$post_id'");
$row = $result->fetch_assoc();
$like_count = (int)$row['like_count'];
$conn->query("UPDATE posts SET likes=$like_count WHERE post_id='$post_id'");

echo json_encode(["success" => true, "likes" => $like_count]);
?>
