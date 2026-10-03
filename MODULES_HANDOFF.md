# Bàn giao Thống kê và Nhắc nhở

Ngày kiểm tra: 02/10/2026. Dự án giữ Kotlin, Compose Material 3 và SQLite hiện có.

## Thay đổi chính

- `ui/StatsScreen.kt`: ba chỉ số, biểu đồ bốn khoảng ngày (1–7, 8–14, 15–21, 22–cuối tháng), trạng thái loading/lỗi/trống, thử lại, mở ghi nhận buổi tập và mở nhắc nhở. Sheet tháng/năm có Hủy/Xác nhận và khôi phục lựa chọn.
- `model/MonthlyStats.kt`, `model/WorkoutDraft.kt`, `ui/HomeScreen.kt`: thống nhất `completedExercises`, đếm lượt bài có hiệp và tất cả hiệp hoàn thành. Tháng/năm được so sánh bằng ngày đã lưu; phút lấy từ từng Workout đã làm tròn khi lưu.
- `ui/SettingsScreen.kt`: theme lưu vào SQLite, trạng thái cấu hình/quyền, lịch và lần nhắc tiếp theo; form DAILY/WEEKLY, nhiều ngày, sheet giờ 24 giờ, lỗi nhập/lưu và trạng thái đã lưu. Giữ khả năng đọc/sửa lịch ONCE cũ.
- `model/Reminder.kt`, `model/ReminderRules.kt`: kiểm tra lịch, đọc ngày chữ/số, tính lần tương lai theo múi giờ thiết bị; xử lý qua tuần/tháng/năm và DST.
- `controller/FitnessController.kt`, `data/FitnessDatabase.kt`: cập nhật lịch chính trong transaction, chuẩn hóa giờ/ngày, không tạo trùng từ ID cũ/0. Migration bổ sung `schedule_revision`, có thể chạy lại.
- `data/ReminderScheduler.kt`: RTC_WAKEUP, exact khi được cấp quyền, fallback inexact, hủy alarm cũ, định danh phiên bản và token chống alarm cũ; khôi phục qua boot/thời gian/múi giờ/cập nhật app/cấp quyền. Receiver đọc SQLite mới nhất và hoàn tất `goAsync()` trong `finally`.
- `MainActivity.kt`, `ui/FitnessMainApp.kt`, `AndroidManifest.xml`: tích hợp quyền, lifecycle, thao tác IO, deep link thông báo và trạng thái tải/lưu. “Bắt đầu ngay” mở luồng ghi nhận; “Nhắc lại sau 15p” dùng alarm riêng.
- `ui/FitnessComponents.kt`, `ui/ModuleDesign.kt`, `ui/theme/Theme.kt`, assets SVG, font và notification drawable: các thành phần dùng chung, font cục bộ và system bar theo theme. `RecordWorkoutScreen.kt` chỉ cập nhật cách gọi callback nút cho tương thích.
- `app/build.gradle.kts`: thêm build type `qa` với application ID `com.example.fitnessapp.qa`, dùng làm đích instrumentation riêng.
- Kiểm thử bổ sung/cập nhật trong `MonthlyStatsTest`, `ReminderRulesTest`, `FitnessPersistenceTest`, `FitnessFlowTest`, `ModuleUiTest`, `ReminderDeviceTest`, `NotificationNavigationTest`, `ExampleInstrumentedTest`.

## Database nằm ở đâu

Database mẫu trong repository: `app/src/main/assets/fitness_app.db`.

Database đang dùng trên thiết bị: `context.getDatabasePath("fitness_app.db")`, thường là `/data/user/0/com.example.fitnessapp/databases/fitness_app.db` (tương đương `/data/data/com.example.fitnessapp/databases/fitness_app.db`). Bản QA có package riêng: `/data/user/0/com.example.fitnessapp.qa/databases/fitness_app.db`.

Không đọc/ghi trực tiếp asset khi người dùng thao tác. Asset được sao chép khi chưa có database; migration cập nhật bổ sung database trong sandbox. Đã so sánh byte với Git: asset không thay đổi, SHA-256 `5ccc700925e4d67c6641029f3c85ac343f1dcb61682e1519ad028b14ca53ec68`.

## Kết quả kiểm tra

Lệnh build đã chạy:

```sh
sh gradlew :app:assembleQa :app:assembleQaAndroidTest :app:assembleDebug :app:testQaUnitTest :app:lintDebug :app:lintQa --console=plain
```

- Build thành công; 15 unit test đạt. Lint Debug/QA: không lỗi, 22 cảnh báo mỗi variant (dependency, resource chưa dùng, quy ước Compose/KTX).
- Pixel 6 emulator API 37: 22 kiểm thử giao diện/SQLite đạt, gồm các luồng bài tập/ghi nhận/lịch sử cũ, lỗi SQLite thật và retry, lưu/mở lại lịch, migration, lựa chọn tháng/giờ/ngày và khôi phục saved state.
- Bảy ca kiểm tra hệ thống được chạy qua nhiều lượt: alarm thật phát sau khoảng 8 giây; tính lần tiếp theo; snooze độc lập và hủy khi tắt/sửa; broadcast khôi phục không phát ngay; chặn/mở channel; từ chối/cấp POST_NOTIFICATIONS; thiếu quyền exact fallback; thao tác thông báo mở Activity mới/đang chạy. Ca cuối gộp kiểm tra hai nút thông báo. Các ca yêu cầu quyền bị từ chối được chạy riêng với quyền bị thu hồi trước khi khởi động instrumentation.
- Năm ca Compose chạy lại ở khoảng 390dp, 320dp và bố cục ngang 640×320dp; đều đạt. Ảnh nhỏ đã phát hiện số phút bị xuống dòng; đã sửa số/nhãn tự co một dòng và ba thẻ cùng chiều cao, rồi kiểm tra lại. Sheet và form vẫn cuộn được ở chiều ngang.
- Đã chụp thông báo hệ thống Android thật với đầy đủ nội dung và hai action trong `app/build/module-qa/module-qa/notification-preview.png`.
- Log và ảnh nằm trong `app/build/module-qa/`; unit report tại `app/build/reports/tests/testQaUnitTest/`, lint tại `app/build/reports/lint-results-debug.html` và `lint-results-qa.html`.
- Sau kiểm thử đã trả emulator về kích thước vật lý 1080×2400 và force-stop riêng package QA để không để lại alarm thử chạy nền.

