package com.example.fitnessapp

import android.app.NotificationManager
import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fitnessapp.data.*
import com.example.fitnessapp.xmlui.SettingsXmlActivity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RealAlarmDeviceQaTest {
    @Test fun savedDailyAlarmActuallyFiresAtScheduledTime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("QA cần cấp quyền thông báo", ReminderScheduler.permitted(context))
        assertTrue("QA cần cấp quyền báo thức chính xác", ReminderScheduler.exact(context))
        val repository = FitnessRepository(FitnessDatabase.open(context))
        val original = repository.getPrimaryReminder()
        val notifications = context.getSystemService(NotificationManager::class.java)
        context.getSharedPreferences("permission", 0).edit().putBoolean("asked", true).commit()
        notifications.cancel(41)
        try {
            repository.saveReminder(original.copy(isEnabled = false))
            ActivityScenario.launch<SettingsXmlActivity>(Intent(context, SettingsXmlActivity::class.java)
                .putExtra("reminder", true)).use { s ->
                val readyEnd = System.currentTimeMillis() + 15000
                var ready = false
                while (!ready && System.currentTimeMillis() < readyEnd) {
                    s.onActivity { ready = it.findViewById<TextView>(R.id.reminder_save).isEnabled }
                    Thread.sleep(100)
                }
                assertTrue(ready)
                onView(withId(R.id.reminder_daily)).perform(scrollTo(), click())
                val target = LocalDateTime.now().plusMinutes(2).withSecond(0).withNano(0)
                onView(withId(R.id.reminder_edit)).perform(scrollTo(), click())
                onView(withId(R.id.picker_hour)).perform(replaceText(target.format(DateTimeFormatter.ofPattern("HH"))))
                onView(withId(R.id.picker_minute)).perform(replaceText(target.format(DateTimeFormatter.ofPattern("mm"))), closeSoftKeyboard())
                onView(withId(R.id.picker_confirm)).perform(click())
                onView(withId(R.id.reminder_toggle)).perform(scrollTo(), click())
                onView(withId(R.id.reminder_save)).perform(click())
                val delivery = context.applicationContext.getSharedPreferences("alarm_delivery", 0)
                val saveEnd = System.currentTimeMillis() + 10000
                while (delivery.getLong("due", 0) == 0L && System.currentTimeMillis() < saveEnd) Thread.sleep(100)
                val expected = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                assertEquals(expected, delivery.getLong("due", 0))
                android.util.Log.i("FitnessQA", "REAL_ALARM_WAIT_UNTIL $target")
                while (notifications.activeNotifications.none { it.id == 41 } && System.currentTimeMillis() < expected + 15000) Thread.sleep(500)
                val notification = notifications.activeNotifications.firstOrNull { it.id == 41 }
                assertNotNull("Đến giờ nhưng không có thông báo", notification)
                assertEquals("Đến giờ tập luyện! 💪", notification!!.notification.extras.getString("android.title"))
                assertTrue("Báo thức xuất hiện quá sớm", System.currentTimeMillis() >= expected)
                android.util.Log.i("FitnessQA", "REAL_ALARM_RECEIVED_SHOWING_FOR_REVIEW")
                Thread.sleep(20000)
            }
        } finally {
            notifications.cancel(41)
            repository.saveReminder(original)
            ReminderScheduler.restore(context.applicationContext)
        }
    }
}
