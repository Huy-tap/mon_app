package com.example.fitnessapp.xmlui

import android.content.Context
import android.graphics.drawable.PictureDrawable
import android.widget.ImageView
import com.caverock.androidsvg.SVG

fun svg(context: Context, resource: Int) = PictureDrawable(SVG.getFromResource(context,resource).renderToPicture())
fun ImageView.asset(resource: Int) { scaleType=ImageView.ScaleType.FIT_CENTER;setLayerType(ImageView.LAYER_TYPE_SOFTWARE,null);setImageDrawable(svg(context,resource)) }
