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

Module Thống kê đã được cập nhật theo Figma: có biểu đồ tần suất, bố cục trống/lỗi và nút Bắt đầu tập luyện. Biểu đồ truy vấn database, gồm bốn nhóm ngày 1–7, 8–14, 15–21 và 22–cuối tháng để giữ bốn cột như thiết kế và không bỏ buổi cuối tháng. Cột có số buổi lớn nhất tô #111827. Nút Bắt đầu tập luyện hiện thông báo chưa tích hợp vì repo chưa có màn hình tập luyện. Các tab ngoài hai module, snooze và status/navigation bar mô phỏng vẫn ngoài phạm vi. Android quản lý thanh hệ thống; vùng nội dung xử lý insets. Các ngày chọn là control thật có touch target 48dp và được xuống dòng ở màn hình hẹp. DAILY/WEEKLY đều có nhãn dễ đọc cả trạng thái chưa chọn (khắc phục chữ trắng trên nền trắng trong frame).
