# Bản XML: Trang chủ, Quản lý bài tập, Thống kê và Nhắc nhở

Mở thư mục `FitnessAppmyworkpart2` bằng Android Studio, chờ Gradle Sync rồi chạy cấu hình `app`.

## Phạm vi đã chuyển

Phần **Quản lý bài tập** dùng XML và Android Views: danh sách RecyclerView, thêm/sửa bài, chọn nhóm cơ, bảng chọn nguồn ảnh/video, chi tiết, xác nhận xóa, xem ảnh và phát video.

**Trang chủ** cũng đã chuyển sang XML, giữ phần tổng kết tháng, buổi tập gần nhất và nhắc nhở từ dữ liệu SQLite hiện có.

**Thống kê và Nhắc nhở** đã ghép từ `module-ntd`, dùng XML/ViewBinding và Fragment. Vào Thống kê từ thanh điều hướng, vào Nhắc nhở từ nút Chỉnh sửa trên Trang chủ hoặc nút chuông trong Thống kê. Chạm thông báo cũng mở Nhắc nhở.

**Tạm khóa**: Theo dõi/ghi nhận tập luyện, Lịch sử và Cài đặt tổng. Nút “Ghi nhận buổi tập”, “Xem tất cả” và thẻ buổi tập gần nhất vẫn hiện “Trang này chưa phát triển”. Nút “Bắt đầu tập luyện” trong Thống kê thông báo màn hình tập luyện chưa tích hợp.

`MainActivity` mở trực tiếp Trang chủ XML. Các màn hình đang hoạt động không cần Compose. Đã xóa `ui/StatsScreen.kt` và hàm `ReminderSettingsScreen` cũ. Thư mục `ui` giữ code các module Compose còn lại để phát triển tiếp; `FitnessMainApp` và `ExerciseXmlRoute` không được gọi trong luồng chạy hiện tại. Không xóa database hay lịch sử khi khóa module.

## File của Trang chủ

- View: `app/src/main/res/layout/activity_home_xml.xml`.
- Màu/kiểu chữ: `app/src/main/res/values/home_xml_styles.xml`, dùng chung màu sáng/tối từ `exercise_xml_styles.xml`.
- Xử lý giao diện/nút bấm: `app/src/main/java/com/example/fitnessapp/xmlui/HomeXmlActivity.kt`.
- Đọc dữ liệu và giữ trạng thái: `app/src/main/java/com/example/fitnessapp/xmlui/HomeXmlModel.kt`.
- Điều hướng và khóa module: `app/src/main/java/com/example/fitnessapp/xmlui/XmlNavigation.kt`.
- Điểm vào app: `app/src/main/java/com/example/fitnessapp/MainActivity.kt`.

Khi phát triển module tiếp theo: hoàn thiện màn hình XML, nối sự kiện tương ứng trong `HomeXmlActivity` / `XmlNavigation`, rồi mới mở module trong `FeatureAvailability`. Không chỉ bỏ khóa để quay về giao diện Compose cũ.

## Đọc code theo thứ tự

1. `app/src/main/res/layout/screen_exercise_list_xml.xml`: bố cục danh sách và nút thêm.
2. `app/src/main/res/layout/item_exercise_xml.xml`: một dòng RecyclerView.
3. `app/src/main/res/layout/screen_exercise_form_xml.xml`: form thêm/sửa.
4. `app/src/main/res/layout/screen_exercise_detail_xml.xml`: màn hình chi tiết.
5. `app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt`: nối các View với sự kiện bấm, camera, thư viện và điều hướng.
6. `app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlModel.kt`: giữ dữ liệu đang nhập, tác vụ lưu và khôi phục trạng thái.
7. `app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlAdapter.kt`: gắn dữ liệu bài tập vào từng dòng XML.

`exercise_xml_styles.xml` trong `res/values` chứa màu, kiểu chữ, giao diện sáng/tối. Các file `ex_*.xml` trong `res/drawable` chứa nền, viền và góc bo.

## Dữ liệu và trạng thái

- Giữ cùng applicationId `com.example.fitnessapp`, cấu trúc SQLite, FileProvider và đường dẫn lưu media để cài cập nhật trên app hiện có.
- Database mẫu đã khai báo ảnh/video cho Push Up bằng đường dẫn `media/...`. `FitnessDatabase` gọi thêm `BundledExerciseMedia` để sao chép media mẫu và bổ sung cho bản đã cài. Các buổi tập, lịch sử và lựa chọn media của người dùng được giữ. Xem [HUONG_DAN_MEDIA_MAU.md](C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/HUONG_DAN_MEDIA_MAU.md).
- ViewModel giữ nội dung nhập khi tạo lại màn hình; Bundle lưu form, màn hình hiện tại, vị trí cuộn và đường dẫn camera để khôi phục sau khi Android đóng tiến trình.
- Kết quả camera/thư viện được chờ xử lý nếu database chưa tải xong sau khi khôi phục tiến trình.
- Không gỡ ứng dụng hoặc xóa dữ liệu nếu muốn giữ database hiện có trên thiết bị. Hai thư mục project không tạo hai bộ dữ liệu riêng trên cùng máy khi dùng cùng applicationId.

## Kiểm thử

