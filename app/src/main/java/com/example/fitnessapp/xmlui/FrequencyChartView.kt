package com.example.fitnessapp.xmlui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.fitnessapp.R

/** Native, responsive rendering of Figma's four frequency columns. */
class FrequencyChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var counts = listOf(0L, 0L, 0L, 0L)
    private val regular = ResourcesCompat.getFont(context, R.font.inter_regular)
    private val bold = ResourcesCompat.getFont(context, R.font.inter_bold)
    private val density get() = resources.displayMetrics.density

    fun setCounts(value: List<Long>) {
        require(value.size == 4 && value.all { it >= 0 })
        counts = value.toList()
        contentDescription = counts.mapIndexed { i, count ->
            context.getString(R.string.statistics_week_accessibility, i + 1, count)
        }.joinToString(", ")
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gap = 12 * density
        val columnWidth = (width - 3 * gap) / 4f
        if (columnWidth <= 0) return
        paint.textSize = 11 * resources.displayMetrics.scaledDensity
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = regular
        val fm = paint.fontMetrics
        val labelBaseline = height - fm.descent
        val bottom = labelBaseline + fm.ascent - 8 * density
        val available = (bottom - (fm.descent - fm.ascent) - 8 * density).coerceAtLeast(0f)
        val maximum = counts.maxOrNull() ?: 0L
        // Figma's sample uses 12dp per session; scale large values to fit the same card.
        val unit = if (maximum > 0) minOf(12 * density, available / maximum) else 0f
        counts.forEachIndexed { index, count ->
            val left = index * (columnWidth + gap)
            val top = bottom - count * unit
            paint.color = Color.parseColor(if (count == maximum && count > 0) "#111827" else "#E5E7EB")
            path.reset()
            val radius = minOf(6 * density, (bottom - top) / 2)
            path.addRoundRect(RectF(left, top, left + columnWidth, bottom),
                floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f), Path.Direction.CW)
            canvas.drawPath(path, paint)
            paint.typeface = bold
            paint.color = Color.parseColor("#111827")
            canvas.drawText(count.toString(), left + columnWidth / 2, top - 8 * density - paint.fontMetrics.descent, paint)
            paint.typeface = regular
            paint.color = Color.parseColor("#6B7280")
            canvas.drawText(context.getString(R.string.statistics_week, index + 1), left + columnWidth / 2, labelBaseline, paint)
        }
    }
}
