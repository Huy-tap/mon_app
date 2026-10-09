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
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppTheme.wrap(newBase))
    }

    override fun onCreate(state: Bundle?) {
        val dark = AppTheme.isDark(this)
        delegate.localNightMode = if (dark) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_statistics_reminder_xml)
        val root = findViewById<View>(R.id.module_root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
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
        XmlNavigation.bind(this, "STATS", dark) { tab ->
            setResult(RESULT_OK, Intent().putExtra("tab", tab))
            finish()
        }
        if (state == null) {
            supportFragmentManager.beginTransaction().replace(R.id.container, StatisticsFragment()).commitNow()
            if (intent.getBooleanExtra("reminder", false)) openReminder()
        }
    }

    fun openReminder() {
        startActivity(Intent(this, SettingsXmlActivity::class.java).putExtra("reminder", true))
    }

    override fun onResume() {
        super.onResume()
        if (AppTheme.needsRefresh(this)) { recreate(); return }
        ReminderScheduler.restore(applicationContext)
    }
}
