# 🧠 Smart Reminder System (JavaFX + AI)

A cross-language **AI-powered Smart Reminder & Task Manager** built
using **JavaFX, JDBC (MySQL), and Python**.\
This project allows users to:\
- ✅ Add and manage tasks with time/date\
- ✅ Store tasks in a MySQL database\
- ✅ Ask AI for **schedule improvements & suggestions**\
- ✅ Detect **task conflicts** (overlapping tasks)\
- ✅ Provide **intelligent lifestyle suggestions** (like minimizing
gaming hours, avoiding overwork, etc.)

------------------------------------------------------------------------

## 📌 Features

-   **JavaFX GUI** -- modern interface for task management\
-   **MySQL Database** -- persistent storage for tasks\
-   **Python AI Module** -- improves schedules & provides suggestions\
-   **Conflict Detection** -- warns if tasks overlap\
-   **Rule-based AI** -- simple NLP parser for natural queries

------------------------------------------------------------------------

## 🛠️ Tech Stack

-   **Frontend:** JavaFX\
-   **Backend:** Java + JDBC + MySQL\
-   **AI & Suggestions:** Python\
-   **Database:** MySQL

------------------------------------------------------------------------


------------------------------------------------------------------------

## 🛠️ Add Personal Telegram Bot

-   Create a personal telegram bot from BotFather name as it pleases you
-   Get Bot Token and Chat_ID of your Telegram bot
  
-   STEPS FOR CREATING BOT :
      1.Open Telegram and search for BotFather.
      2.Send command: /newbot.
      3.Give it a name + username.
      4.BotFather will give you a token
    
-   STEPS FOR CHAT ID :
    1. Start a chat with your new bot (/start).
    2. Open this URL in browser (replace TOKEN): https://api.telegram.org/bot<TOKEN>/getUpdates
  
------------------------------------------------------------------------

## 🚀 Installation & Setup

### 1️⃣ Clone the repo

``` bash
git clone https://github.com/YOUR_USERNAME/Smart-Reminder-System.git
cd Smart-Reminder-System
```

### 2️⃣ Setup MySQL Database

``` sql
CREATE DATABASE smart_reminder;
USE smart_reminder;

CREATE TABLE tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(255),
    start_time DATETIME,
    end_time DATETIME
);
```

### 3️⃣ Configure Database in Java

In `TaskApp.java` update your DB credentials:

``` java
String url = "jdbc:mysql://localhost:3306/smart_reminder";
String user = "root";
String password = "your_password";
```

### 4️⃣ Install Python Dependencies

``` bash
pip install mysql-connector-python
```

### 5️⃣ Run the App

-   Build & run JavaFX app from `SmartReminder.java`\
-   Ensure `bot.py` is in `src/` for AI suggestions

## 🧪 Example

**Add Task:** `Wake up at 7 AM`\
**Add Task:** `Go Running at 7 AM`\
**Ask AI:** `Improve my schedule`\
**AI Response:**

    ⚠ Conflict detected: "Wake Up" overlaps with "Go Running"
    Suggestion: How about shift go running to 7.30 AM

------------------------------------------------------------------------

## 🔮 Future Improvements

-   Voice-based reminders\
-   Notifications system\
-   Google Calendar API integration

------------------------------------------------------------------------

## 🤝 Contributing

Feel free to fork this project and submit PRs for new features.

------------------------------------------------------------------------

## 📜 License

MIT License © 2025
