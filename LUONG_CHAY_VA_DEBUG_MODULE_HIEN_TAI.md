# Luồng chạy hiện tại — Trang chủ và Quản lý bài tập

> Cập nhật giao diện Cài đặt: tab SETTINGS đã mở bằng XML; form nhắc nhở dùng chung cho Trang chủ/Cài đặt/Thống kê và lưu lịch thật. Xem [GIAO_DIEN_CAI_DAT.md](GIAO_DIEN_CAI_DAT.md) cho luồng hiện tại.


Tài liệu này bám theo code trong **FitnessAppmyworkpart2**. Đọc từ trên xuống như đang theo dõi Debug: thao tác → hàm xử lý → dữ liệu → giao diện.

**Phạm vi đang mở:** Trang chủ, Quản lý bài tập, Thống kê và Nhắc nhở dùng XML. Ghi nhận tập luyện, Lịch sử và Cài đặt tổng đang khóa. Trang chủ đọc dữ liệu SQLite để hiển thị tổng quan.

## 1. Trước hết, phân biệt các loại file

| Loại file | Vai trò trong app | Ví dụ |
|---|---|---|
| Layout XML — View | Mô tả vị trí chữ, nút, danh sách. Không tự truy vấn SQLite | [activity_home_xml.xml][home-xml] |
| Activity | Mở layout, bắt nút bấm, hiển thị dữ liệu, mở màn hình khác | [HomeXmlActivity.kt][home-create] |
| ViewModel | Giữ trạng thái màn hình, gọi tác vụ đọc/lưu dữ liệu | [HomeXmlModel.kt][home-load], [ExerciseXmlModel.kt][model-start] |
| Controller | Thực hiện thao tác dữ liệu, câu SQL đọc/ghi các bảng | [FitnessController.kt][controller] |
| Database | Mở database, sao chép dữ liệu ban đầu, bổ sung cấu trúc còn thiếu | [FitnessDatabase.kt][db-open] |
| Model dữ liệu | Chứa kết quả đọc từ database dưới dạng đối tượng Kotlin | [Exercise.kt][exercise], [Workout.kt][workout-model] |
| Adapter | Gắn từng đối tượng bài tập lên từng dòng RecyclerView | [ExerciseXmlAdapter.kt][adapter] |

**Lưu ý tên gọi:** `ExerciseXmlModel` là ViewModel giữ trạng thái màn hình; `Exercise` là đối tượng dữ liệu một bài tập. Hai file này làm việc khác nhau.

## 2. Bắt đầu mở ứng dụng: file nào chạy trước?

| Bước | File/hàm chạy | Điều xảy ra |
|---|---|---|
| 1 | [AndroidManifest.xml][manifest] | Khai báo `MainActivity` là màn hình mở từ biểu tượng ứng dụng |
| 2 | [MainActivity.kt][main] | `MainActivity : HomeXmlActivity()` kế thừa toàn bộ xử lý Trang chủ |
| 3 | [HomeXmlActivity.onCreate()][home-create] | Lấy/tạo `HomeXmlModel`, khôi phục vị trí cuộn nếu có |
| 4 | [HomeXmlActivity.showLayout()][home-layout] | `setContentView(R.layout.activity_home_xml)` nạp bố cục, gắn nút bấm và thanh menu |
| 5 | `model.state.collect { render(it) }` trong `onCreate()` | Đăng ký theo dõi dữ liệu màn hình. Khi trạng thái thay đổi sẽ cập nhật giao diện |
| 6 | [HomeXmlActivity.onResume()][home-resume] | Gọi `model.refresh()` để tải dữ liệu |
| 7 | [HomeXmlModel.refresh()][home-load] | Gọi Controller trên luồng xử lý dữ liệu, rồi cập nhật `state` |
| 8 | [HomeXmlActivity.render(state)][home-render] | Đọc `state.data`, tính thống kê tháng và gán nội dung vào các TextView |

`MainActivity` không có `onCreate()` riêng vì dùng hàm được kế thừa từ `HomeXmlActivity`.

