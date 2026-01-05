# Library Manager

Hệ thống quản lý thư viện được xây dựng bằng Java với JavaFX, sử dụng MongoDB để lưu trữ dữ liệu và Firebase Authentication để quản lý người dùng.

## Mục lục

- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt](#cài-đặt)
- [Cấu hình](#cấu-hình)
- [Chạy ứng dụng](#chạy-ứng-dụng)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Tính năng](#tính-năng)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)

## Yêu cầu hệ thống

- Java Development Kit (JDK) 17 hoặc cao hơn
- Apache Maven 3.6 trở lên
- MongoDB 5.0 trở lên (đã cài đặt và đang chạy)
- Firebase project (để sử dụng Firebase Authentication)
- Kết nối Internet (để kết nối Firebase và tải dữ liệu sách)

## Cài đặt

### 1. Cài đặt Java JDK 17

**Windows:**
- Tải JDK 17 từ [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) hoặc [OpenJDK](https://adoptium.net/)
- Cài đặt và thiết lập biến môi trường JAVA_HOME

**macOS:**
```bash
brew install openjdk@17
```

**Linux:**
```bash
sudo apt-get update
sudo apt-get install openjdk-17-jdk
```

Kiểm tra cài đặt:
```bash
java -version
```

### 2. Cài đặt Apache Maven

**Windows:**
- Tải Maven từ [Apache Maven](https://maven.apache.org/download.cgi)
- Giải nén và thêm thư mục bin vào PATH

**macOS:**
```bash
brew install maven
```

**Linux:**
```bash
sudo apt-get install maven
```

Kiểm tra cài đặt:
```bash
mvn -version
```

### 3. Cài đặt MongoDB

**Windows:**
- Tải MongoDB Community Server từ [MongoDB Download Center](https://www.mongodb.com/try/download/community)
- Cài đặt và chọn "Install MongoDB as a Service"
- MongoDB sẽ tự động chạy trên port 27017

**macOS:**
```bash
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community
```

**Linux:**
```bash
sudo apt-get install -y mongodb-org
sudo systemctl start mongod
sudo systemctl enable mongod
```

Kiểm tra MongoDB đang chạy:
```bash
mongosh --eval "db.version()"
```

### 4. Clone dự án

```bash
git clone <repository-url>
cd library-manager
```

## Cấu hình

### 1. Cấu hình Firebase

#### Bước 1: Tạo Firebase Project

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Nhấn "Add project" và làm theo hướng dẫn
3. Sau khi tạo project, vào phần "Project Settings"

#### Bước 2: Tạo Web App

1. Trong Firebase Console, chọn "Add app" và chọn biểu tượng Web
2. Đặt tên cho app và đăng ký
3. Sao chép các thông tin cấu hình (API Key, Auth Domain, Project ID, v.v.)

#### Bước 3: Kích hoạt Firebase Authentication

1. Trong Firebase Console, vào "Authentication"
2. Chọn tab "Sign-in method"
3. Kích hoạt "Email/Password" provider

#### Bước 4: Tạo Service Account Key

1. Vào "Project Settings" > "Service accounts"
2. Nhấn "Generate new private key"
3. Tải file JSON về
4. Đổi tên file thành `serviceAccountKey.json`
5. Đặt file vào thư mục `src/main/resources/`

**Lưu ý quan trọng:** File `serviceAccountKey.json` chứa thông tin nhạy cảm. Không commit file này lên Git. File này đã được thêm vào `.gitignore`.

### 2. Tạo file .env

Tạo file `.env` ở thư mục gốc của dự án với nội dung sau:

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

**Hướng dẫn lấy thông tin Firebase:**

- `FIREBASE_API_KEY`: Lấy từ Firebase Console > Project Settings > General > Web API Key
- `FIREBASE_AUTH_DOMAIN`: Thường có dạng `your-project.firebaseapp.com`
- `FIREBASE_DATABASE_URL`: Lấy từ Firebase Console > Realtime Database (nếu sử dụng)
- `FIREBASE_PROJECT_ID`: ID của project, hiển thị trong Project Settings
- `FIREBASE_STORAGE_BUCKET`: Lấy từ Firebase Console > Storage
- `FIREBASE_MESSAGING_SENDER_ID`: Lấy từ Project Settings > Cloud Messaging
- `FIREBASE_APP_ID`: Lấy từ Project Settings > General > App ID

**Lưu ý:** File `.env` cũng đã được thêm vào `.gitignore` để bảo mật thông tin.

### 3. Cấu hình MongoDB

Mặc định, ứng dụng kết nối đến MongoDB tại `mongodb://localhost:27017`. Nếu MongoDB của bạn chạy ở địa chỉ khác, cập nhật giá trị `MONGODB_URI` trong file `.env`.

Database sẽ được tạo tự động với tên `libraryManager` khi ứng dụng chạy lần đầu.

## Chạy ứng dụng

### Sử dụng Maven

```bash
# Build project
mvn clean install

# Chạy ứng dụng
mvn javafx:run
```

### Sử dụng Maven Wrapper (khuyến nghị)

Maven Wrapper cho phép chạy ứng dụng mà không cần cài đặt Maven toàn cục.

**Windows:**
```bash
mvnw.cmd clean install
mvnw.cmd javafx:run
```

**macOS/Linux:**
```bash
./mvnw clean install
./mvnw javafx:run
```

### Chạy từ IDE

**IntelliJ IDEA:**
1. Mở dự án trong IntelliJ IDEA
2. Đợi Maven import dependencies
3. Tìm class `MainApplication.java`
4. Nhấn chuột phải và chọn "Run 'MainApplication.main()'"

**Eclipse:**
1. Import dự án như một Maven project
2. Đợi Maven build workspace
3. Nhấn chuột phải vào project > Run As > Java Application
4. Chọn `MainApplication` làm main class

## Kiến trúc hệ thống

### Mô hình MVC (Model-View-Controller)

Ứng dụng được xây dựng theo mô hình MVC:

- **Model**: Các class trong package `com.app.librarymanager.models`
  - Định nghĩa cấu trúc dữ liệu
  - Xử lý logic nghiệp vụ cơ bản
  
- **View**: Các file FXML trong `src/main/resources/views`
  - Định nghĩa giao diện người dùng
  - Sử dụng JavaFX FXML
  
- **Controller**: Các class trong package `com.app.librarymanager.controllers`
  - Xử lý logic nghiệp vụ
  - Kết nối Model và View
  - Xử lý sự kiện người dùng

### Cơ sở dữ liệu

**MongoDB Collections:**

1. **users** (quản lý bởi Firebase Authentication)
   - Lưu thông tin người dùng
   - Xác thực và phân quyền

2. **books**
   - Thông tin sách (tiêu đề, tác giả, mô tả, v.v.)
   - Danh mục, đánh giá, hình ảnh

3. **bookCopies**
   - Số lượng bản sao của mỗi cuốn sách
   - Quản lý tồn kho

4. **bookLoan**
   - Thông tin mượn sách
   - Trạng thái: PENDING, AVAILABLE, RETURNED, REJECTED, EXPIRED
   - Loại: ONLINE (đọc online), OFFLINE (mượn vật lý)

5. **bookRating**
   - Đánh giá sách của người dùng
   - Điểm số và nhận xét

6. **categories**
   - Danh mục sách
   - Phân loại theo thể loại

7. **libraryCard**
   - Thẻ thư viện của người dùng
   - Thời hạn, trạng thái, phí đăng ký

8. **transactions**
   - Giao dịch tài chính
   - Loại: SELL_BOOKS, REGISTER_CARD, ADD_BOOK, RETURN_BOOK
   - Theo dõi thu chi

9. **penalty**
   - Phạt người dùng
   - Lý do: trả sách muộn, hỏng, mất
   - Trạng thái thanh toán

10. **returnRecord**
    - Lịch sử trả sách
    - Tình trạng sách khi trả
    - Số tiền phạt (nếu có)

11. **comments**
    - Bình luận về sách
    - Tương tác người dùng

12. **rolePermissions**
    - Phân quyền hệ thống
    - Quản lý vai trò (admin, user)

## Tính năng

### Dành cho người dùng (User)

**Quản lý tài khoản:**
- Đăng ký tài khoản mới
- Đăng nhập/Đăng xuất
- Quên mật khẩu
- Cập nhật thông tin cá nhân
- Xem lịch sử hoạt động

**Quản lý thẻ thư viện:**
- Đăng ký thẻ thư viện
- Gia hạn thẻ
- Xem thông tin thẻ (thời hạn, trạng thái)

**Tìm kiếm và duyệt sách:**
- Tìm kiếm sách theo tên, tác giả, thể loại
- Duyệt sách theo danh mục
- Xem chi tiết sách (mô tả, đánh giá, bình luận)
- Xem sách phổ biến, sách mới

**Mượn sách:**
- Yêu cầu mượn sách (online hoặc offline)
- Xem danh sách sách đang mượn
- Yêu cầu trả sách
- Xem lịch sử mượn sách
- Giới hạn mượn: tối đa 5 cuốn cùng lúc
- Thời hạn mượn: 30 ngày

**Tương tác với sách:**
- Đánh giá sách (rating)
- Viết bình luận
- Thêm sách vào danh sách yêu thích
- Xem danh sách sách yêu thích

### Dành cho quản trị viên (Admin)

**Quản lý sách:**
- Thêm sách mới
- Chỉnh sửa thông tin sách
- Xóa sách
- Quản lý số lượng bản sao
- Upload hình ảnh sách

**Quản lý danh mục:**
- Thêm/Sửa/Xóa danh mục
- Gán sách vào danh mục

**Quản lý người dùng:**
- Xem danh sách người dùng
- Chỉnh sửa thông tin người dùng
- Vô hiệu hóa/Kích hoạt tài khoản
- Phân quyền (admin/user)

**Quản lý thẻ thư viện:**
- Xem danh sách đăng ký thẻ
- Duyệt/Từ chối đăng ký thẻ
- Gia hạn thẻ cho người dùng

**Quản lý mượn/trả sách:**
- Xem danh sách yêu cầu mượn sách
- Duyệt/Từ chối yêu cầu mượn
- Ghi nhận tình trạng sách khi cho mượn
- Xử lý trả sách
- Ghi nhận tình trạng sách khi trả
- Tính phạt (nếu trả muộn/hỏng/mất)
- Xem lịch sử mượn/trả

**Quản lý giao dịch:**
- Xem danh sách giao dịch
- Thêm/Sửa/Xóa giao dịch
- Tìm kiếm giao dịch
- Thống kê thu chi
- Loại giao dịch:
  - SELL_BOOKS: Bán sách
  - REGISTER_CARD: Đăng ký thẻ
  - ADD_BOOK: Mua sách nhập kho
  - RETURN_BOOK: Phạt trả sách

**Quản lý phạt:**
- Xem danh sách phạt
- Tạo phạt thủ công
- Đánh dấu đã thanh toán
- Phạt tự động khi:
  - Trả sách muộn
  - Sách bị hỏng
  - Sách bị mất

**Thống kê và báo cáo:**
- Thống kê số lượng sách
- Thống kê số lượng người dùng
- Thống kê mượn/trả sách
- Sách được mượn nhiều nhất
- Thống kê doanh thu

## Cấu trúc dự án

```
library-manager/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── app/
│   │   │           └── librarymanager/
│   │   │               ├── controllers/          # Controllers xử lý logic
│   │   │               │   ├── AdminDashboardController.java
│   │   │               │   ├── AuthController.java
│   │   │               │   ├── BookController.java
│   │   │               │   ├── BookLoanController.java
│   │   │               │   ├── LibraryCardController.java
│   │   │               │   ├── TransactionController.java
│   │   │               │   ├── UserController.java
│   │   │               │   └── ...
│   │   │               ├── models/                # Data models
│   │   │               │   ├── Book.java
│   │   │               │   ├── BookLoan.java
│   │   │               │   ├── LibraryCard.java
│   │   │               │   ├── Transaction.java
│   │   │               │   ├── Penalty.java
│   │   │               │   ├── ReturnRecord.java
│   │   │               │   ├── User.java
│   │   │               │   └── ...
│   │   │               ├── services/              # Services (Firebase, MongoDB)
│   │   │               │   ├── Firebase.java
│   │   │               │   ├── FirebaseAuthentication.java
│   │   │               │   └── MongoDB.java
│   │   │               ├── utils/                 # Utility classes
│   │   │               │   └── DateUtil.java
│   │   │               ├── interfaces/            # Interfaces
│   │   │               │   └── AuthStateListener.java
│   │   │               └── MainApplication.java   # Entry point
│   │   └── resources/
│   │       ├── views/                             # FXML files
│   │       │   ├── admin/                         # Admin views
│   │       │   │   ├── admin-dashboard.fxml
│   │       │   │   ├── manage-books.fxml
│   │       │   │   ├── manage-users.fxml
│   │       │   │   ├── manage-cards.fxml
│   │       │   │   ├── manage-transactions.fxml
│   │       │   │   └── ...
│   │       │   ├── user/                          # User views
│   │       │   │   ├── home.fxml
│   │       │   │   ├── book-detail.fxml
│   │       │   │   ├── my-loans.fxml
│   │       │   │   ├── my-card.fxml
│   │       │   │   └── ...
│   │       │   ├── auth/                          # Authentication views
│   │       │   │   ├── login.fxml
│   │       │   │   ├── register.fxml
│   │       │   │   └── forgot-password.fxml
│   │       │   └── layout.fxml                    # Main layout
│   │       ├── styles/                            # CSS files
│   │       │   └── styles.css
│   │       ├── images/                            # Images
│   │       └── serviceAccountKey.json             # Firebase service account (không commit)
│   └── test/                                      # Test files
├── .env                                           # Environment variables (không commit)
├── .gitignore
├── pom.xml                                        # Maven configuration
├── mvnw                                           # Maven wrapper (Unix)
├── mvnw.cmd                                       # Maven wrapper (Windows)
└── README.md
```

## Công nghệ sử dụng

### Backend

- **Java 17**: Ngôn ngữ lập trình chính
- **JavaFX 17.0.12**: Framework UI
- **MongoDB 5.2.0**: Cơ sở dữ liệu NoSQL
- **Firebase Admin SDK 9.3.0**: Xác thực người dùng
- **Google Cloud Firestore 3.27.0**: Cloud database (optional)

### Libraries

- **Lombok 1.18.34**: Giảm boilerplate code
- **Ikonli 12.3.1**: Icon library
- **BootstrapFX 0.4.0**: Bootstrap styling cho JavaFX
- **java-dotenv 5.2.2**: Quản lý environment variables
- **Apache Commons Lang3 3.17.0**: Utility functions
- **JUnit 5.10.2**: Unit testing

### Build Tools

- **Apache Maven 3.6+**: Build automation
- **Maven Compiler Plugin 3.13.0**: Java compilation
- **JavaFX Maven Plugin 0.0.8**: JavaFX support

## Quy trình làm việc

### Quy trình mượn sách

1. User tìm kiếm và chọn sách muốn mượn
2. User gửi yêu cầu mượn sách (chọn ONLINE hoặc OFFLINE)
3. Yêu cầu được lưu với trạng thái PENDING
4. Admin xem danh sách yêu cầu mượn
5. Admin kiểm tra:
   - Số lượng sách còn lại (nếu OFFLINE)
   - Số lượng sách user đang mượn (tối đa 5)
   - Trạng thái thẻ thư viện của user
6. Admin duyệt hoặc từ chối:
   - Nếu duyệt: 
     - Trạng thái chuyển thành AVAILABLE
     - Ghi nhận tình trạng sách ban đầu
     - Giảm số lượng bản sao (nếu OFFLINE)
     - Thiết lập ngày mượn và hạn trả (30 ngày)
   - Nếu từ chối:
     - Trạng thái chuyển thành REJECTED
     - Ghi nhận lý do từ chối

### Quy trình trả sách

1. User yêu cầu trả sách (đánh dấu returnRequested = true)
2. Admin xem danh sách yêu cầu trả sách
3. Admin kiểm tra tình trạng sách khi trả
4. Admin xử lý trả sách:
   - Ghi nhận tình trạng sách khi trả
   - So sánh với tình trạng ban đầu
   - Tính toán phạt (nếu có):
     - Trả muộn: phạt theo số ngày
     - Sách hỏng: phạt theo mức độ hư hỏng
     - Sách mất: phạt toàn bộ giá trị sách
   - Tạo ReturnRecord
   - Nếu có phạt:
     - Tạo Penalty record
     - Tạo Transaction với type RETURN_BOOK
   - Tăng số lượng bản sao (nếu OFFLINE)
   - Trạng thái chuyển thành RETURNED

### Quy trình đăng ký thẻ thư viện

1. User gửi yêu cầu đăng ký thẻ
2. User chọn thời hạn (số tháng)
3. Yêu cầu được lưu với trạng thái PENDING
4. Admin xem danh sách yêu cầu đăng ký
5. Admin duyệt hoặc từ chối:
   - Nếu duyệt:
     - Tạo thẻ thư viện với thời hạn tương ứng
     - Tính phí = phí tháng × số tháng
     - Tạo Transaction với type REGISTER_CARD
     - Trạng thái thẻ: ACTIVE
   - Nếu từ chối:
     - Ghi nhận lý do từ chối

## Xử lý lỗi thường gặp

### MongoDB connection failed

**Lỗi:** `com.mongodb.MongoTimeoutException: Timed out after 30000 ms`

**Nguyên nhân:** MongoDB chưa chạy hoặc URI không đúng

**Giải pháp:**
```bash
# Kiểm tra MongoDB đang chạy
mongosh

# Nếu không chạy, khởi động MongoDB
# Windows: MongoDB sẽ tự động chạy như service
# macOS:
brew services start mongodb-community
# Linux:
sudo systemctl start mongod
```

### Firebase initialization failed

**Lỗi:** `FirebaseApp with name [DEFAULT] doesn't exist`

**Nguyên nhân:** File `serviceAccountKey.json` không tồn tại hoặc đường dẫn sai

**Giải pháp:**
1. Kiểm tra file `serviceAccountKey.json` trong `src/main/resources/`
2. Kiểm tra đường dẫn trong file `.env`
3. Tải lại service account key từ Firebase Console

### JavaFX runtime components missing

**Lỗi:** `Error: JavaFX runtime components are missing`

**Nguyên nhân:** JavaFX không được cài đặt đúng

**Giải pháp:**
```bash
# Sử dụng Maven để chạy (Maven sẽ tự động tải JavaFX)
mvn javafx:run

# Hoặc sử dụng Maven wrapper
./mvnw javafx:run
```

### Port 27017 already in use

**Lỗi:** MongoDB không khởi động được vì port đã được sử dụng

**Giải pháp:**
```bash
# Tìm process đang sử dụng port 27017
# Windows:
netstat -ano | findstr :27017
taskkill /PID <PID> /F

# macOS/Linux:
lsof -i :27017
kill -9 <PID>
```

## Bảo mật

### Thông tin nhạy cảm

Các file sau chứa thông tin nhạy cảm và KHÔNG được commit lên Git:
- `.env`: Chứa API keys và cấu hình
- `src/main/resources/serviceAccountKey.json`: Firebase service account key

Các file này đã được thêm vào `.gitignore`.

### Khuyến nghị

1. Không chia sẻ file `.env` và `serviceAccountKey.json`
2. Sử dụng Firebase Security Rules để bảo vệ dữ liệu
3. Định kỳ thay đổi API keys
4. Sử dụng HTTPS khi deploy production
5. Mã hóa dữ liệu nhạy cảm trong database

## Đóng góp

Nếu bạn muốn đóng góp vào dự án:

1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit thay đổi (`git commit -m 'Add some AmazingFeature'`)
4. Push lên branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## Giấy phép

Dự án này được phát triển cho mục đích học tập và nghiên cứu.

## Liên hệ

Nếu có vấn đề hoặc câu hỏi, vui lòng tạo issue trên GitHub repository.

## Changelog

### Version 1.0.0 (2026-01-05)

**Tính năng mới:**
- Thêm loại giao dịch RETURN_BOOK để theo dõi phạt trả sách
- Tự động tạo transaction khi có penalty
- Cải thiện quy trình trả sách với ghi nhận tình trạng chi tiết

**Cải tiến:**
- Tối ưu hóa hiệu suất truy vấn MongoDB
- Cải thiện UI/UX cho admin dashboard
- Thêm validation cho form nhập liệu

**Bug fixes:**
- Sửa lỗi tính toán phí đăng ký thẻ
- Sửa lỗi hiển thị hình ảnh sách
- Sửa lỗi đếm số lượng sách đang mượn