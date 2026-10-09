package com.example.fitnessapp

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import android.widget.VideoView
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.controller.FitnessController
import com.example.fitnessapp.data.MediaStorage
import com.example.fitnessapp.xmlui.ExerciseXmlActivity
import com.example.fitnessapp.xmlui.ExerciseXmlModel
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** QA có thao tác trên camera/thư viện thật bằng cửa sổ hệ thống của máy ảo riêng. */
class MediaDeviceQaTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun ready(s: ActivityScenario<ExerciseXmlActivity>) {
        val end = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < end) {
            var ok = false
            s.onActivity { val m = ViewModelProvider(it)[ExerciseXmlModel::class.java]; ok = m.ready && !m.busy }
            if (ok) return
            Thread.sleep(100)
        }
        fail("Không tải được form bài tập")
    }
    private fun launch(name: String): ActivityScenario<ExerciseXmlActivity> {
        val s = ActivityScenario.launch<ExerciseXmlActivity>(Intent(context, ExerciseXmlActivity::class.java).putExtra("route", "add"))
        ready(s)
        onView(withId(R.id.ex_name_input)).perform(replaceText(name), closeSoftKeyboard())
        onView(withId(R.id.ex_muscle)).perform(click())
        onView(withText("Ngực")).perform(click())
        return s
    }
    private fun choose(video: Boolean, camera: Boolean) {
        onView(withId(if (video) R.id.ex_video_add else R.id.ex_image_add)).perform(scrollTo(), click())
        Log.i("FitnessQA", "WAIT_${if(camera) "CAMERA" else "PICKER"}_${if(video) "VIDEO" else "IMAGE"}")
        onView(withId(if (camera) R.id.ex_source_camera else R.id.ex_source_gallery)).perform(click())
    }
    private fun waitMedia(s: ActivityScenario<ExerciseXmlActivity>, video: Boolean): String {
        val end = System.currentTimeMillis() + 120000
        while (System.currentTimeMillis() < end) {
            var path: String? = null
            s.onActivity { val m = ViewModelProvider(it)[ExerciseXmlModel::class.java]
                if (!m.busy) path = m.form.getString(if (video) "video" else "image")
            }
            path?.let { return it }
            Thread.sleep(200)
        }
        fail("Chưa nhận kết quả ${if(video) "video" else "ảnh"} từ ứng dụng hệ thống")
        return ""
    }
    private fun previewAndSave(s: ActivityScenario<ExerciseXmlActivity>, name: String, photo: String, video: String) {
        assertNotNull(BitmapFactory.decodeFile(photo)); assertNotNull(MediaStorage.videoDuration(video))
        onView(withId(R.id.ex_image_preview)).perform(scrollTo(), click())
        onView(withId(R.id.ex_full_image)).check(matches(isDisplayed()))
        onView(withId(R.id.ex_back)).perform(click())
        onView(withId(R.id.ex_video_preview)).perform(scrollTo(), click())
        val end = System.currentTimeMillis() + 15000
        var playing = false
        while (System.currentTimeMillis() < end && !playing) {
            s.onActivity { playing = it.findViewById<VideoView>(R.id.ex_player)?.isPlaying == true }
            Thread.sleep(100)
        }
        assertTrue("Video không phát được trong giao diện", playing)
        onView(withId(R.id.ex_back)).perform(click())
        onView(withId(R.id.ex_save)).perform(click())
        val controller = FitnessController(context)
        val saveEnd = System.currentTimeMillis() + 15000
        while (controller.getAllExercises().none { it.name == name } && System.currentTimeMillis() < saveEnd) Thread.sleep(100)
        val e = controller.getAllExercises().first { it.name == name }
        assertEquals(photo, e.instructionImage); assertEquals(video, e.instructionVideo)
    }
    @Test fun cameraPhotoAndVideoCanBeCapturedPreviewedAndSaved() {
        val name = "QA Camera ${System.nanoTime()}"
        launch(name).use { s ->
            choose(false, true); val photo = waitMedia(s, false)
            choose(true, true); val video = waitMedia(s, true)
            previewAndSave(s, name, photo, video)
        }
    }
    @Test fun galleryPhotoAndVideoCanBeSelectedPreviewedAndSaved() {
        fun publish(asset: String, collection: android.net.Uri, mime: String): android.net.Uri {
            val uri = context.contentResolver.insert(collection, ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "fitness_qa_${asset.substringAfterLast('/')}")
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            })!!
            context.contentResolver.openOutputStream(uri)!!.use { out -> context.assets.open(asset).use { it.copyTo(out) } }
            context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        }
        val photoUri = publish("media/images/benchpress.png", MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/png")
        val videoUri = publish("media/videos/benchpress.mp4", MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/mp4")
        try {
            val name = "QA Gallery ${System.nanoTime()}"
            launch(name).use { s ->
                choose(false, false); val photo = waitMedia(s, false)
                choose(true, false); val video = waitMedia(s, true)
                context.assets.open("media/images/benchpress.png").use { assertArrayEquals(it.readBytes(), File(photo).readBytes()) }
                context.assets.open("media/videos/benchpress.mp4").use { assertArrayEquals(it.readBytes(), File(video).readBytes()) }
                previewAndSave(s, name, photo, video)
            }
        } finally {
            context.contentResolver.delete(photoUri, null, null); context.contentResolver.delete(videoUri, null, null)
        }
    }
}
