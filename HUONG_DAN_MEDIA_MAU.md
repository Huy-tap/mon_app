# Thêm ảnh/video mẫu cho bài tập

## 1. Bỏ file vào đâu?

Trong project này:

```text
C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/assets/
├── fitness_app.db
└── media/
    ├── images/
    │   └── pushup.png
    └── videos/
        └── pushup.mp4
```

Ảnh và video được đóng gói trong APK. Sau khi cài, máy ảo hoặc điện thoại Android đều có thể dùng media mẫu mà không cần Internet hay truy cập ổ C: của máy tính.

## 2. Điền vào bảng nào, cột nào?

Mở file database **trong assets của project**:

`C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/assets/fitness_app.db`

Trong bảng `exercises`, sửa hai cột của đúng bài:

| exercise_id | name | instruction_image | instruction_video |
|---:|---|---|---|
| 1 | Push Up | media/images/pushup.png | media/videos/pushup.mp4 |

Hai giá trị trên đã được thiết lập. Không cần đổi tên bài thành tên file.

Ví dụ sau này bạn có file Squat thật, hãy điền ở bài Squat (mã 4):

| Cột | Giá trị ví dụ |
|---|---|
| instruction_image | media/images/squat.jpg |
| instruction_video | media/videos/squat.mp4 |

- Tên file, phần mở rộng và chữ hoa/thường phải khớp file thật.
- Đường dẫn bắt đầu bằng `media/images/` hoặc `media/videos/`, dùng dấu `/`.
- Không ghi `C:/...`, không thêm `assets/` phía trước, không dùng `/uploads/...`.
- Chưa có ảnh hoặc video thì để `NULL` ở cột tương ứng.
- Nên dùng ảnh PNG/JPEG và video MP4 có định dạng mà Android phát được.
- Giữ ổn định `exercise_id`. Code dùng mã bài để gắn media, nên đổi tên bài không làm gắn nhầm.

## 3. Sau khi thêm file và sửa database

1. Lưu thay đổi database trong assets.
2. Build lại app và cài bản mới lên thiết bị (Run từ Android Studio hoặc cài APK mới).
3. Mở lại ứng dụng, vào **Bài tập → Push Up** để xem ảnh/video.

Chỉ sửa file trên máy tính mà chưa build/cài lại thì APK trên thiết bị chưa có dữ liệu mẫu mới.

Database Inspector khi app đang chạy hiển thị **database trên thiết bị**. Nó không sửa file database mẫu trong project. Muốn đóng gói cho các máy khác, phải thay file mẫu trong assets.

## 4. App tự làm những gì?

```text
FitnessDatabase.openInternal()
  → Mở database trên thiết bị, migrate cấu trúc
  → BundledExerciseMedia.install(context, database)
      → Đọc exercises từ database đóng gói trong APK
      → Lấy exercise_id và hai cột đường dẫn media mẫu
      → Đối chiếu bài có cùng mã trong database đang dùng
      → Sao chép file vào Pictures/bundled hoặc Movies/bundled của app
      → Ghi đường dẫn tuyệt đối trên thiết bị vào trường ảnh/video
  → Màn hình Bài tập đọc database và hiển thị như bình thường
```

Code dùng chung nằm ở:

[BundledExerciseMedia.kt](C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/data/BundledExerciseMedia.kt)

Điểm gọi nằm ở:

[FitnessDatabase.kt](C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/data/FitnessDatabase.kt)

Tên bài, số hiệp/lần, các buổi tập và lịch sử không bị thay bằng dữ liệu mẫu khi cập nhật media.

## 5. Cài mới và cập nhật khác nhau thế nào?

| Trạng thái trên thiết bị | Cách xử lý |
|---|---|
| Cài mới | Sao chép database ban đầu và cài media mẫu |
| App cũ còn đường dẫn mẫu `/uploads/images/pushup.png` | Thay đường dẫn mẫu cũ bằng file thật trên thiết bị |
| Trường media đang trống và chưa từng được xét cài mẫu | Điền media mẫu tương ứng nếu file có sẵn |
| Đã tự chọn ảnh/video | Giữ nguyên, kể cả khi file người dùng hiện không truy cập được |
| Người dùng thay hoặc xóa media sau khi mẫu được cài/xét | Không tự gắn lại khi khởi động hoặc cập nhật APK |
| Vẫn đang dùng media mẫu, cài APK mới | Cập nhật mẫu theo file/đường dẫn trong APK mới |
| File mẫu đã cài bị mất nhưng đường dẫn vẫn còn | Thử sao chép lại mẫu |
| Bài đã ẩn/xóa khỏi danh mục | Không khôi phục bài, không gắn media cho bài đó |
| File mẫu sai đường dẫn, mất hoặc rỗng | Bỏ qua trường đó, ghi thông tin vào Logcat; app vẫn mở được và lần sau có thể thử lại |

App ghi các khóa `bundled_media:<mã bài>:<cột>` trong `app_state` để phân biệt media mẫu với lựa chọn của người dùng. Không xóa các khóa này khi sử dụng bình thường.

Ở lần nâng cấp đầu tiên có tính năng này, code không biết trường trống từ đầu hay đã được người dùng xóa trước đó; trường trống chưa có dấu ghi nhận sẽ nhận mẫu. Từ lần xét đầu tiên trở đi, thao tác thay/xóa của người dùng được giữ.

**Phạm vi cập nhật:** tự bổ sung media cho bài đã có cùng `exercise_id` trong database của thiết bị. Thêm một bài hoàn toàn mới vào database mẫu không tự tạo bài đó trên các máy đã cài từ trước; đó là một loại cập nhật dữ liệu khác.

Không cần gỡ app hoặc xóa dữ liệu để thử bản cập nhật.

## 6. Kết quả kiểm tra bản hiện tại

- Build debug thành công; unit test đạt.
- 6 kiểm thử media mẫu và 8 kiểm thử lưu trữ đạt: cài mới, bổ sung cho database cũ, giữ lựa chọn người dùng, giữ thao tác xóa, khôi phục file mẫu bị mất, ánh xạ theo mã bài và xử lý đường dẫn lỗi.
- Trên máy ảo đã có dữ liệu: Push Up tự có ảnh thu nhỏ, ảnh chi tiết và video 10 giây; đã mở phát video thực tế.
- Kiểm tra database mẫu trước/sau: chỉ đổi hai đường dẫn ảnh/video của Push Up; dữ liệu buổi tập, hiệp, lịch sử và nhắc nhở không đổi.
