<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

$data = json_decode(file_get_contents("php://input"), true);

if (!$data) {
    echo json_encode(["status" => "error", "message" => "Invalid JSON input"]);
    exit();
}

$emailOrUsername = trim($data["email"]);
$password = $data["password"];

if (empty($emailOrUsername) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Missing credentials"]);
    exit();
}

$stmt = $conn->prepare("SELECT * FROM users WHERE email=? OR username=? LIMIT 1");
$stmt->bind_param("ss", $emailOrUsername, $emailOrUsername);
$stmt->execute();
$result = $stmt->get_result();

if ($user = $result->fetch_assoc()) {
    if (password_verify($password, $user["password"])) {
        // Update online status + lastSeen
        $update = $conn->prepare("UPDATE users SET is_online=1, last_seen=NOW() WHERE id=?");
        $update->bind_param("i", $user["id"]);
        $update->execute();

        // remove password before sending back
        unset($user["password"]);

        echo json_encode([
            "status" => "success",
            "message" => "Login successful",
            "user" => $user
        ]);
    } else {
        echo json_encode(["status" => "error", "message" => "Invalid password"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "User not found"]);
}

$stmt->close();
$conn->close();
?>
