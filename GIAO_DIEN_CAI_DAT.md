# Cài đặt và nhắc nhở tập luyện

Ứng dụng dùng một form nhắc nhở chung: `activity_reminder_settings_xml.xml`, giữ bố cục của màn Cài đặt. `SettingsXmlActivity.kt` xử lý form, bảng nhập giờ và trạng thái quyền.

## Các đường vào

- Trang chủ → Nhắc nhở → Chỉnh sửa: mở `SettingsXmlActivity` với `reminder=true`.
- Cài đặt → Thiết lập lịch nhắc: mở cùng form trong Activity hiện có.
- Thống kê → nút nhắc nhở: mở `SettingsXmlActivity` với `reminder=true`.
- Bấm thông báo → `MainActivity` → cùng form nhắc nhở.

Quay lại từ form mở trực tiếp sẽ về Trang chủ hoặc Thống kê. Quay lại từ Cài đặt sẽ về trang Cài đặt. Nếu chưa Lưu thì bản nháp không ghi database.

## Lưu và đặt báo thức

`SettingsXmlActivity` → `ReminderSaveModel.save(draft)` → `FitnessRepository.saveReminder()` → bảng SQLite `reminders` → `ReminderScheduler.schedule()` → AlarmManager → `ReminderReceiver` → Notification.

Lịch hằng ngày hoặc theo tuần được lưu thật. Theo tuần phải chọn ít nhất một ngày; giờ hợp lệ 00–23 và phút 00–59. Tắt nhắc nhở rồi Lưu sẽ hủy báo thức đang chờ. ViewModel giữ thao tác lưu khi Android tạo lại Activity; form và bảng giờ giữ nội dung bản nháp.

Sau khi lưu lịch bật, ứng dụng xin quyền thông báo nếu chưa từng hỏi. Nếu quyền bị chặn, lịch vẫn lưu nhưng không phát thông báo; form hiện cảnh báo và nút mở quyền hệ thống. Khi thiếu quyền báo thức chính xác, liên kết “Cho phép nhắc đúng giờ” mở trang quyền; bộ nhắc vẫn dùng báo thức có thể trễ trong lúc chưa được cấp quyền.

Giao diện sáng/tối vẫn dùng `AppTheme.kt` và `app_state.dark_theme`. Database mẫu, bài tập, media và lịch sử không bị thay thế.

## File chính

- `xmlui/SettingsXmlActivity.kt`: trang Cài đặt và form nhắc nhở chung.
- `xmlui/ReminderSaveModel.kt`: lưu lịch trên luồng nền và giữ thao tác qua việc tạo lại Activity.
- `res/layout/activity_settings_xml.xml`: trang Cài đặt.
- `res/layout/activity_reminder_settings_xml.xml`: form nhắc nhở duy nhất.
- `res/layout/settings_time_picker.xml`: nhập giờ/phút.
- `data/FitnessRepository.kt`, `data/ReminderScheduler.kt`: lưu SQLite, đặt/hủy báo thức và gửi thông báo.

Đã bỏ `ReminderFragment.kt`, `fragment_reminder.xml`, `sheet_time.xml` và lớp `TimeSheet`. Bộ chọn tháng thống kê `MonthSheet` vẫn giữ.

## Kiểm tra

`SettingsXmlTest`, `UnifiedReminderXmlTest` và `ExerciseXmlNavigationTest` kiểm tra các đường vào, quay lại đúng trang, bản nháp chưa lưu, lưu giờ/ngày thật, tạo lại Activity, quyền thông báo, đặt/hủy alarm và theme. Kiểm thử chạy trên máy ảo QA với database riêng; không dùng database đang làm việc của người dùng.
