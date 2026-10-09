package com.example.fitnessapp.xmlui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import com.caverock.androidsvg.SVG
import com.example.fitnessapp.R

/** Bitmap-backed SVGs support Android tinting; PictureDrawable ignores color filters. */
fun svg(context: Context, resource: Int): BitmapDrawable = renderSvg(context, SVG.getFromResource(context, resource))

fun renderSvg(context: Context, image: SVG): BitmapDrawable {
    val picture = image.renderToPicture()
    val density = context.resources.displayMetrics.density
    val bitmap = Bitmap.createBitmap((picture.width * density).toInt().coerceAtLeast(1),
        (picture.height * density).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    Canvas(bitmap).apply { scale(density, density); drawPicture(picture) }
    return BitmapDrawable(context.resources, bitmap)
}

fun ImageView.asset(resource: Int) {
    scaleType = ImageView.ScaleType.FIT_CENTER
    setImageDrawable(svg(context, resource))
    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        setColorFilter(context.getColor(R.color.ink))
    else clearColorFilter()
}
