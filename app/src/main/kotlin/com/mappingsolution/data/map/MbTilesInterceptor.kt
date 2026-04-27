package com.mappingsolution.data.map

import android.util.Log
import com.mappingsolution.data.fs.RasterLayerRepository
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that handles tile requests for local `.mbtiles` files.
 *
 * Tile URLs follow the pattern: `http://mbtiles-local/{layerId}/{z}/{x}/{y}`
 *
 * All other requests pass through unmodified.
 */
@Singleton
class MbTilesInterceptor @Inject constructor(
    private val rasterLayerRepository: RasterLayerRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        if (url.host != HOST) return chain.proceed(request)

        val segments = url.pathSegments
        if (segments.size != 4) return errorResponse(request, "Invalid mbtiles URL: $url")

        val (layerId, zStr, xStr, yStr) = segments
        val z = zStr.toIntOrNull() ?: return errorResponse(request, "Invalid z: $zStr")
        val x = xStr.toIntOrNull() ?: return errorResponse(request, "Invalid x: $xStr")
        val y = yStr.toIntOrNull() ?: return errorResponse(request, "Invalid y: $yStr")

        val layer = rasterLayerRepository.observeAll().let {
            rasterLayerRepository.findById(layerId)
        } ?: return notFoundResponse(request, "Layer not found: $layerId")

        return try {
            MbTilesReader(layer.filePath).use { reader ->
                val tileData = reader.getTile(z, x, y)
                    ?: return notFoundResponse(request, "Tile not found z=$z x=$x y=$y in $layerId")

                val mediaType = when {
                    tileData.isPng() -> "image/png"
                    tileData.isJpeg() -> "image/jpeg"
                    tileData.isWebP() -> "image/webp"
                    else -> "application/octet-stream"
                }.toMediaType()

                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(tileData.toResponseBody(mediaType))
                    .build()
            }
        } catch (e: Exception) {
            Log.e("MbTilesInterceptor", "Error reading tile", e)
            errorResponse(request, "Error reading tile: ${e.message}")
        }
    }

    private fun errorResponse(request: okhttp3.Request, message: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message(message)
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

    private fun notFoundResponse(request: okhttp3.Request, message: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message(message)
            .body("".toResponseBody("text/plain".toMediaType()))
            .build()

    companion object {
        const val HOST = "mbtiles-local"
    }
}

private fun ByteArray.isPng() = size >= 4 && this[0] == 0x89.toByte() && this[1] == 0x50.toByte()
private fun ByteArray.isJpeg() = size >= 3 && this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte()
private fun ByteArray.isWebP() = size >= 12 && this[0] == 0x52.toByte() && this[3] == 0x46.toByte()
