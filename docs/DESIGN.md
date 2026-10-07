# Thiết kế và nguồn

Ref nguồn đã đọc: `origin/huyphan`, commit `1606b75896514ef61fc5ca397db8c2f59b8e9c71`.

Figma file `kjkcenI9nvQYV8xEku9osc`; design context và screenshot đã đọc qua MCP:

| Nhóm | Node |
| --- | --- |
| Thống kê có dữ liệu, trống, lỗi | 288:3336, 288:3417, 288:3484 |
| Chọn tháng/năm | 288:3542 |
| Tham chiếu trạng thái nhắc từ Cài đặt | 288:4650, 288:4731, 288:4812, 288:4881 |
| Nhắc hằng ngày, theo tuần | 288:4964, 288:5111 |
| Thiếu quyền, chọn giờ, notification | 288:5013, 288:5170, 288:5071 |

Nền #F8FAFC; chữ #111827 và #6B7280; thẻ trắng viền #E5E7EB, bo 16dp; nút nhắc #1E3A8A. Thống kê dùng Inter; nhắc dùng Outfit cho tiêu đề/số giờ và Manrope cho nội dung. Font được lưu trong res/font, giấy phép OFL trong docs/licenses. Các font biến thiên đã được tạo instance tĩnh bằng fontTools để hỗ trợ minSdk 27: Inter 500/800, Manrope 600/700, Outfit 700/800; tên nội bộ của bản instance là Fitness Stats/Body/Heading.

Icon SVG tải nguyên asset Figma và render bằng AndroidSVG: bell, chevron_left/right, back, clock, empty, error, edit. Callsite nằm trong StatisticsFragment, ReminderFragment và MonthSheet. Không có URL asset tạm trong mã sản phẩm. Notification dùng vector đơn sắc riêng phù hợp quy tắc small icon của Android.

Theo phạm vi yêu cầu, giao diện bỏ biểu đồ tùy chọn, các tab ngoài hai module, nút Bắt đầu tập luyện, snooze và status/navigation bar mô phỏng. Android quản lý thanh hệ thống; vùng nội dung xử lý insets. Các ngày chọn là control thật có touch target 48dp và được xuống dòng ở màn hình hẹp. DAILY/WEEKLY đều có nhãn dễ đọc cả trạng thái chưa chọn (khắc phục chữ trắng trên nền trắng trong frame).