Lệnh instrumentation dùng APK QA cài bằng `adb install -r`, không dùng runner gỡ package app chính:

```sh
adb install -r app/build/outputs/apk/qa/app-qa.apk
adb install -r app/build/outputs/apk/androidTest/qa/app-qa-androidTest.apk
adb shell am instrument -w -e class com.example.fitnessapp.ExampleInstrumentedTest,com.example.fitnessapp.FitnessPersistenceTest,com.example.fitnessapp.FitnessFlowTest,com.example.fitnessapp.ModuleUiTest com.example.fitnessapp.qa.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e class com.example.fitnessapp.ReminderDeviceTest,com.example.fitnessapp.NotificationNavigationTest com.example.fitnessapp.qa.test/androidx.test.runner.AndroidJUnitRunner
```

Ca fallback: đặt app-op `SCHEDULE_EXACT_ALARM deny` trước khi chạy, truyền `-e exactDenied true -e class com.example.fitnessapp.ReminderDeviceTest#missingExactAccessUsesFallback`; khôi phục app-op `allow` sau đó. Ca notification: thu hồi POST_NOTIFICATIONS trước khi chạy, truyền `-e notificationDenied true -e class com.example.fitnessapp.ReminderDeviceTest#deniedNotificationPermissionRetainsScheduleAndGrantRestoresAlarm`; ca này cấp lại quyền để kiểm tra phục hồi. Hai ca có điều kiện này được skip trong lượt chạy thông thường.

## Thử thông báo

1. Mở Cài đặt → Thiết lập lịch nhắc. Bật switch, cho phép thông báo nếu Android hỏi.
2. Chọn Hằng ngày, đặt giờ sau hiện tại 1–2 phút, bấm Lưu cài đặt và chờ thông báo “Đã lưu cài đặt”.
3. Nếu có “Có thể nhắc trễ”, mở “Cho phép nhắc đúng giờ” để cấp quyền báo thức chính xác, rồi quay lại app.
4. Kiểm tra giờ lần nhắc tiếp theo, về Home của Android và chờ. “Bắt đầu ngay” phải mở ghi nhận; “Nhắc lại sau 15p” đóng thông báo và đặt một alarm riêng, lịch ngày/tuần vẫn giữ nguyên.
5. Để thử lịch tuần, chọn cả ngày hôm nay rồi đặt giờ tương lai gần. Sau khi thử, trả lại lịch mong muốn và lưu.

Thiếu quyền thông báo/chặn app/chặn channel: lịch vẫn lưu nhưng trạng thái “Chưa hoạt động”, alarm được hủy đến khi được phép trở lại. Thiếu exact: dùng `setAndAllowWhileIdle()`, giờ nhận có thể trễ. Doze, giới hạn tần suất alarm và chính sách tiết kiệm pin của nhà sản xuất có thể ảnh hưởng; force-stop cần mở lại app để khôi phục. Chưa xác minh trên máy vật lý/OEM hoặc trong Doze kéo dài. Broadcast phục hồi được kiểm tra bằng intent, chưa thực hiện chu kỳ khởi động lại thiết bị thật. Tài liệu Android: https://developer.android.com/develop/background-work/services/alarms và https://developer.android.com/develop/ui/compose/notifications/notification-permission.

## Figma và giới hạn đối chiếu

Đã đọc design context và screenshot của 13 node yêu cầu trước khi triển khai, chuyển sang Compose và lưu asset/font cục bộ. Status/navigation bar và thông báo là thành phần thật của Android. Số liệu và thời gian Figma chỉ dùng trong fixture kiểm thử; ứng dụng lấy dữ liệu SQLite/thời gian thiết bị.

Đã kiểm tra ảnh chạy thật, chỉnh font, sheet giờ, navigation tối và bố cục nhỏ. Ở lượt tải lại Figma cuối, MCP báo cần xác thực lại; chưa thể kiểm tra thay đổi mới trên Figma hoặc xác nhận pixel-diff cuối với bản thiết kế trực tuyến.

## Sự cố trong quá trình kiểm thử

Lượt `connectedDebugAndroidTest` ban đầu đã để Gradle gỡ package `com.example.fitnessapp` trên emulator sau khi chạy. Không có bản sao dữ liệu trước lượt đó đã được xác minh, nên không thể khẳng định dữ liệu cũ của package trên emulator còn nguyên hoặc đã khôi phục. Kiểm tra chỉ đọc cuối phiên cho thấy package chính đã có trở lại, với `firstInstallTime` và `lastUpdateTime` là 22:45:49 ngày 02/10/2026, cùng database 81.920 byte; điều này không chứng minh dữ liệu cũ đã phục hồi. Asset database trong source không thay đổi. Các lượt kiểm thử tiếp theo chuyển sang package `.qa` riêng và cài đè bằng `adb install -r`; không tiếp tục cài/gỡ package chính. Cần phân biệt sự cố runner này với migration trong ứng dụng, vốn chỉ bổ sung dữ liệu/cột.
