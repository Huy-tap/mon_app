package com.example.fitnessapp.xmlui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fitnessapp.R
import com.example.fitnessapp.data.ReminderScheduler

/** Khung XML cho Thống kê và Nhắc nhở, dùng chung điều hướng với Trang chủ. */
class StatisticsReminderXmlActivity : AppCompatActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_statistics_reminder_xml)
        val root = findViewById<View>(R.id.module_root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            findViewById<View>(R.id.module_bottom_inset).apply {
                layoutParams = layoutParams.apply { height = maxOf(bars.bottom, keyboard.bottom) }
            }
            insets
        }
        XmlNavigation.bind(this, "STATS", false) { tab ->
            setResult(RESULT_OK, Intent().putExtra("tab", tab))
            finish()
        }
        supportFragmentManager.addOnBackStackChangedListener { renderNavigation() }
        if (state == null) {
            val first = if (intent.getBooleanExtra("reminder", false)) ReminderFragment() else StatisticsFragment()
            supportFragmentManager.beginTransaction().replace(R.id.container, first).commitNow()
        }
        renderNavigation()
    }

    fun openReminder() {
        if (supportFragmentManager.findFragmentById(R.id.container) is ReminderFragment) return
        supportFragmentManager.beginTransaction().replace(R.id.container, ReminderFragment())
            .addToBackStack("reminder").commit()
    }

    fun closeReminder() {
        if (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStack()
        else finish()
    }

    private fun renderNavigation() {
        findViewById<View>(R.id.module_navigation).visibility =
            if (supportFragmentManager.findFragmentById(R.id.container) is ReminderFragment) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        ReminderScheduler.restore(applicationContext)
    }
}
