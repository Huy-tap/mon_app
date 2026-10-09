package com.example.fitnessapp

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.data.BundledExerciseMedia
import com.example.fitnessapp.data.FitnessDatabase
import com.example.fitnessapp.data.MediaStorage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class BundledExerciseMediaTest {
    private lateinit var folder: File
    private lateinit var dbFile: File
    private lateinit var db: SQLiteDatabase
    private lateinit var context: Context

    @Before fun setup() {
        val real = InstrumentationRegistry.getInstrumentation().targetContext
        folder = File.createTempFile("media-test-", "", real.cacheDir).apply { delete(); mkdirs() }
        context = object : ContextWrapper(real) {
            override fun getExternalFilesDir(type: String?): File = File(folder, type ?: "files").apply { mkdirs() }
        }
        dbFile = File(folder, "test.db")
        real.assets.open("fitness_app.db").use { input -> dbFile.outputStream().use { input.copyTo(it) } }
        db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        FitnessDatabase.migrate(db)
    }
    @After fun cleanup() { db.close(); folder.deleteRecursively() }
    private fun field(column: String, id: Long = 1) = db.rawQuery(
        "SELECT $column FROM exercises WHERE exercise_id = ?", arrayOf(id.toString())
    ).use { it.moveToFirst(); it.getString(0) }
    private fun countWorkouts() = db.rawQuery("SELECT count(*) FROM workouts", null).use { it.moveToFirst(); it.getInt(0) }

    @Test fun freshInstallCopiesPlayableMediaAndSurvivesReopen() {
        val workouts = countWorkouts()
        BundledExerciseMedia.install(context, db)
        val image = field("instruction_image")!!
        val video = field("instruction_video")!!
        assertTrue(File(image).isFile)
        assertTrue(File(video).isFile)
        assertNotNull(BitmapFactory.decodeFile(image))
        assertNotNull(MediaStorage.videoDuration(video))
        context.assets.open("media/images/pushup.png").use { assertArrayEquals(it.readBytes(), File(image).readBytes()) }
        context.assets.open("media/videos/pushup.mp4").use { assertArrayEquals(it.readBytes(), File(video).readBytes()) }
        val modified = File(image).lastModified()
        db.close()
        db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        BundledExerciseMedia.install(context, db)
        assertEquals(image, field("instruction_image"))
        assertEquals(modified, File(image).lastModified())
        assertEquals(workouts, countWorkouts())
    }

    @Test fun upgradeFillsLegacyAndEmptyPathsByIdEvenAfterRename() {
        db.execSQL("UPDATE exercises SET name='Tên đã đổi', instruction_image='/uploads/images/pushup.png', instruction_video=NULL WHERE exercise_id=1")
        BundledExerciseMedia.install(context, db)
        assertTrue(File(field("instruction_image")!!).isFile)
        assertTrue(File(field("instruction_video")!!).isFile)
        assertEquals("Tên đã đổi", field("name"))
    }

    @Test fun existingUserMediaIsPreservedEvenWhenFileUnavailable() {
        db.execSQL("UPDATE exercises SET instruction_image='/user/photo.jpg', instruction_video='/user/video.mp4' WHERE exercise_id=1")
        BundledExerciseMedia.install(context, db)
        assertEquals("/user/photo.jpg", field("instruction_image"))
        assertEquals("/user/video.mp4", field("instruction_video"))
        db.execSQL("UPDATE exercises SET instruction_image=NULL WHERE exercise_id=1")
        BundledExerciseMedia.install(context, db)
        assertNull(field("instruction_image"))
    }

    @Test fun replacingOrRemovingInstalledSampleIsNotUndone() {
        BundledExerciseMedia.install(context, db)
        db.execSQL("UPDATE exercises SET instruction_image='/user/new.png', instruction_video=NULL WHERE exercise_id=1")
        BundledExerciseMedia.install(context, db)
        assertEquals("/user/new.png", field("instruction_image"))
        assertNull(field("instruction_video"))
    }

    @Test fun missingManagedFileIsRecoveredButArchivedExerciseIsUntouched() {
        BundledExerciseMedia.install(context, db)
        val path = field("instruction_image")!!
        File(path).delete()
        BundledExerciseMedia.install(context, db)
        assertTrue(File(path).isFile)
        db.execSQL("UPDATE exercises SET is_archived=1, instruction_image=NULL WHERE exercise_id=1")
        BundledExerciseMedia.install(context, db)
        assertNull(field("instruction_image"))
        assertEquals("1", field("is_archived"))
    }

    @Test fun mappingWorksForAnotherIdAndMissingAssetCanBeRetried() {
        val missing = BundledExerciseMedia.Entry(4, "instruction_image", "media/images/not-present.png")
        val valid = BundledExerciseMedia.Entry(4, "instruction_image", "media/images/pushup.png")
        BundledExerciseMedia.installEntries(context, db, listOf(missing))
        assertNull(field("instruction_image", 4))
        BundledExerciseMedia.installEntries(context, db, listOf(valid))
        assertTrue(File(field("instruction_image", 4)!!).isFile)
        assertEquals("Squat", field("name", 4))
        // Không chấp nhận đường dẫn thoát ra ngoài thư mục mẫu.
        val before = field("instruction_image", 2)
        BundledExerciseMedia.installEntries(context, db, listOf(BundledExerciseMedia.Entry(2, "instruction_image", "media/images/../pushup.png")))
        assertEquals(before, field("instruction_image", 2))
    }
}
