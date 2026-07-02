# Hướng dẫn Cài đặt (Setup Guide) - fubabeo.mod

Tài liệu này hướng dẫn chi tiết cách cài đặt Web Server Backend (lên Render) và cách Build (xuất file `.jar`) cho mod Minecraft.

---

## Phần 1: Cài đặt Web Server (Backend & Dashboard) lên Render.com

Hệ thống backend được viết bằng Node.js và yêu cầu một cơ sở dữ liệu PostgreSQL. Bạn sẽ cần tạo database miễn phí trước, sau đó đưa code lên Render.com.

### 1.1 Tạo Cơ sở dữ liệu (Database) miễn phí tại Neon.tech
1. Truy cập **[Neon.tech](https://neon.tech/)** và tạo một tài khoản miễn phí.
2. Tạo một Project mới.
3. Sau khi tạo xong, Neon sẽ hiển thị cho bạn một đường link gọi là **Connection String** (nó bắt đầu bằng `postgresql://...`).
4. Hãy **Copy** đường link này lại, bạn sẽ cần dùng nó ở bước cấu hình Render.

### 1.2 Triển khai máy chủ lên Render.com
1. Đưa toàn bộ thư mục code của bạn (bao gồm thư mục `backend`) lên một kho lưu trữ **GitHub**.
2. Truy cập **[Render.com](https://render.com/)**, đăng nhập bằng GitHub.
3. Tạo một **New Web Service**, chọn kho lưu trữ GitHub chứa code của bạn.
4. Cấu hình Render như sau:
   - **Root Directory**: `backend` *(Rất quan trọng, để báo cho Render biết code web nằm ở thư mục backend)*.
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
5. Cuộn xuống phần **Environment Variables** (Biến môi trường) và thêm 2 biến sau:
   - `DATABASE_URL`: Dán Connection String từ Neon.tech mà bạn đã copy ở bước 1.1 vào đây.
   - `API_KEY`: Điền mã bảo mật để mod giao tiếp với web (Ví dụ: `iufubabeobeo`). Mã này phải giống với mã trong cấu hình của mod.
6. Bấm **Create Web Service**. Đợi vài phút để Render tải code về và khởi chạy.
7. Khi Render báo thành công (trạng thái "Live"), nó sẽ cung cấp cho bạn một đường link web (VD: `https://fubabeo-api.onrender.com`). Đây chính là Web Dashboard của bạn!
   - *Lưu ý: Bạn nên dùng dịch vụ như [UptimeRobot](https://uptimerobot.com) để ping vào đường link web cứ 5 phút 1 lần để web trên Render không bị tắt tự động (do dùng gói miễn phí).*

---

## Phần 2: Cách Build Mod (Xuất file .jar)

Mod được viết cho Fabric 1.21.x bằng Java 21.

### 2.1 Các phần mềm cần tải (Bắt buộc)
Để Build (xuất file jar) và chơi được mod, bạn cần cài đặt 2 thứ sau:

1. **Java Development Kit 21 (JDK 21):**
   - Link tải: [Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21)
   - *Cách cài:* Chọn hệ điều hành của bạn (Windows), tải bản `.msi`, bấm Next liên tục để cài đặt. **Lưu ý:** Ở màn hình cài đặt, nhớ chọn "Set JAVA_HOME environment variable" (nếu có hỏi) để máy tính nhận diện được Java.

2. **Fabric API (Dành cho người chơi):**
   - Link tải: [Fabric API 1.21.x](https://modrinth.com/mod/fabric-api/versions)
   - *Cách dùng:* Đây là mod bắt buộc đi kèm. Khi bạn cài `fubabeo.mod` vào game, bạn cũng **phải bỏ file Fabric API này** vào chung thư mục `mods` của game, nếu không game sẽ bị crash.

### 2.2 Sửa link API của Web Server
Trước khi xuất file mod, bạn phải báo cho mod biết cái Web Server (bạn vừa cài trên Render) nằm ở đường link nào.
1. Mở file này lên: `fubabeo-mod/src/main/java/com/fubabeo/mod/network/ApiConfig.java`
2. Ở dòng số 13, đổi link `https://death-compass-api.onrender.com` thành **đường link Render của bạn**.
*(Mã API_KEY tôi đã đổi sẵn thành `iufubabeobeo` cho bạn rồi).*

### 2.2 Các bước Build
1. Mở Terminal / PowerShell và di chuyển vào thư mục `fubabeo-mod`:
   ```bash
   cd fubabeo-mod
   ```
2. Chạy lệnh build:
   - **Trên Windows**:
     ```bash
     .\gradlew build
     ```
   - **Trên Mac / Linux**:
     ```bash
     ./gradlew build
     ```
3. Gradle sẽ tiến hành tải các công cụ cần thiết và biên dịch mod (Lần đầu chạy sẽ mất vài phút).
4. Khi thấy thông báo **BUILD SUCCESSFUL**, hãy vào thư mục sau để lấy file mod:
   `fubabeo-mod/build/libs/`
5. File có tên dạng `fubabeo-1.0.0.jar` chính là file mod của bạn! Hãy copy nó vào thư mục `mods` của Minecraft Fabric 1.21.

### 2.3 Cách chạy trực tiếp để kiểm tra (Test)
Bạn không cần build ra file jar nếu chỉ muốn test nhanh giao diện mod.
Tại thư mục `fubabeo-mod`, hãy chạy lệnh:
- **Trên Windows**:
  ```bash
  .\gradlew runClient
  ```
Lệnh này sẽ tự động khởi động một phiên bản Minecraft đã cài sẵn mod để bạn vào test trực tiếp.
