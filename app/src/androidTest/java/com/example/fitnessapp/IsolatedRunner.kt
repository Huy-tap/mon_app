package com.example.fitnessapp

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import java.io.File

/** All instrumentation activities use a separate database, never the installed user database. */
class IsolatedRunner: AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader,name: String,context: Context): Application = super.newApplication(cl,IsolatedApplication::class.java.name,context)
}
class IsolatedApplication: Application() {
    override fun getDatabasePath(name: String): File = super.getDatabasePath(if(name=="fitness_app.db") "instrumentation_fitness.db" else name)
    override fun onCreate() { super.onCreate();getDatabasePath("fitness_app.db").delete() }
    override fun getSharedPreferences(name: String,mode: Int)=super.getSharedPreferences("instrumentation_$name",mode)
}
