package com.example.fitnessapp.xmlui

import android.content.Context
import android.content.res.Configuration
import com.example.fitnessapp.controller.FitnessController

/** One persisted choice for all XML activities, dialogs and resource qualifiers. */
object AppTheme {
    fun isDark(context: Context): Boolean = FitnessController(context.applicationContext).getState("dark_theme") == "true"

    fun save(context: Context, dark: Boolean) {
        FitnessController(context.applicationContext).setState("dark_theme", dark.toString())
    }

    fun wrap(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (isDark(context)) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        return context.createConfigurationContext(config)
    }

    fun needsRefresh(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) != isDark(context)
}
