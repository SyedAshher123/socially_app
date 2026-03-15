<?php
error_reporting(0);
ini_set('display_errors', 0);

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");

include 'db_connect.php';

$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data['user_id']) || !isset($data['caption']) || !isset($data['imagesBase64'])) {
    echo json_encode(["success" => false, "message" => "Missing required fields"]);
    exit;
}

$user_id = $data['user_id'];
$caption = $data['caption'];
$images = $data['imagesBase64'];

if (!is_array($images)) {
    echo json_encode(["success" => false, "message" => "Images should be an array"]);
    exit;
}

$post_id = uniqid("post_", true);
$created_at = time();

$stmt = $conn->prepare("INSERT INTO posts (post_id, user_id, caption, created_at) VALUES (?, ?, ?, ?)");
$stmt->bind_param("sssi", $post_id, $user_id, $caption, $created_at);

if ($stmt->execute()) {
    foreach ($images as $img) {
        $img_stmt = $conn->prepare("INSERT INTO post_images (post_id, image_base64) VALUES (?, ?)");
        $img_stmt->bind_param("ss", $post_id, $img);
        $img_stmt->execute();
        $img_stmt->close();
    }
    echo json_encode(["success" => true, "message" => "Post uploaded successfully"]);
} else {
    echo json_encode(["success" => false, "message" => "Failed to upload post"]);
}

$stmt->close();
$conn->close();
?>
