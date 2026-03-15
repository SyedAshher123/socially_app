Socially – Instagram Clone

Socially is a fully-featured social media application inspired by Instagram. It allows users to create accounts, post images and stories, follow friends, chat in real-time, make calls, and explore content in a feed-driven interface.

Features
User Features

User authentication (Sign up, Log in, Password reset)

User profiles (Edit profile, profile picture, bio)

Follow and unfollow users

Post images, videos, and stories

Like, comment, and share posts

Explore feed with posts from followed users

Search users by username

Notifications for likes, comments, follows, and messages

Social Interactions

Real-time chat

Voice and video calls

Story viewing (24-hour temporary stories)

Friend requests with accept/decline functionality

Other Features

Push notifications for activity

Profile privacy settings

Responsive design for mobile devices

Real-time updates using backend APIs

Screens / Pages

1. Authentication

Login Page

Sign-Up Page

Forgot Password

2. Home / Feed

Display posts from followed users

Like, comment, and share posts

Story carousel on top

3. Profile Page

User information and posts

Follow / Unfollow button

Edit profile option

4. Search

Search users

Explore new posts

5. Chat

One-to-one messages

Media sharing

Voice and video call options

6. Notifications

Display likes, comments, follows, and messages

Tech Stack

Frontend:

Android (Java/Kotlin)

XML for UI layouts

Backend:

PHP scripts (socially.php and others)

XAMPP for local server environment (Apache + MySQL)

Database:

MySQL for storing users, posts, messages, and stories

Other Tools:

Firebase for real-time messaging and notifications

Git and GitHub for version control

Installation / Setup

Clone the repository

git clone https://github.com/YourUsername/Socially.git
cd Socially

Backend setup with XAMPP

Install XAMPP and start Apache and MySQL

Copy the backend PHP files into the htdocs folder
Example: C:\xampp\htdocs\Socially

Database setup

Open phpMyAdmin in XAMPP

Create a new database (e.g., socially)

Import the provided SQL file (socially.sql) to set up tables and initial data

Android setup

Open the project in Android Studio

Sync Gradle and run the app

Ensure the backend URL in your API service files points to your XAMPP server, for example:

http://localhost/Socially/socially.php
Usage

Sign up with a unique username and email

Create posts, stories, and interact with friends

Explore the feed, like, comment, and share posts

Use chat for messaging and voice/video calls

Manage your profile and settings

Contribution

Contributions are welcome. To contribute:

Fork the repository

Create a new branch (git checkout -b feature/YourFeature)

Commit your changes (git commit -m "Add some feature")

Push to the branch (git push origin feature/YourFeature)

Open a Pull Request

License

This project is open-source and free to use for learning purposes
