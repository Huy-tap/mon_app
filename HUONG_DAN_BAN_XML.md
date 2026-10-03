# Bản XML: Trang chủ và Quản lý bài tập

Mở thư mục `FitnessAppmyworkpart2` bằng Android Studio, chờ Gradle Sync rồi chạy cấu hình `app`.

## Phạm vi đã chuyển

Phần **Quản lý bài tập** dùng XML và Android Views: danh sách RecyclerView, thêm/sửa bài, chọn nhóm cơ, bảng chọn nguồn ảnh/video, chi tiết, xác nhận xóa, xem ảnh và phát video.

**Trang chủ** cũng đã chuyển sang XML, giữ phần tổng kết tháng, buổi tập gần nhất và nhắc nhở từ dữ liệu SQLite hiện có.

**Tạm khóa**: Theo dõi/ghi nhận tập luyện, Lịch sử, Thống kê, Cài đặt và chỉnh nhắc nhở. Bấm các mục này, nút “Ghi nhận buổi tập”, “Xem tất cả”, thẻ buổi tập gần nhất hoặc “Chỉnh sửa” sẽ hiện “Trang này chưa phát triển”. Sau khi đóng thông báo, người dùng vẫn ở trang đang xem.

`MainActivity` mở trực tiếp Trang chủ XML. Hai màn hình hiện hoạt động không còn cần cầu nối Compose. Thư mục `ui` giữ code cũ để phát triển tiếp; `FitnessMainApp` và `ExerciseXmlRoute` không được gọi trong luồng chạy hiện tại. Không xóa database hay lịch sử khi khóa module.

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
- Database trong assets và toàn bộ file thuộc `data`, `controller`, `model` giữ nguyên nội dung so với project gốc khi sao chép.
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
