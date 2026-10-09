package com.example.fitnessapp

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import java.io.File

/** Kiểm thử dùng database, cấu hình và media riêng, không sửa dữ liệu app đang sử dụng. */
class IsolatedRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, name: String, context: Context): Application =
        super.newApplication(cl, IsolatedApplication::class.java.name, context)
}

class IsolatedApplication : Application() {
    override fun getDatabasePath(name: String): File =
        super.getDatabasePath(if (name == "fitness_app.db") "instrumentation_fitness.db" else name)

    override fun onCreate() {
        super.onCreate()
        getDatabasePath("fitness_app.db").delete()
    }

    override fun getSharedPreferences(name: String, mode: Int) =
        super.getSharedPreferences("instrumentation_$name", mode)

    override fun getExternalFilesDir(type: String?): File? =
        super.getExternalFilesDir(null)?.let { File(it, "instrumentation_media/${type ?: "files"}").apply { mkdirs() } }
}