Thứ tự ở trên là luồng chính để học. `render()` có thể được gọi khi chưa tải xong để hiện trạng thái chờ, rồi được gọi lại khi có dữ liệu; không phải mỗi hàm chỉ chạy đúng một lần.

## 3. Database thực tế được mở ở đâu?

Khi `HomeXmlModel.refresh()` chạy dòng `FitnessController(context)`, constructor của Controller gọi [FitnessDatabase.open(context)][db-open].

```text
FitnessController(context)
  → FitnessDatabase.open(context)
      → Có kết nối SQLite đang mở: dùng lại kết nối đó
      → Chưa có kết nối: openInternal(context)
          → Lấy đường dẫn bằng context.getDatabasePath("fitness_app.db")
          → File chưa có hoặc rỗng: copyBundledDatabase(...)
          → SQLiteDatabase.openDatabase(...)
          → migrate(database)
          → BundledExerciseMedia.install(context, database): bổ sung ảnh/video mẫu
          → Giữ kết nối để dùng lại
```

- **File ban đầu trong project:** `C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/assets/fitness_app.db`.
- **File đang sử dụng:** database trong bộ nhớ riêng của ứng dụng trên máy ảo/điện thoại. Xem giá trị `databaseFile.absolutePath` trong Debug để biết đường dẫn chính xác.
- Thêm/sửa/xóa từ giao diện sẽ ghi vào database trên thiết bị, không ghi ngược vào file trong `assets`.
- [migrate()][db-migrate] bổ sung `exercises.is_archived`, các cột lưu tên/nhóm cơ/thời lượng trong `workout_exercises`, và bảng `app_state` nếu chưa có.
- Nếu database đã mở trong tiến trình, `open()` có thể trả về ngay. Vì vậy breakpoint trong `openInternal()` không nhất thiết dừng mỗi lần bạn mở một màn hình.

## 4. Trang chủ lấy từng phần dữ liệu như thế nào?

Trong [HomeXmlModel.refresh()][home-load], ba lời gọi chính tạo ra `HomeXmlData`:

| Lời gọi | Dữ liệu đọc | Được dùng ở đâu? |
|---|---|---|
| [controller.getAllWorkouts()][workouts] | Các buổi tập, bài trong buổi và kết quả hiệp | Tổng kết tháng và buổi tập gần nhất |
| [controller.getPrimaryReminder()][reminder] | Dòng đầu tiên theo `reminder_id` của bảng `reminders`; không có thì dùng đối tượng mặc định | Giờ nhắc, kiểu lặp, trạng thái bật/tắt |
| [controller.getState("dark_theme")][get-state] | `app_state.state_value` với `state_key = 'dark_theme'` | Chọn giao diện sáng/tối |

### 4.1. Đi sâu vào danh sách buổi tập

```text
FitnessController.getAllWorkouts()
  → Đọc workout_id từ bảng workouts
     Sắp xếp workout_date DESC, workout_id DESC
  → Với từng id: getWorkoutDetail(id)
      → workouts: ngày tập, tổng phút, ghi chú
      → workout_exercises: các bài của buổi đó
      → workout_sets: từng hiệp của mỗi bài
      → Ghép thành một đối tượng Workout
  → Trả về List<Workout>
```

Xem [getWorkoutDetail()][workout-detail]. Tên bài trong lịch sử được đọc từ **`workout_exercises.exercise_name`**, tức tên đã lưu lúc ghi nhận; luồng đọc này không lấy tên hiện tại của danh mục để thay thế lịch sử.

### 4.2. Dữ liệu đi từ ViewModel lên giao diện

Sau khi đọc xong, `HomeXmlModel` gán:

```kotlin
mutableState.value = HomeXmlState(data)
```

`HomeXmlActivity` đang theo dõi `state`, nên nhận trạng thái mới và chạy `render(state)`.

