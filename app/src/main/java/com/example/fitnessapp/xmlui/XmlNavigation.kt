package com.example.fitnessapp.xmlui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.PictureDrawable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.caverock.androidsvg.SVG
import com.example.fitnessapp.R

object FeatureAvailability {
    // TODO: Trang này chưa phát triển: Theo dõi tập luyện, Lịch sử, Thống kê, Cài đặt.
    // Chỉ mở từng module sau khi hoàn thiện giao diện XML và kiểm thử chức năng.
    val availableTabs = setOf("HOME", "EXERCISES", "HISTORY")
    fun showUnavailable(context: Context) {
        AlertDialog.Builder(context).setMessage("Trang này chưa phát triển")
            .setPositiveButton("Đóng", null).show()
    }
}

/** Thanh điều hướng dùng chung, hỗ trợ các tab đã phát triển. */
object XmlNavigation {
    fun bind(activity: Activity, selected: String, dark: Boolean, navigate: (String) -> Unit) {
        val tabs = listOf("HOME", "EXERCISES", "HISTORY", "STATS", "SETTINGS")
        val buttons = listOf(R.id.ex_tab_HOME, R.id.ex_tab_EXERCISES, R.id.ex_tab_HISTORY, R.id.ex_tab_STATS, R.id.ex_tab_SETTINGS)
        val icons = listOf(R.id.ex_icon_HOME, R.id.ex_icon_EXERCISES, R.id.ex_icon_HISTORY, R.id.ex_icon_STATS, R.id.ex_icon_SETTINGS)
        val labels = listOf(R.id.ex_tab_label_HOME, R.id.ex_tab_label_EXERCISES, R.id.ex_tab_label_HISTORY, R.id.ex_tab_label_STATS, R.id.ex_tab_label_SETTINGS)
        val files = if (dark) listOf("ad0de.svg", "ce785.svg", "7d27d.svg", "e6548.svg", "e307c.svg") else listOf(
            if (selected == "HOME") "190c1.svg" else "04aea.svg",
            if (selected == "EXERCISES") "c6120.svg" else "2f6cd.svg",
            if (selected == "HISTORY") "16b4a.svg" else "74755.svg",
            "ee2a5.svg",
            "1519d.svg"
        )
        tabs.forEachIndexed { index, tab ->
            activity.findViewById<ImageView>(icons[index]).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                setImageDrawable(activity.assets.open("figma/${files[index]}").use { PictureDrawable(SVG.getFromInputStream(it).renderToPicture()) })
            }
            activity.findViewById<View>(buttons[index]).apply {
                isSelected = tab == selected
                setOnClickListener {
                    if (tab !in FeatureAvailability.availableTabs) FeatureAvailability.showUnavailable(activity)
                    else if (tab != selected) navigate(tab)
                }
            }
            if (tab == selected) activity.findViewById<TextView>(labels[index]).apply {
                setTextColor(TypedValue().also { activity.theme.resolveAttribute(R.attr.exAccent, it, true) }.data)
                setTypeface(typeface, Typeface.BOLD)
            }
        }
    }
}
