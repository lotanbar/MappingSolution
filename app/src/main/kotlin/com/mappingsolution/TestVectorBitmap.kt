package com.mappingsolution

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

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

/**
 * Source type for circle icon borders.
 * - GOOGLE  → colourful sweep gradient (Google brand colours)
 * - OSM     → solid yellow border
 * - BULK    → solid purple border
 * - USER    → solid border using [borderColorHex]
 */
enum class PoiSource { GOOGLE, OSM, BULK, USER }

/**
 * Background colours keyed by icon key — same icon key as in IconCatalog.
 * Icons not listed fall back to [DEFAULT_BG].
 */
private val ICON_BG_COLORS: Map<String, Int> = mapOf(
    // Location
    "place"          to 0xFF5C6BC0.toInt(),
    "location_on"    to 0xFF5C6BC0.toInt(),
    "my_location"    to 0xFF5C6BC0.toInt(),
    "explore"        to 0xFF5C6BC0.toInt(),
    "travel_explore" to 0xFF5C6BC0.toInt(),
    "navigation"     to 0xFF5C6BC0.toInt(),
    "near_me"        to 0xFF5C6BC0.toInt(),
    "gps_fixed"      to 0xFF5C6BC0.toInt(),
    "flag"           to 0xFF5C6BC0.toInt(),
    "tour"           to 0xFF5C6BC0.toInt(),
    "map"            to 0xFF5C6BC0.toInt(),
    "push_pin"       to 0xFF5C6BC0.toInt(),
    "satellite"      to 0xFF5C6BC0.toInt(),
    "location_city"  to 0xFF5C6BC0.toInt(),
    // Nature
    "park"           to 0xFF388E3C.toInt(),
    "terrain"        to 0xFF558B2F.toInt(),
    "waves"          to 0xFF0288D1.toInt(),
    "water_drop"     to 0xFF0288D1.toInt(),
    "landscape"      to 0xFF558B2F.toInt(),
    "nature"         to 0xFF388E3C.toInt(),
    "grass"          to 0xFF388E3C.toInt(),
    "forest"         to 0xFF2E7D32.toInt(),
    "spa"            to 0xFF7B1FA2.toInt(),
    "filter_vintage" to 0xFF7B1FA2.toInt(),
    "eco"            to 0xFF388E3C.toInt(),
    "ac_unit"        to 0xFF0288D1.toInt(),
    "wb_sunny"       to 0xFFF57F17.toInt(),
    "cloud"          to 0xFF607D8B.toInt(),
    // Food & Drink
    "restaurant"     to 0xFFE53935.toInt(),
    "local_cafe"     to 0xFF795548.toInt(),
    "local_bar"      to 0xFFAD1457.toInt(),
    "fastfood"       to 0xFFE53935.toInt(),
    "lunch_dining"   to 0xFFE53935.toInt(),
    "dinner_dining"  to 0xFFE53935.toInt(),
    "brunch_dining"  to 0xFFE53935.toInt(),
    "bakery_dining"  to 0xFFBF360C.toInt(),
    "ramen_dining"   to 0xFFE53935.toInt(),
    "local_pizza"    to 0xFFE53935.toInt(),
    "icecream"       to 0xFFEC407A.toInt(),
    "cake"           to 0xFFEC407A.toInt(),
    "wine_bar"       to 0xFFAD1457.toInt(),
    "coffee"         to 0xFF795548.toInt(),
    // Activities
    "directions_walk"  to 0xFF00897B.toInt(),
    "directions_run"   to 0xFF00897B.toInt(),
    "directions_bike"  to 0xFF00897B.toInt(),
    "hiking"           to 0xFF558B2F.toInt(),
    "fitness_center"   to 0xFF1565C0.toInt(),
    "pool"             to 0xFF0288D1.toInt(),
    "sailing"          to 0xFF0288D1.toInt(),
    "kayaking"         to 0xFF0288D1.toInt(),
    "snowboarding"     to 0xFF0288D1.toInt(),
    "downhill_skiing"  to 0xFF0288D1.toInt(),
    "surfing"          to 0xFF0288D1.toInt(),
    "sports_soccer"    to 0xFF2E7D32.toInt(),
    "sports_basketball" to 0xFFE65100.toInt(),
    "golf_course"      to 0xFF388E3C.toInt(),
    "paragliding"      to 0xFF1565C0.toInt(),
    // Accommodation
    "hotel"          to 0xFF6A1B9A.toInt(),
    "home"           to 0xFF1565C0.toInt(),
    "apartment"      to 0xFF1565C0.toInt(),
    "house"          to 0xFF1565C0.toInt(),
    "night_shelter"  to 0xFF6A1B9A.toInt(),
    "beach_access"   to 0xFF0288D1.toInt(),
    "king_bed"       to 0xFF6A1B9A.toInt(),
    "single_bed"     to 0xFF6A1B9A.toInt(),
    "meeting_room"   to 0xFF1565C0.toInt(),
    // Transport
    "directions_car"  to 0xFF455A64.toInt(),
    "directions_bus"  to 0xFF455A64.toInt(),
    "train"           to 0xFF455A64.toInt(),
    "flight"          to 0xFF1565C0.toInt(),
    "motorcycle"      to 0xFF455A64.toInt(),
    "two_wheeler"     to 0xFF455A64.toInt(),
    "electric_car"    to 0xFF00897B.toInt(),
    "directions_boat" to 0xFF0288D1.toInt(),
    "anchor"          to 0xFF0288D1.toInt(),
    "local_taxi"      to 0xFFF57F17.toInt(),
    "tram"            to 0xFF455A64.toInt(),
    // Services
    "local_hospital"        to 0xFFE53935.toInt(),
    "local_pharmacy"        to 0xFF2E7D32.toInt(),
    "local_gas_station"     to 0xFF455A64.toInt(),
    "local_parking"         to 0xFF1565C0.toInt(),
    "shopping_cart"         to 0xFF00897B.toInt(),
    "storefront"            to 0xFF00897B.toInt(),
    "local_atm"             to 0xFF1B5E20.toInt(),
    "account_balance"       to 0xFF1B5E20.toInt(),
    "school"                to 0xFFF57F17.toInt(),
    "local_police"          to 0xFF1A237E.toInt(),
    "local_fire_department" to 0xFFB71C1C.toInt(),
    "local_laundry"         to 0xFF455A64.toInt(),
    // Entertainment
    "museum"          to 0xFF6A1B9A.toInt(),
    "music_note"      to 0xFFAD1457.toInt(),
    "nightlife"       to 0xFF6A1B9A.toInt(),
    "theaters"        to 0xFF6A1B9A.toInt(),
    "casino"          to 0xFF1B5E20.toInt(),
    "sports_bar"      to 0xFFAD1457.toInt(),
    "sports_esports"  to 0xFF1565C0.toInt(),
    "photo_camera"    to 0xFF455A64.toInt(),
    "attractions"     to 0xFFE65100.toInt(),
    // Markers
    "star"            to 0xFFF57F17.toInt(),
    "favorite"        to 0xFFE53935.toInt(),
    "bookmark"        to 0xFF1565C0.toInt(),
    "label"           to 0xFF455A64.toInt(),
    "warning"         to 0xFFF57F17.toInt(),
    "info"            to 0xFF1565C0.toInt(),
    "emergency"       to 0xFFB71C1C.toInt(),
    "whatshot"        to 0xFFE65100.toInt(),
    "bolt"            to 0xFFF57F17.toInt(),
    "visibility"      to 0xFF1565C0.toInt(),
    "work"            to 0xFF455A64.toInt(),
    "business_center" to 0xFF455A64.toInt(),
)

private val DEFAULT_BG = 0xFF5C6BC0.toInt()

// Google brand sweep gradient colours — kept for potential future use
private val GOOGLE_GRADIENT_COLORS = intArrayOf(
    0xFF4285F4.toInt(),
    0xFF34A853.toInt(),
    0xFFFBBC05.toInt(),
    0xFFEA4335.toInt(),
    0xFF4285F4.toInt(),
)

/**
 * Creates a square bitmap of [size]×[size] showing a filled circle with
 * background colour from [ICON_BG_COLORS] keyed by [iconKey].
 * The white icon is drawn on top by the Compose-side helper [createPoiCircle] in MapComponent.
 */
fun createCircleIcon(
    iconKey: String,
    source: PoiSource,
    borderColorHex: String = "#FFFFFF",
    size: Int = 80,
): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val borderWidth = size * 0.08f
    val outerR = size / 2f - 1f
    val innerR = outerR - borderWidth

    val bgColor = ICON_BG_COLORS[iconKey] ?: DEFAULT_BG
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, outerR, bgPaint)

    return bitmap
}

