package com.mappingsolution.data.util

import android.os.Environment
import java.io.File

class StorageManager {

    private val rootMediaDir = File(Environment.getExternalStorageDirectory(), "mapping-solution-assets")

    init {
        if (!rootMediaDir.exists()) {
            rootMediaDir.mkdirs()
        }
    }

    fun getDirForPoi(poiId: Long): File {
        val poiDir = File(rootMediaDir, "media/pois/$poiId")
        if (!poiDir.exists()) {
            poiDir.mkdirs()
        }
        return poiDir
    }

    fun getTempDir(): File {
        val tempDir = File(rootMediaDir, "temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return tempDir
    }

    fun deletePoiFolder(poiId: Long): Boolean {
        val poiDir = File(rootMediaDir, "media/pois/$poiId")
        return poiDir.deleteRecursively()
    }

    fun resolvePath(relativePath: String): File {
        return File(rootMediaDir, relativePath)
    }

    fun toRelativePath(file: File): String {
        return file.absolutePath.removePrefix(rootMediaDir.absolutePath + File.separator)
    }
}
