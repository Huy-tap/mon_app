# Giao diện Cài đặt — giai đoạn UI

Thiết kế được đọc bằng MCP Figma từ file `kjkcenI9nvQYV8xEku9osc`:

- Cài đặt sáng / tối: `288:4650`, `288:4731`.
- Chưa thiết lập / thiếu quyền thông báo: `288:4812`, `288:4881`.
- Nhắc hằng ngày / theo tuần: `288:4964`, `288:5111`.
- Cảnh báo quyền / chọn giờ: `288:5013`, `288:5170`.

Mở app → tab **Cài đặt**, từ Trang chủ hoặc Bài tập. Chọn Sáng/Tối để xem giao diện, vào **Thiết lập lịch nhắc** để thử bật/tắt, đổi tần suất, chọn ngày và nhập giờ. **Lưu cài đặt** cập nhật bản xem thử trong màn hình; thông báo sau khi bấm giải thích rõ chưa lưu lịch vào thiết bị. Quay lại khi chưa bấm Lưu sẽ bỏ phần chỉnh sửa. Nội dung được giữ khi Android tạo lại Activity.

Màn hình đọc lịch hiện có và quyền thông báo để hiển thị trạng thái ban đầu. Giai đoạn này **chưa ghi SQLite, chưa lưu theme toàn app, chưa đặt/hủy alarm và chưa xin quyền thông báo**. Thoát module rồi mở lại sẽ đọc dữ liệu hiện có. Nút Mở cài đặt hệ thống dẫn đến trang quyền thông báo của ứng dụng.

## File chính

- `xmlui/SettingsXmlActivity.kt`: trạng thái xem thử, điều hướng trong module, chọn giờ, khôi phục trạng thái.
- `res/layout/activity_settings_xml.xml`: trang cài đặt chính.
- `res/layout/activity_reminder_settings_xml.xml`: form lịch nhắc.
- `res/layout/settings_time_picker.xml`: bảng nhập giờ/phút, kiểm tra 00–23 và 00–59.
- `res/values/settings_xml_styles.xml`, `res/drawable/settings_*.xml`: theme và nền.
- `assets/figma/settings/`: SVG tải từ kết quả MCP; không phụ thuộc URL Figma lúc chạy.
- `res/font/`: Outfit và Manrope từ Google Fonts; giấy phép trong `assets/licenses/`.

Dùng thanh trạng thái/thanh hệ thống thật của Android thay cho các thanh minh họa trong Figma. Bố cục cuộn theo chiều cao thiết bị; nút Lưu và thanh tab ở ngoài vùng cuộn. Chữ tab tần suất chưa chọn được tăng độ tương phản (Figma dùng chữ trắng trên nền trắng). Trạng thái thiếu quyền vẫn cho chỉnh bản nháp; không thể lên lịch thật trong giai đoạn UI. Theme tối của form nhắc nhở dùng token sẵn có vì Figma chỉ cung cấp form sáng.

## Kiểm tra

Build debug và unit test; kiểm thử máy ảo bằng `SettingsXmlTest` và `ExerciseXmlNavigationTest`. Kiểm tra chuyển Trang chủ ↔ Cài đặt ↔ Bài tập, đổi theme, ngày/giờ, nhập giờ sai, khôi phục form/bảng giờ sau recreate và xác nhận dữ liệu nhắc/theme trong SQLite không bị thay đổi.
