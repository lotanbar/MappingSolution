package com.mappingsolution.data.map

import android.database.sqlite.SQLiteDatabase
import android.util.Log

/**
 * Reads tiles from a single `.mbtiles` file.
 *
 * MBTiles stores tiles in TMS convention (y=0 at the bottom). MapLibre expects XYZ convention
 * (y=0 at the top), so the y coordinate is flipped before querying.
 */
class MbTilesReader(filePath: String) : AutoCloseable {

    private val db: SQLiteDatabase = SQLiteDatabase.openDatabase(
        filePath,
        null,
        SQLiteDatabase.OPEN_READONLY,
    )

    fun readMetadata(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        db.rawQuery("SELECT name, value FROM metadata", null).use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getString(0)] = cursor.getString(1)
            }
        }
        return result
    }

    /**
     * Returns the raw tile blob for the given XYZ tile coordinate, or null if the tile
     * does not exist in the archive.
     */
    fun getTile(z: Int, x: Int, y: Int): ByteArray? {
        val tmsY = (1 shl z) - 1 - y
        return try {
            db.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?",
                arrayOf(z.toString(), x.toString(), tmsY.toString()),
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getBlob(0) else null
            }
        } catch (e: Exception) {
            Log.w("MbTilesReader", "Failed to read tile z=$z x=$x y=$y", e)
            null
        }
    }

    override fun close() {
        try { db.close() } catch (_: Exception) {}
    }
}