| Ô trên Trang chủ | Cách lấy/tính trong `render()` | ID trong XML |
|---|---|---|
| Ngày hiện tại | `LocalDate.now()` theo ngày trên thiết bị | `home_date` |
| Tháng đang xem | `today.monthValue` | `home_month` |
| Buổi tập | `stats.totalWorkouts` | `home_workouts` |
| Phút tập | `stats.totalMinutes` | `home_minutes` |
| Lượt bài xong | `stats.completedSets` | `home_completed` |
| Buổi gần nhất | `data.workouts.firstOrNull()` | Nhóm `home_recent_*` |
| Nhắc nhở | `data.reminder`, kết hợp quyền thông báo | Nhóm `home_reminder_*` |

**Các chỉ số tổng quan trên Trang chủ được tính ở [FitnessRules.monthStats()][rules], nằm trong file WorkoutDraft.kt.** Hàm lọc theo tháng của `workoutDate`, đếm buổi, cộng `durationMinutes` và đếm những lượt bài có hiệp, trong đó tất cả các hiệp đều hoàn thành.

Tên biến `completedSets` dễ gây nhầm: code hiện tại đếm **lượt bài hoàn thành**, không cộng số hiệp, cũng không đếm số tên bài khác nhau.

Ví dụ: nếu `today` thuộc tháng 10/2026 nhưng các buổi đều có `workout_date` bắt đầu bằng `2026-09`, thống kê tháng hiện tại là 0. Buổi gần nhất vẫn có thể hiện một buổi tháng 9 vì phần đó lấy từ toàn bộ danh sách.

Trang chủ hiện gọi trực tiếp `FitnessRules.monthStats(...)`, không gọi `FitnessController.getMonthlyStats()`.

Ngoài việc đọc dữ liệu, `refresh()` còn gọi [ReminderScheduler.schedule()][scheduler] để cập nhật lịch báo theo cấu hình đã lưu. Thao tác này không tạo buổi tập mới.

## 5. Từ Trang chủ bấm “Bài tập”

```text
Bấm ex_tab_EXERCISES trên thanh menu XML
  → XmlNavigation.bind(): listener nhận tab = "EXERCISES"
  → Kiểm tra tab có trong availableTabs
  → Gọi callback trong HomeXmlActivity.showLayout()
  → exercises.launch(Intent(...ExerciseXmlActivity...))
  → Android mở ExerciseXmlActivity
```

Xem [XmlNavigation.kt][navigation] và [HomeXmlActivity.showLayout()][home-layout]. Intent truyền cấu hình `dark`; không truyền danh sách bài tập. Màn hình Bài tập tự đọc danh sách qua Controller.

### 5.1. Màn hình Bài tập tải danh sách

| Bước | File/hàm | Nội dung |
|---|---|---|
| 1 | [ExerciseXmlActivity.onCreate()][ex-create] | Nạp khung `activity_exercise_xml`, lấy/tạo `ExerciseXmlModel` |
| 2 | `model.revision.collect { render() }` | Theo dõi tín hiệu để vẽ lại màn hình |
| 3 | [ExerciseXmlModel.start(...)][model-start] | Không có `route` từ Intent thì bắt đầu ở `list`; có trạng thái lưu thì khôi phục |
| 4 | [ExerciseXmlModel.task(...)][model-task] | Bật `busy`, xử lý tác vụ đọc dữ liệu trong `Dispatchers.IO` |
| 5 | `controller = FitnessController(...)`, rồi `reload()` | Gọi `controller.getAllExercises()` |
| 6 | [FitnessController.getAllExercises()][get-exercises] | Đọc bảng `exercises`, chỉ lấy dòng có `is_archived = 0` |
| 7 | `ready = true`, kết thúc `task()` | `busy = false`, gọi `changed()` làm tăng `revision` |
| 8 | [ExerciseXmlActivity.render()][ex-render] | Đọc `model.screen`; nếu là `list` thì gọi `renderList()` |
| 9 | [renderList()][ex-list] | Nạp [screen_exercise_list_xml.xml][list-xml], gán số bài, tạo RecyclerView và Adapter |
| 10 | `adapter.submitList(model.exercises)` | Đưa danh sách vào Adapter |
| 11 | [ExerciseXmlAdapter.onBindViewHolder()][adapter] | Gán tên, nhóm cơ, số hiệp/lần và ảnh lên [item_exercise_xml.xml][row-xml] |

