<?php
header("Content-Type: application/json; charset=UTF-8");
include("db_connect.php");

$data = json_decode(file_get_contents("php://input"), true);

if (!$data) {
    echo json_encode(["status" => "error", "message" => "Invalid JSON input"]);
    exit();
}

$userId = uniqid("usr_", true);  // generate unique userId
$username = trim($data["username"]);
$email = trim($data["email"]);
$password = $data["password"];
$firstName = trim($data["first_name"]);
$lastName = trim($data["last_name"]);
$dob = trim($data["date_of_birth"]);
$phone = $data["phone_number"] ?? "";
$bio = $data["bio"] ?? "";
$website = $data["website"] ?? "";
$gender = $data["gender"] ?? "other";
$profilePic = $data["profile_picture_url"] ?? "";
$accountPrivate = isset($data["account_private"]) ? (bool)$data["account_private"] : false;

if (empty($username) || empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Missing required fields"]);
    exit();
}

// check if username/email already exists
$check = $conn->prepare("SELECT id FROM users WHERE username=? OR email=? LIMIT 1");
$check->bind_param("ss", $username, $email);
$check->execute();
$check->store_result();

if ($check->num_rows > 0) {
    echo json_encode(["status" => "error", "message" => "Username or email already exists"]);
    $check->close();
    exit();
}
$check->close();

// hash password
$hashedPassword = password_hash($password, PASSWORD_BCRYPT);

// insert user
$stmt = $conn->prepare("
    INSERT INTO users (
        user_id, username, email, password, display_name, first_name, last_name,
        date_of_birth, phone_number, bio, profile_picture_url, gender, website,
        account_private, is_online, created_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, NOW())
");
$displayName = "$firstName $lastName";
$stmt->bind_param(
    "sssssssssssssb",
    $userId, $username, $email, $hashedPassword, $displayName, $firstName,
    $lastName, $dob, $phone, $bio, $profilePic, $gender, $website, $accountPrivate
);

if ($stmt->execute()) {
    // create stats entry
    $stats = $conn->prepare("INSERT INTO user_stats (user_id, post_count, follower_count, following_count) VALUES (?, 0, 0, 0)");
    $stats->bind_param("s", $userId);
    $stats->execute();

    // return full user object for Android app
    echo json_encode([
        "status" => "success",
        "message" => "User registered successfully",
        "user" => [
            "user_id" => $userId,
            "username" => $username,
            "email" => $email
        ]
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Registration failed: " . $conn->error]);
}


$stmt->close();
$conn->close();
?>
