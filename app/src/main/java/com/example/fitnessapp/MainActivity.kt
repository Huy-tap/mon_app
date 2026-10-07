package com.example.fitnessapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fitnessapp.databinding.ActivityMainBinding
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.ui.*

class MainActivity: AppCompatActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view,insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);insets
        }
        if(state==null) {
            supportFragmentManager.beginTransaction().replace(R.id.container,StatisticsFragment()).commitNow()
            if(intent.getBooleanExtra("reminder",false)) openReminder()
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if(intent.getBooleanExtra("reminder",false) && supportFragmentManager.findFragmentById(R.id.container) !is ReminderFragment) openReminder()
    }
    fun openReminder() { supportFragmentManager.beginTransaction().replace(R.id.container,ReminderFragment()).addToBackStack("reminder").commit() }
    override fun onResume() { super.onResume();ReminderScheduler.restore(applicationContext) }
}