Câu SQL lấy danh mục:

```sql
SELECT * FROM exercises
WHERE is_archived = 0
ORDER BY exercise_id;
```

`revision` chỉ là một số báo “trạng thái đã đổi”. Danh sách thực tế nằm ở `model.exercises`; màn hình hiện tại nằm ở `model.screen`.

## 6. Bấm một bài để xem chi tiết

```text
Bấm một dòng RecyclerView
  → Listener trong ExerciseXmlAdapter nhận đối tượng Exercise
  → Callback của renderList()
  → model.navigate("detail/<id>")
  → Thêm route vào stack, gọi changed()
  → render() gọi renderDetail()
  → model.selected() tìm bài theo id trong model.exercises
  → Gán nội dung lên screen_exercise_detail_xml.xml
```

Xem [navigate()][model-route], [renderDetail()][ex-detail] và [XML chi tiết][detail-xml].

Ở luồng bấm từ danh sách này, chi tiết dùng dữ liệu đã tải trong `model.exercises`, không truy vấn SQLite lại mỗi lần bấm.

Ví dụ route và stack dưới đây chỉ để minh họa, không khẳng định database đang có bài số 5:

```text
Danh sách:            ["list"]
Mở chi tiết bài 5:    ["list", "detail/5"]
Bấm sửa:             ["list", "detail/5", "edit/5"]
```

## 7. Thêm bài tập: khi nào dữ liệu thật sự được lưu?

| Bước | Hàm chạy | Dữ liệu thay đổi |
|---|---|---|
| Bấm “Thêm bài tập” | `renderList()` → `model.navigate("add")` | Chuyển màn hình, chưa ghi SQLite |
| Chuẩn bị form | `prepareForm(null)` trong ViewModel | Tên/nhóm cơ rỗng, mặc định 3 hiệp và 12 lần |
| Hiện form | [renderForm()][ex-form] | Nạp [screen_exercise_form_xml.xml][form-xml] |
| Gõ tên, số hiệp/lần | `doAfterTextChanged` | Ghi chuỗi vào `model.form` — một Bundle giữ nội dung đang nhập |
| Chọn nhóm cơ | [showMuscles()][ex-muscles] | Ghi `form["muscle"]`, cập nhật chữ trên form |
| Bấm “Lưu bài tập” | `model.save()` | Bắt đầu kiểm tra và lưu |

Chi tiết [ExerciseXmlModel.save()][model-save]:

```text
save()
  → attempted = true
  → draft(): ghép model.form thành đối tượng Exercise
  → FitnessRules.exerciseError(exercise): kiểm tra dữ liệu
      → Sai: changed() để hiện lỗi, không ghi database
      → Đúng: task { ... }
          → original == null: controller.insertExercise(exercise)
          → reload(): đọc lại danh mục từ SQLite
          → Bỏ route "add", xóa form tạm
  → task kết thúc: changed()
  → renderList(): hiển thị lại danh sách đã có bài mới
```

[insertExercise()][insert] kiểm tra dữ liệu rồi ghi vào bảng **`exercises`** bằng `insertOrThrow()`.

**Thêm bài vào danh mục không tạo buổi tập.** Luồng này không ghi `workouts`, nên số buổi trên Trang chủ không tăng.

Nếu tên bị trùng, ràng buộc tên duy nhất trong SQLite báo lỗi; `task()` đổi thành thông báo “Tên bài tập đã tồn tại. Hãy chọn tên khác.”

## 8. Sửa và xóa bài tập

### Sửa

```text
renderDetail(): bấm “Sửa bài tập”
  → model.navigate("edit/<id>")
  → original = bài đang sửa; prepareForm(original) điền dữ liệu cũ
  → renderForm(): người dùng thay đổi nội dung
  → model.save(): original khác null
  → FitnessController.updateExercise(exercise)
  → UPDATE bảng exercises theo exercise_id
  → reload(), bỏ route edit, renderDetail() với dữ liệu mới
```

Xem [updateExercise()][update]. Sửa tên ở đây không sửa `workout_exercises.exercise_name`, nên tên đã lưu trong lịch sử được giữ nguyên.

