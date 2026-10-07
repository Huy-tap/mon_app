# FitnessApp — Thống kê và Nhắc nhở (XML)

Worktree `/Users/endgame/mon_app_fresh`, nhánh `fresh-start`. Chỉ có hai module, dùng Android Views, Kotlin và ViewBinding. Không dùng Compose để dựng màn hình.

## Chạy ứng dụng

Mở worktree bằng Android Studio, đồng bộ Gradle và chạy `app`. Giữ cấu hình từ `origin/huyphan`: applicationId/namespace `com.example.fitnessapp`, compile/target SDK 37, min SDK 27, Java compatibility 11, AGP 9.3.3, Gradle 9.5.0; daemon toolchain Java 25 theo file nguồn. `local.properties` là cấu hình SDK riêng máy, không đưa vào Git.

Ứng dụng mở **Thống kê** ở tháng hiện tại. Chạm tên tháng để chọn năm/tháng; chỉ **Xác nhận** áp dụng lựa chọn. Chọn **09/2026** để xem dữ liệu asset: 11 buổi, 450 phút, 30 lượt bài xong. Các số này được truy vấn từ database, không hardcode trong giao diện. Nút chuông mở **Nhắc nhở tập luyện**; Back quay lại tháng đã chọn.

Để thử thông báo: bật cấu hình, chọn **Hằng ngày**, đặt giờ 24 giờ trong tương lai gần (ít nhất 2 phút), rồi **Lưu cài đặt**. Cho phép thông báo và quyền báo thức chính xác nếu muốn thử đúng giờ. Kiểm tra dòng lịch đã lưu/lần nhắc tiếp theo; đưa app xuống nền và đợi. Chạm thông báo mở màn hình nhắc nhở. Với **Theo tuần**, chọn ít nhất một ngày T2–CN rồi lưu. Tắt switch và lưu để hủy alarm.

Thiếu quyền thông báo không làm mất lịch đã lưu. Màn hình hiển thị “Chưa hoạt động” và đường dẫn đến cài đặt hệ thống. Thiếu quyền báo thức chính xác sử dụng `setAndAllowWhileIdle()` nên có thể đến trễ. Android/OEM, chế độ tiết kiệm pin và Doze có thể ảnh hưởng thời gian nhận; force-stop ứng dụng ngăn báo thức cho đến khi mở lại. Không có nút snooze hoặc mở ghi nhận buổi tập.

## Dữ liệu và tích hợp

`app/src/main/assets/fitness_app.db` được lấy nguyên byte từ `origin/huyphan`, SHA-256:

```
5ccc700925e4d67c6641029f3c85ac343f1dcb61682e1519ad028b14ca53ec68
```

`FitnessDatabase` giữ cơ chế copy asset lần đầu, foreign key và migration bổ sung có thể chạy lại của nguồn. Không ghi đè database đã cài. Các bảng ngoài phạm vi được giữ để tích hợp sau này.

`FitnessRepository.monthStats()` đếm các bản ghi buổi trong tháng, tổng phút từ bảng workouts riêng, và các lượt workout_exercises có ít nhất một hiệp, mọi hiệp completed=1. Không đếm hiệp hoặc bài duy nhất trong danh mục. Thống kê chỉ đọc, nạp lại khi quay lại màn hình.

Lịch chính là bản ghi reminders có reminder_id nhỏ nhất. Transaction lưu đọc lại khóa chính để tránh tạo trùng; giữ hai lịch nguồn còn lại. Giờ lưu HH:mm:ss; ngày lưu 1–7 (T2–CN), đọc được MON/WED/FRI. Lịch ONCE cũ vẫn đọc/lưu nguyên loại trừ khi người dùng chủ động chọn DAILY/WEEKLY.

Alarm dùng RTC_WAKEUP, múi giờ thiết bị, PendingIntent immutable ổn định; đặt lần gần nhất trong tương lai và đặt lại sau khi nhận. Receiver kiểm tra cấu hình và định danh lần đặt để bỏ alarm cũ; khôi phục sau boot, thay đổi giờ/múi giờ, cập nhật ứng dụng và cấp quyền exact alarm. Broadcast khôi phục không phát notification. Xử lý database trên executor/background; `goAsync()` luôn finish trong finally.

## Source chính

- `app/src/main/java/com/example/fitnessapp/MainActivity.kt`: launcher XML, insets hệ thống, điều hướng hai module.
- `ui/StatisticsFragment.kt`, `ui/ReminderFragment.kt`, `ui/PickerSheets.kt`: ViewBinding, form, trạng thái và bottom sheets.
- `data/FitnessDatabase.kt`, `data/FitnessRepository.kt`: mở/migrate database và các truy vấn hai module.
- `data/ReminderScheduler.kt`, `model/ReminderRules.kt`, `model/Reminder.kt`: báo thức, quyền, notification và quy tắc lịch.
- `res/layout/activity_main.xml`, `fragment_statistics.xml`, `fragment_reminder.xml`, `sheet_month.xml`, `sheet_time.xml`: toàn bộ giao diện.
- `res/values`, `res/drawable`, `res/raw`, `res/font`: style, màu, icon Figma lưu cục bộ và font; giấy phép font trong `docs/licenses`.
- `app/src/test`: quy tắc lịch và SQLite tạm với Robolectric.
- `app/src/androidTest`: Espresso/ActivityScenario, runner dùng database `instrumentation_fitness.db` riêng.

## Kiểm tra

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
./gradlew connectedDebugAndroidTest
```

Instrumentation nên chạy trên emulator riêng với quyền POST_NOTIFICATIONS chưa cấp để kiểm tra trạng thái bị chặn. Không xóa database người dùng; runner dùng bản sao riêng. Kết quả chạy thực tế và ảnh đối chiếu được ghi trong `docs/qa/RESULTS.md`.

Thiết kế: https://www.figma.com/design/kjkcenI9nvQYV8xEku9osc?node-id=288-3336 . Đã đọc design context/screenshot của toàn bộ 13 node yêu cầu. Các tab chức năng ngoài phạm vi, biểu đồ tùy chọn và thanh hệ thống mô phỏng không được đưa vào ứng dụng.

Tài liệu Android: https://developer.android.com/develop/background-work/services/alarms và https://developer.android.com/develop/ui/views/notifications/notification-permission .
