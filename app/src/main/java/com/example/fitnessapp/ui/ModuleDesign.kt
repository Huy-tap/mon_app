package com.example.fitnessapp.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ExperimentalTextApi
import com.example.fitnessapp.R

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resource: Int) = FontFamily(listOf(400, 500, 600, 700, 800).map { weight ->
    Font(resource, weight = FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
})

val StatsFont = variableFamily(R.font.inter)
val ReminderFont = variableFamily(R.font.manrope)
val ReminderHeadingFont = variableFamily(R.font.outfit)
