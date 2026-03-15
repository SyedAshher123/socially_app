Socially – Instagram Clone

Socially is a fully-featured social media application inspired by Instagram. It allows users to create accounts, post images and stories, follow friends, chat in real-time, make calls, and explore content in a feed-driven interface. The app is designed to provide a complete social networking experience with modern mobile UI and real-time interaction features.

Features

Socially includes a comprehensive set of features for users to interact with the platform and with each other. The authentication system allows users to sign up, log in, and reset their password securely. Each user has a profile where they can update their profile picture, edit their bio, and view their posts. Users can follow or unfollow others, create posts containing images or videos, upload stories that are visible for 24 hours, and interact with content through likes, comments, and shares.

The app includes a feed page that displays posts from users that a person follows, as well as a search functionality to find new users. Notifications are provided for likes, comments, follows, and messages to ensure users stay updated with activity related to their account.

Social interactions are enhanced through a real-time chat system, which supports one-to-one messaging along with media sharing. Additionally, the app supports voice and video calls, allowing users to connect instantly. Friend requests are managed through accept and decline options, and the platform includes settings to control privacy and notifications.

Screens / Pages

The application consists of several main screens to provide a complete social networking experience. The authentication pages include login, sign-up, and password recovery functionality. The home or feed page displays posts from followed users, allows users to like, comment, or share content, and includes a story carousel at the top.

The profile page displays user information, posts, and provides options to follow or unfollow other users, as well as edit profile details. The search page allows users to discover new users and explore additional posts. The chat page enables one-to-one messaging with the option to share media, while the call functionality supports both voice and video calls. Finally, the notifications page aggregates all interactions such as likes, comments, follows, and messages in one place.

Tech Stack

The frontend of the application is developed using Android with Java/Kotlin, utilizing XML layouts to create a responsive and intuitive interface. The backend is implemented using PHP scripts hosted on XAMPP, with Apache serving the files and MySQL managing all the database operations. MySQL stores user information, posts, messages, stories, and other relevant data required for the application to function.

Firebase is used for real-time messaging and push notifications, while Git and GitHub are employed for version control and collaboration. This combination provides a robust environment for both development and deployment.

Installation and Setup

To set up the Socially application, first clone the repository using:

git clone https://github.com/YourUsername/Socially.git
cd Socially

For the backend setup, install XAMPP and start both the Apache and MySQL services. Copy the PHP backend files into the htdocs folder of XAMPP, for example C:\xampp\htdocs\Socially.

Next, set up the database by opening phpMyAdmin in XAMPP. Create a new database, for example socially, and import the provided SQL file (socially.sql) to set up all required tables and initial data.

Open the Android project in Android Studio, sync Gradle, and run the app on your emulator or device. Ensure that the backend URL in your API service files points to the XAMPP server, such as:

http://localhost/Socially/socially.php
Usage

Once the application is set up, users can sign up with a unique username and email. They can create posts, upload stories, and interact with friends. The feed displays posts from followed users, allowing users to like, comment, and share content. The chat functionality enables messaging and voice/video calls. Users can manage their profile and privacy settings from the profile page, and receive notifications for all relevant activity.

Contribution

Contributions to the Socially project are welcome. To contribute, fork the repository and create a new branch for your feature using git checkout -b feature/YourFeature. Commit your changes using git commit -m "Add feature description" and push to your branch with git push origin feature/YourFeature. Finally, open a pull request to have your changes reviewed and merged.

License
This project is open-source and free to use for learning and educational purposes.