### Xóa

```text
renderDetail(): bấm Xóa → hiện hộp xác nhận
  → Bấm xác nhận: model.delete(e)
  → FitnessController.deleteExercise(e.id)
  → UPDATE exercises SET is_archived = 1 WHERE exercise_id = ...
  → Loại bài khỏi buổi tập đang nhập dở, lưu lại workout_draft
  → reload(): danh mục chỉ lấy is_archived = 0 nên bài biến mất
  → Bỏ route liên quan đến bài đã xóa
```

Xem [ExerciseXmlModel.delete()][model-delete] và [FitnessController.deleteExercise()][delete-db].

Đây là **ẩn bài khỏi danh mục**, không xóa dòng bài tập khỏi SQLite. Các buổi đã ghi trong `workouts`, `workout_exercises`, `workout_sets` vẫn được giữ. Việc xóa còn có thể cập nhật khóa `workout_draft` trong `app_state` để bản nháp không chứa bài đã ẩn.

## 9. Chọn ảnh/video: file và database liên quan như thế nào?

**Media có sẵn:** trước khi màn hình đọc bài tập, `FitnessDatabase.openInternal()` gọi `BundledExerciseMedia.install()`. Hàm đọc đường dẫn `media/...` trong database đóng gói, sao chép file sang bộ nhớ app và cập nhật đường dẫn cho bài cùng mã, giữ media người dùng đã chọn. Xem [hướng dẫn media mẫu](C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/HUONG_DAN_MEDIA_MAU.md). Các luồng dưới đây là thao tác chọn/chụp media của người dùng.

Bắt đầu ở `wireMedia()` → [showSources(video)][ex-sources]. Tham số `video = false` là ảnh; `true` là video.

| Thao tác | Luồng xử lý |
|---|---|
| Chọn từ thư viện | Photo Picker trả `Uri` → `model.importMedia(uri, video)` → `MediaStorage.import(...)` |
| Chụp ảnh/quay video | `openCamera(video)` → tạo file đích và URI FileProvider → mở camera → callback `model.captured(ok)` |
| Hủy chọn từ thư viện | `uri == null` → kết thúc, giữ lựa chọn cũ |
| Xem trước | `model.preview(path, video)` → `renderMedia()` → ảnh hoặc VideoView |

Xem [importMedia()][model-import], [MediaStorage.import()][media], [openCamera()][ex-camera] và [captured()][model-captured].

- File media được lưu vào vùng bộ nhớ ngoài dành riêng cho app, trong thư mục Pictures hoặc Movies.
- Đường dẫn được giữ tạm trong `model.form` ở khóa `image` hoặc `video`.
- **Chỉ khi lưu bài tập**, đường dẫn mới được ghi vào `exercises.instruction_image` hoặc `exercises.instruction_video`.
- SQLite lưu **đường dẫn**, không chứa toàn bộ ảnh/video. Chọn media có thể đã tạo file trên bộ nhớ dù bạn chưa lưu bài.
- Khi Android khôi phục ứng dụng và kết quả camera về trước lúc database tải xong, ViewModel chờ `ready && !busy` rồi mới xử lý kết quả.

## 10. Quay lại Trang chủ, khôi phục trạng thái và mục bị khóa

**Từ danh sách Bài tập bấm Trang chủ:** `XmlNavigation.bind()` → callback `close("HOME")` → [ExerciseXmlActivity.close()][ex-close] gọi `setResult()` và `finish()`. Android đưa Trang chủ phía dưới lên lại → `HomeXmlActivity.onResume()` → `model.refresh()`.

Callback nhận kết quả trong `HomeXmlActivity` hiện để trống. Việc tải lại dữ liệu xảy ra qua **onResume()**, không nằm trong callback đó.

**Bấm Back trong module Bài tập:** `back()` kiểm tra thay đổi chưa lưu; nếu được phép quay lại thì `model.back()` bỏ route cuối. Khi không còn màn hình để quay lại trong module, `finished = true`, `render()` gọi `close()`.

**Xoay máy hoặc Android tạo lại màn hình:**

