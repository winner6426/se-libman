# Library Manager

Một ứng dụng mô phỏng quản lý thư viện, được xây dựng bằng Java.
## Yêu cầu hệ thống

- Java 17 hoặc cao hơn
- Maven 3.6+
- MongoDB (đã cài đặt và đang chạy)
- Firebase project (để sử dụng Firebase Authentication và Firestore)

## Cài đặt

### 1. Cài đặt MongoDB

Đảm bảo MongoDB đã được cài đặt và đang chạy trên máy của bạn:
- **Windows**: Tải và cài đặt từ [MongoDB Download Center](https://www.mongodb.com/try/download/community)
- **macOS**: `brew install mongodb-community` hoặc tải từ trang web chính thức
- **Linux**: `sudo apt-get install mongodb` hoặc tương đương

Sau khi cài đặt, khởi động MongoDB service:
- **Windows**: MongoDB sẽ tự động chạy như một service
- **macOS/Linux**: `sudo systemctl start mongod` hoặc `brew services start mongodb-community`

### 2. Cấu hình Firebase

1. Tạo một Firebase project tại [Firebase Console](https://console.firebase.google.com/)
2. Tải service account key JSON file và đặt vào `src/main/resources/serviceAccountKey.json`
3. Lấy các thông tin cấu hình từ Firebase Console:
   - API Key
   - Auth Domain
   - Database URL
   - Project ID
   - Storage Bucket
   - Messaging Sender ID
   - App ID

### 3. Tạo file .env

Tạo file `.env` ở thư mục gốc của project với nội dung sau:

```env
# MongoDB Configuration
MONGODB_URI=mongodb://localhost:27017

# Firebase Configuration
FIREBASE_API_KEY=your-firebase-api-key
FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
FIREBASE_DATABASE_URL=https://your-project-default-rtdb.firebaseio.com
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_STORAGE_BUCKET=your-project.appspot.com
FIREBASE_MESSAGING_SENDER_ID=your-messaging-sender-id
FIREBASE_APP_ID=your-app-id
FIREBASE_SERVICE_ACCOUNT_PATH=src/main/resources/serviceAccountKey.json
```

**Lưu ý**: Thay thế các giá trị `your-*` bằng thông tin thực tế từ Firebase project của bạn.

### 4. Chạy ứng dụng

```bash
# Build project
mvn clean install

# Chạy ứng dụng
mvn javafx:run
```

Hoặc sử dụng Maven wrapper:
```bash
# Windows
mvnw.cmd javafx:run

# macOS/Linux
./mvnw javafx:run
```