Kiểm tra bản Trang chủ XML: build debug thành công; 5 unit test, 7 test Quản lý bài tập và 3 test điều hướng/Trang chủ đều đạt. Đã chạy lại 3 test Trang chủ ở kích thước màn hình nhỏ 320 × 550 dp, bao gồm giữ vị trí cuộn khi tạo lại màn hình và chuyển sang Bài tập rồi quay lại. Đã đối chiếu Trang chủ với APK trước khi chuyển trên cùng máy ảo.

Ở giai đoạn Quản lý bài tập trước đó, 8 test lưu trữ cũng đã đạt; camera, thư viện ảnh/video và khôi phục sau khi đóng tiến trình đã được kiểm tra.

- `ExerciseXmlTest`: thêm/sửa, giữ lịch sử khi sửa/xóa, khôi phục form, giữ kết quả camera trong lúc khởi tạo lại, hủy bảng chọn media, vùng cuộn không đè nút.
- `ExerciseXmlNavigationTest`: chuyển nhiều lần giữa Trang chủ và Bài tập XML, chặn toàn bộ lối vào module chưa phát triển, kiểm tra dữ liệu không đổi và giữ vị trí cuộn khi tạo lại màn hình.
- `FitnessPersistenceTest`: lưu buổi tập, các hiệp, thời lượng, giao dịch, nhắc nhở, migration và tên trùng.
- Kiểm thử thủ công trên máy ảo: chụp ảnh, quay và phát video, chọn ảnh/video từ thư viện; đóng tiến trình app trong lúc mở camera rồi xác nhận ảnh.

Các màn hình Compose cũ và test dựa trên cây giao diện Compose của module đã chuyển nằm trong `reference/compose-exercises` để đối chiếu. Chúng không được biên dịch vào ứng dụng mới. Thành phần ảnh/dòng bài tập dùng chung cho màn hình chọn bài của phần ghi nhận buổi tập vẫn được giữ trong `ui`.

APK sau khi build: `app/build/outputs/apk/debug/app-debug.apk`.

Phạm vi xác nhận hiện tại là máy ảo Android. Cần đối chiếu thêm trên thiết bị bạn dùng; không coi việc chuyển công nghệ là chứng nhận mọi điểm ảnh và mọi trạng thái trên mọi thiết bị đều giống tuyệt đối.

## File Thống kê và Nhắc nhở

- `xmlui/StatisticsReminderXmlActivity.kt`: chứa hai Fragment, xử lý Back, thanh điều hướng và vùng thanh hệ thống/bàn phím.
- `xmlui/StatisticsFragment.kt`: chọn tháng, chỉ số, trạng thái trống/lỗi và tải lại khi quay lại.
- `xmlui/FrequencyChartView.kt`: biểu đồ 4 cột; nhóm cuối gồm ngày 22 đến cuối tháng.
- `xmlui/ReminderFragment.kt`: form lịch nhắc, quyền thông báo và báo thức chính xác.
- `xmlui/PickerSheets.kt`: hộp chọn tháng/năm và giờ/phút.
- `xmlui/UiAssets.kt`: đọc biểu tượng SVG từ `res/raw`.
- `data/FitnessRepository.kt`: tổng số buổi, tổng phút, lượt bài hoàn thành, tần suất và đọc/lưu lịch chính. `StatisticsTotals` là dữ liệu trả về cho module Thống kê, tránh trùng `model/MonthlyStats` của Trang chủ.
- `model/ReminderRules.kt`: tính lần nhắc tiếp theo, hỗ trợ DAILY/WEEKLY/ONCE.
- `data/ReminderScheduler.kt`: AlarmManager, Notification, khôi phục sau boot/đổi giờ/cập nhật app; bỏ qua báo thức cũ.
- `res/layout/activity_statistics_reminder_xml.xml`: khung màn hình và thanh điều hướng.
- `res/layout/fragment_statistics.xml`, `fragment_reminder.xml`: hai giao diện chính.
- `res/layout/sheet_month.xml`, `sheet_time.xml`: hộp chọn.
- `res/values/statistics_reminder_*.xml`, `res/raw`, các font Inter/Manrope/Outfit và nền đi kèm: tài nguyên module nguồn.

Lịch nhắc vẫn lưu trong bảng `reminders` như module nguồn. SharedPreferences trong module này chỉ lưu trạng thái xin quyền và thông tin lần đặt báo thức; chưa chuyển cấu hình lịch nhắc hoặc giao diện sang SharedPreferences theo đề. Hai màn hình mới giữ giao diện sáng của module nguồn; Trang chủ/Bài tập vẫn giữ cấu hình giao diện trước đó.

Giữ `FitnessDatabase.kt`, database mẫu và media của project đích. Không sao chép database nguồn đè lên dữ liệu hiện tại. `Reminder.kt` giữ bản hiện có vì nội dung tương đương.

Thứ tự đọc luồng: Trang chủ → `HomeXmlActivity.navigate("STATS")` hoặc `openReminder()` → `StatisticsReminderXmlActivity` → Fragment → `FitnessRepository(FitnessDatabase.open(context))` → SQLite. Khi lưu lịch: `ReminderFragment.save()` → repository → `ReminderScheduler.schedule()` → receiver → thông báo → `MainActivity` → Nhắc nhở XML.

Các kiểm thử module mới nằm trong `ReminderRulesTest`, `RepositoryTest`, `StatisticsReminderXmlTest`; kiểm thử điều hướng chung nằm trong `ExerciseXmlNavigationTest`. Instrumentation dùng `IsolatedRunner` với database và cấu hình riêng.
