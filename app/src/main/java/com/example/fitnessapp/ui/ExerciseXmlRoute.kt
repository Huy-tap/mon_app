package com.example.fitnessapp.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.fitnessapp.xmlui.ExerciseXmlActivity

/** Temporary navigation bridge from the remaining Compose modules into the XML module. */
@Composable
fun ExerciseXmlRoute(route: String, dark: Boolean, onClose: (String?) -> Unit) {
    val context = LocalContext.current
    var launched by rememberSaveable { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Block a late effect in this composition, but allow a later visit to launch again.
        completed = true
        launched = false
        onClose(result.data?.getStringExtra("tab"))
    }
    LaunchedEffect(Unit) {
        if (!launched && !completed) {
            launched = true
            launcher.launch(Intent(context, ExerciseXmlActivity::class.java).putExtra("route", route).putExtra("dark", dark))
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