- Trang chủ dùng ViewModel giữ dữ liệu; `onSaveInstanceState()` lưu vị trí cuộn. `render()` khôi phục cuộn sau khi có dữ liệu.
- Bài tập dùng [saveState()][model-state] lưu stack, form, vị trí cuộn, đường dẫn camera, trạng thái media vào Bundle. `start()` đọc lại nếu cần khởi tạo sau khi tiến trình bị đóng.
- Nội dung form chưa lưu không phải một dòng trong bảng `exercises` và không được đảm bảo giữ sau khi người dùng chủ động thoát/hủy form hoặc xóa dữ liệu ứng dụng.

**Điều hướng:** [FeatureAvailability][locked] cho phép đủ năm tab `HOME`, `EXERCISES`, `HISTORY`, `STATS`, `SETTINGS`. `HomeXmlActivity` nhận kết quả tab từ các Activity và mở màn hình tương ứng. Ghi nhận mở `RecordWorkoutXmlActivity`, Xem tất cả mở `WorkoutHistoryXmlActivity`, buổi gần nhất mở `WorkoutDetailXmlActivity`; chỉnh nhắc nhở mở `SettingsXmlActivity` với `reminder=true`.

## 11. Tự quan sát bằng Debug trong Android Studio

1. Mở đúng project **FitnessAppmyworkpart2**, chọn cấu hình `app` và máy ảo.
2. Mở file Kotlin, bấm lề trái cạnh một dòng lệnh có thể thực thi để đặt breakpoint; chọn **Debug app**.
3. Khi dừng, xem **Variables**. Dùng **Step Over** để chạy qua dòng, **Step Into** để vào hàm, **Resume Program** để tới breakpoint tiếp theo. Nguồn thao tác: [Android Studio — Debug your app](https://developer.android.com/studio/debug).

Đặt vài breakpoint cho một luồng mỗi lần. Nên đặt ở dòng lệnh **bên trong hàm**, không ở dòng `package`, `import`, XML hoặc comment. Dòng đang được tô sáng thường là dòng sắp thực thi; muốn xem kết quả gán biến, chạy qua dòng đó trước.

### Lượt A — quan sát Trang chủ lấy dữ liệu

| Thứ tự | Vị trí đặt breakpoint | Biến cần xem |
|---|---|---|
| 1 | [HomeXmlActivity.onCreate()][home-create], tại dòng lấy ViewModel | `savedInstanceState` |
| 2 | [HomeXmlModel.refresh()][home-load], tại `val controller = FitnessController(context)` | `loading`, `context` |
| 3 | [FitnessDatabase.open()][db-open] | `instance`; nếu mở mới, đi tiếp để xem `databaseFile.absolutePath` |
| 4 | [getWorkoutDetail(id)][workout-detail], bên trong hàm | `id`, `workout`, `entries` sau khi được gán |
| 5 | `HomeXmlModel.refresh()`, tại `mutableState.value = HomeXmlState(data)` | `data.workouts.size`, `data.reminder`, `data.dark` |
| 6 | [HomeXmlActivity.render()][home-render], sau dòng tính `stats` | `today`, `data.workouts`, `stats.totalWorkouts`, `stats.totalMinutes`, `stats.completedSets` |

Nếu breakpoint trong `render()` dừng nhiều lần khi `state.data == null`, bấm Resume tới lượt có dữ liệu. Nếu chỉ tìm nguyên nhân thống kê tháng bằng 0, xem `today`, các giá trị `workoutDate`, rồi vào [monthStats()][rules] và quan sát `selected` sau dòng lọc.

### Lượt B — từ Trang chủ vào Bài tập

| Thứ tự | Vị trí | Biến cần xem |
|---|---|---|
| 1 | [XmlNavigation.bind()][navigation], **bên trong** `setOnClickListener` | `tab` phải là `EXERCISES` |
| 2 | [ExerciseXmlActivity.onCreate()][ex-create], tại `model.start(...)` | `intent`, `savedInstanceState` |
| 3 | [ExerciseXmlModel.start()][model-start], bên trong `task` | `stack`, `screen`, `ready`, `busy` |
| 4 | [FitnessController.getAllExercises()][get-exercises], trong thân đọc cursor | Kết quả đọc từng bài |
| 5 | [renderList()][ex-list], tại `submitList(...)` | `model.exercises.size`, `model.exercises` |
| 6 | [Adapter.onBindViewHolder()][adapter], sau `val e = getItem(position)` | `position`, `e.name`, `e.muscleGroup`, `e.instructionImage` |

Lưu ý: nếu đặt breakpoint ở đầu `XmlNavigation.bind()`, nó dừng lúc gắn menu. Muốn dừng đúng lúc người dùng bấm thì đặt **trong listener**. Tương tự, đặt trong thân `task { ... }` để theo dõi công việc thật sự được chạy.

### Lượt C — lưu thử một bài

1. Bấm Thêm bài tập, nhập một tên chưa có, chọn nhóm cơ và số hiệp/lần.
2. Dừng ở [ExerciseXmlModel.save()][model-save]: xem `form`, `original` và đối tượng `exercise` sau `draft()`.
3. `original == null` nghĩa là thêm mới; khác null nghĩa là sửa.
4. Dừng ở [insertExercise()][insert] hoặc [updateExercise()][update]: xem giá trị chuẩn bị ghi SQLite.
5. Dừng ở `reload()` sau khi ghi: xem danh sách đọc lại, rồi Resume để giao diện cập nhật.

Thao tác lưu trong Debug **vẫn ghi dữ liệu thật vào database của máy ảo đang chọn**.

### Vì sao bấm Step Into đôi khi đi vào code thư viện?

Code dùng coroutine: `viewModelScope.launch` khởi động tác vụ; `withContext(Dispatchers.IO)` chạy phần đọc/ghi trên luồng dữ liệu. Khi xong, code cập nhật trạng thái và Activity nhận thay đổi để vẽ lại.

Để dễ học, đặt breakpoint ngay trong hàm của app muốn xem rồi bấm Resume. Không cần đi qua toàn bộ phần xử lý nội bộ của coroutine hoặc Android.

## 12. Khi gặp một hiện tượng, bắt đầu đọc file nào?

| Hiện tượng | Điểm bắt đầu |
|---|---|
| Trang chủ tháng 10 hiện 0 | `HomeXmlActivity.render()` → `FitnessRules.monthStats()` → kiểm tra `workoutDate` |
| Danh sách không hiện một bài | `FitnessController.getAllExercises()` → kiểm tra `is_archived` |
| Lưu không thành công | `ExerciseXmlModel.save()` → `FitnessRules.exerciseError()` → Controller; xem `model.error` |
| Chọn ảnh xong chưa thấy đường dẫn trong SQLite | Kiểm tra `model.form`, rồi xác nhận đã bấm Lưu bài tập hay chưa |
| Ảnh hiện dấu trống | `decodeExercisePhoto()` trong ExerciseXmlAdapter.kt → kiểm tra đường dẫn và file tồn tại |
| Xóa rồi database vẫn có dòng đó | `deleteExercise()` dùng `is_archived = 1`; đây là cách giữ liên kết với lịch sử |
| Lịch sử tháng hiện tại trống | Chọn tháng có dữ liệu; database mẫu có lịch sử tháng 09/2026 |
| Sửa file Compose mà app không thay đổi | Luồng đang mở là `MainActivity` → `HomeXmlActivity` → `ExerciseXmlActivity` |

Các số dòng trong liên kết là vị trí lúc tạo tài liệu. Nếu bạn chỉnh code làm dòng thay đổi, tìm theo tên hàm được ghi bên cạnh.

[main]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/MainActivity.kt:6>

[manifest]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/AndroidManifest.xml:31>

[home-create]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/HomeXmlActivity.kt:31>

[home-layout]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/HomeXmlActivity.kt:48>

[home-resume]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/HomeXmlActivity.kt:41>

[home-render]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/HomeXmlActivity.kt:77>

[home-load]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/HomeXmlModel.kt:25>

[home-xml]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/res/layout/activity_home_xml.xml>

[navigation]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/XmlNavigation.kt:27>

[locked]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/XmlNavigation.kt:15>

[db-open]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/data/FitnessDatabase.kt:14>

[db-migrate]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/data/FitnessDatabase.kt:42>

[controller]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/controller/FitnessController.kt:15>

[workouts]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/controller/FitnessController.kt:49>

[workout-detail]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/controller/FitnessController.kt:54>

[reminder]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/controller/FitnessController.kt:111>

[get-state]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/controller/FitnessController.kt:126>

[rules]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/model/WorkoutDraft.kt:31>

[get-exercises]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/controller/FitnessController.kt:23>

[insert]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/controller/FitnessController.kt:37>

[update]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/controller/FitnessController.kt:41>

[delete-db]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/controller/FitnessController.kt:46>

[ex-create]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt:56>

[ex-render]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt:111>

[ex-list]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt:163>

[ex-form]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt:179>

[ex-detail]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt:291>

[ex-camera]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt:280>

[ex-sources]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt:258>

[ex-muscles]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt:220>

[ex-close]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlActivity.kt:359>

[model-start]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlModel.kt:50>

[model-route]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlModel.kt:99>

[model-save]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlModel.kt:134>

[model-delete]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlModel.kt:145>

[model-task]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlModel.kt:188>

[model-state]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlModel.kt:88>

[model-import]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlModel.kt:153>

[model-captured]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlModel.kt:162>

[adapter]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/xmlui/ExerciseXmlAdapter.kt:38>

[media]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/data/MediaStorage.kt:22>

[exercise]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/model/Exercise.kt:6>

[workout-model]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/model/Workout.kt:6>

[scheduler]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/java/com/example/fitnessapp/data/ReminderScheduler.kt:30>

[list-xml]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/res/layout/screen_exercise_list_xml.xml>

[row-xml]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/res/layout/item_exercise_xml.xml>

[form-xml]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/res/layout/screen_exercise_form_xml.xml>

[detail-xml]: <C:/Users/PC1/AndroidStudioProjects/FitnessAppmyworkpart2/app/src/main/res/layout/screen_exercise_detail_xml.xml>

## Luồng Thống kê và Nhắc nhở sau khi ghép

```text
MainActivity (Trang chủ XML)
  → HomeXmlActivity.navigate("STATS")
  → StatisticsReminderXmlActivity
  → StatisticsFragment.load()
  → FitnessRepository(FitnessDatabase.open(context))
  → monthStats(month): workouts + workout_exercises + workout_sets
  → monthFrequency(month): workouts
  → chỉ số và FrequencyChartView

Trang chủ: home_reminder_edit / Thống kê: nút chuông
  → SettingsXmlActivity (reminder=true) / form activity_reminder_settings_xml
  → load(): FitnessRepository.getPrimaryReminder()
  → settings_time_picker chọn giờ; chọn DAILY hoặc WEEKLY và ngày
  → ReminderSaveModel giữ thao tác lưu qua việc tạo lại Activity
  → save(): FitnessRepository.saveReminder()
  → ReminderScheduler.schedule()
  → AlarmManager → ReminderReceiver
  → kiểm tra due/signature để bỏ báo thức cũ
  → Notification → MainActivity với extra reminder=true
  → HomeXmlActivity.openReminder()
```

HomeXmlModel chỉ đọc dữ liệu; HomeXmlActivity khôi phục lịch qua `ReminderScheduler.restore()` trên executor chung. Trả về từ màn hình Bài tập/Thống kê có extra `tab` để Trang chủ mở tab đích. Thống kê có thanh điều hướng dưới, Nhắc nhở ẩn thanh này. Bấm Back trên Nhắc nhở quay về Thống kê nếu mở từ chuông, hoặc Trang chủ nếu mở trực tiếp.

Các file mới nằm ở `xmlui`, giao diện ở `res/layout`. Đã xóa `ui/StatsScreen.kt` và hàm Compose `ReminderSettingsScreen`; `ui/FitnessMainApp.kt` chỉ còn cầu nối XML cho shell tham khảo cũ. Database mẫu và bộ cài media mẫu của project đích vẫn được giữ.
