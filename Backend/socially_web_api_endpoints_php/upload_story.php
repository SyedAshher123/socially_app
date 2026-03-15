<?php
error_reporting(0);
ini_set('display_errors', 0);

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");

include 'db_connect.php';

// Get JSON input
$data = json_decode(file_get_contents("php://input"), true);

// Validate required fields
if (!isset($data['user_id']) || !isset($data['story_image'])) {
    echo json_encode(["success" => false, "message" => "Missing required fields"]);
    exit;
}

$user_id = $data['user_id'];
$story_image_base64 = $data['story_image'];
$created_at = time();

// Generate unique story ID
$story_id = uniqid("story_", true);

// Insert into database
$stmt = $conn->prepare("INSERT INTO stories (story_id, user_id, story_image, created_at) VALUES (?, ?, ?, ?)");
$stmt->bind_param("sssi", $story_id, $user_id, $story_image_base64, $created_at);

if ($stmt->execute()) {
    echo json_encode(["success" => true, "message" => "Story uploaded successfully"]);
} else {
    echo json_encode(["success" => false, "message" => "Failed to upload story"]);
}

$stmt->close();
$conn->close();
?>
