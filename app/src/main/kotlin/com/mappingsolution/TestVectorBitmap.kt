package com.mappingsolution

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

fun createPinBitmap(
    colorHex: String,
    width: Int = 120,
    height: Int = 160
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val pinColor = try {
        Color.parseColor(colorHex)
    } catch (e: Exception) {
        Color.BLUE
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pinColor
        style = Paint.Style.FILL
    }
    
    // Draw classic teardrop pin shape
    val path = Path().apply {
        val radius = width / 2f
        val cx = radius
        val cy = radius
        
        moveTo(cx, height.toFloat()) // bottom point
        
        // Curves to the top circle
        cubicTo(cx, height.toFloat() * 0.7f, 0f, cy * 1.5f, 0f, cy)
        arcTo(android.graphics.RectF(0f, 0f, width.toFloat(), width.toFloat()), 180f, 180f)
        cubicTo(width.toFloat(), cy * 1.5f, cx, height.toFloat() * 0.7f, cx, height.toFloat())
        close()
    }
    canvas.drawPath(path, paint)

    // Add a darker stroke for definition
    paint.apply {
        color = Color.BLACK
        alpha = 60
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawPath(path, paint)

    // Draw white inner circle for the icon
    paint.apply {
        color = Color.WHITE
        alpha = 255
        style = Paint.Style.FILL
    }
    canvas.drawCircle(width / 2f, width / 2f, width / 2.6f, paint)
    
    return bitmap
}
