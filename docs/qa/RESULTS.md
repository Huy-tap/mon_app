# Kết quả kiểm tra thực tế

Môi trường: nhánh fresh-start, worktree mon_app_fresh; emulator riêng FitnessFreshQA (Android 17 / API 37, arm64), Gradle 9.5.0, AGP 9.3.3, Java daemon 25. Không dùng worktree mon_app.

- Build debug: PASS; APK `app/build/outputs/apk/debug/app-debug.apk`.
- Unit test: 8/8 PASS (4 ReminderRules, 4 SQLite repository/Robolectric API 34).
- Espresso/ActivityScenario: **4/4 PASS ở lượt cuối**, `espresso-final.xml` và `final-espresso.log`. Chờ cửa sổ bottom sheet mới nhận focus sau recreate; tắt animation hệ thống trên emulator QA theo cách chạy Espresso thông thường. Khi bật animation, test click có thể chạm cửa sổ đang chuyển tiếp; lượt cuối tắt animation hoàn tất trong 16 giây.
- Lint: PASS, 0 error. Còn 54 cảnh báo/khuyến nghị, chủ yếu chuỗi tiếng Việt động, gợi ý KTX/version catalog/cập nhật dependency. Giữ phiên bản toolchain nguồn theo yêu cầu. Không tạo baseline để che lỗi.
- Asset SHA-256 khớp nguồn. Chín file toolchain/configuration ngoài app/build.gradle.kts khớp nguyên byte với origin/huyphan. App Gradle chỉ bỏ cấu hình/dependency giao diện Compose, thêm ViewBinding/Views và kiểm thử.

## Các tình huống đã chạy

SQLite tạm: tháng trống, khác tháng/năm, 2 buổi cùng ngày, tổng 50 phút không bị nhân đôi, cùng bài ở 2 buổi, bài không có hiệp, bài có hiệp chưa xong; cập nhật lịch chính 3 lần không tạo bản ghi thứ tư; 2 lịch còn lại không đổi; đóng/mở SQLite; migration chạy lại; ONCE giữ nguyên loại; từ chối ngày tuần không hợp lệ.

Quy tắc lịch: trước/sau giờ hiện tại, đúng thời điểm đã qua chuyển ngày kế, cuối tháng/năm, nhiều ngày, Chủ nhật, chuyển tuần, MON/WED/FRI và số 1–7, lịch tắt, giờ/ngày sai, ONCE quá hạn, DST gap/overlap.

Espresso: tháng Hủy/Xác nhận, xoay khi sheet đang mở, giữ tháng, Back từ nhắc; giờ Hủy, từ chối 25 giờ, Xác nhận 23:59, xoay form, chọn CN, lưu/mở lại; thiếu quyền vẫn cho lưu form; lỗi SQLite ẩn chỉ số thay vì giả 0, Thử lại hồi phục. Database instrumentation riêng, không xóa database người dùng.

## Đối chiếu giao diện

Đã xem ảnh emulator và đối chiếu các frame Figma trong phạm vi: thống kê trống/có dữ liệu, tháng/năm, nhắc tuần/ngày, thiếu quyền và chọn giờ. Sửa font về instance tĩnh tương thích API 27, icon SVG, nền bộ chọn tháng, màu ngày được chọn, bo góc và thanh kéo bottom sheet. Tháng 09/2026 trên UI hiển thị 11/450/30 đúng truy vấn nguồn. Ảnh nằm cùng thư mục. Layout thích ứng và cuộn; có khác biệt chiều cao do tỷ lệ emulator và thanh hệ thống thật. Biểu đồ, điều hướng ngoài hai module và nút bắt đầu tập bị loại theo phạm vi.

## Alarm và Notification chạy thực tế

- Khi thiếu quyền exact: RTC_WAKEUP với window=1 giờ; không crash, UI báo khả năng trễ (`alarm-inexact.txt`).
- Cấp quyền exact: RTC_WAKEUP window=0, exactAllowReason=permission (`alarm-exact-before.txt`).
- Lưu DAILY 23:50:00 bằng form, đưa app xuống nền: notification id=41 xuất hiện vào 23:50 ngày 07/10/2026 theo giờ emulator, channel workout_reminders, tiêu đề/nội dung tiếng Việt và AUTO_CANCEL (`notification-delivery.txt`, `notification-delivered.png`).
- Sau nhận alarm, lần tiếp theo là 08/10/2026 23:50 (`alarm-after-delivery.txt`).
- Chạm notification mở màn hình Nhắc nhở, notification không còn trong danh sách đang hoạt động.
- Reboot emulator: chưa mở ứng dụng, alarm ngày hôm sau đã được phục hồi; không phát notification ngay (`alarm-after-reboot.txt`).
- Tắt cấu hình và Lưu: không còn alarm pending của app; dumpsys ghi alarm_cancelled (`alarm-disabled.txt`).
- Database đã cài trên QA vẫn có 3 lịch: chỉ id=1 thay đổi; id=2 WEEKLY SAT 08:00 và id=3 DAILY 21:30 giữ nguyên.

## Giới hạn kiểm chứng

Chưa chạy trên điện thoại vật lý hoặc emulator API 27; chưa kiểm thử kéo dài qua nhiều ngày, Doze/OEM tiết kiệm pin và đổi múi giờ thực tế. Logic DST được unit test. Android có thể trì hoãn alarm không chính xác; force-stop chặn lịch cho đến khi mở lại. Các log có cảnh báo native access từ Robolectric chạy trên Java 25, không làm test thất bại.

Source thuộc nhánh fresh-start. `.idea/` có sẵn được giữ nguyên và ignore. Không có source/giao diện ngoài hai module được nhập từ nhánh nguồn.
