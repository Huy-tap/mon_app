package com.example.fitnessapp

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.fitnessapp.data.ReminderScheduler
import com.example.fitnessapp.ui.FitnessMainApp

class MainActivity : ComponentActivity() {
    private var recordRequest by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        recordRequest = if (savedInstanceState == null) recordRequest(intent) else savedInstanceState.getString("recordRequest")
        setContent {
            FitnessMainApp(recordRequest, { recordRequest = null; intent.action = Intent.ACTION_MAIN })
        }
    }
    private fun recordRequest(intent: Intent): String? = if (intent.action == ReminderScheduler.RECORD) intent.getStringExtra("reminderIdentity") else null
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recordRequest = recordRequest(intent)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("recordRequest", recordRequest)
        super.onSaveInstanceState(outState)
    }
}
