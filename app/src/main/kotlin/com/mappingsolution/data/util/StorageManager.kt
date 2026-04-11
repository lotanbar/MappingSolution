package com.mappingsolution.data.util

import android.os.Environment
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor() {

    val rootDir: File = File(Environment.getExternalStorageDirectory(), "mapping-solution-assets")

    init { rootDir.mkdirs() }

    // ── Name sanitization ────────────────────────────────────────────────────
    /** Replaces filesystem-unsafe characters with underscores and trims whitespace. */
    fun sanitizeName(name: String): String =
        name.replace(Regex("""[/\\:*?"<>|]"""), "_").trim()

    // ── Groups — filename is the group name (unique by dedup) ────────────────
    fun getGroupsDir(): File = File(rootDir, "groups").also { it.mkdirs() }
    fun getGroupFile(groupName: String): File = File(getGroupsDir(), "${sanitizeName(groupName)}.json")

    // ── POIs — folder is "{sanitized-name}_{first8ofId}", media flat inside ──
    fun getPoisDir(): File = File(rootDir, "pois").also { it.mkdirs() }
    fun poiFolderName(name: String, id: String): String = "${sanitizeName(name)}_${id.take(8)}"
    fun getPoiDir(name: String, id: String): File = File(getPoisDir(), poiFolderName(name, id)).also { it.mkdirs() }
    fun getPoiFile(name: String, id: String): File = File(getPoiDir(name, id), "poi.json")
    /** Media files live flat inside the POI folder (no media/ subfolder). Does NOT create the dir. */
    fun getPoiMediaDir(name: String, id: String): File = File(getPoisDir(), poiFolderName(name, id))

    // ── Bulk imported POIs — one folder per group ─────────────────────────
    /** The JSONL file holding all POIs for a bulk-imported group (one JSON object per line). */
    fun getBulkPoisFile(name: String, id: String): File = File(getPoiDir(name, id), "bulk_pois.jsonl")
    /** The images subfolder inside a bulk group folder. */
    fun getBulkImagesDir(name: String, id: String): File = File(getPoiDir(name, id), "images")
    fun deletePoiFolder(name: String, id: String): Boolean =
        File(getPoisDir(), poiFolderName(name, id)).deleteRecursively()
    fun renamePoiFolder(oldName: String, newName: String, id: String) {
        val oldDir = File(getPoisDir(), poiFolderName(oldName, id))
        val newDir = File(getPoisDir(), poiFolderName(newName, id))
        if (oldDir.exists() && oldDir.canonicalPath != newDir.canonicalPath) {
            if (oldDir.renameTo(newDir)) {
                // Rename succeeded; clean up any stale old dir left behind on some Android versions.
                if (oldDir.exists()) oldDir.deleteRecursively()
            }
            // If rename failed, leave oldDir intact — deleting it would permanently lose the data.
        }
    }

    // ── Recordings — folder is "{sanitized-name}_{first8ofId}" ───────────────
    fun getRecordingsDir(): File = File(rootDir, "recordings").also { it.mkdirs() }
    fun recordingFolderName(name: String, id: String): String = "${sanitizeName(name)}_${id.take(8)}"
    fun getRecordingDir(name: String, id: String): File = File(getRecordingsDir(), recordingFolderName(name, id)).also { it.mkdirs() }
    fun getRecordingFile(name: String, id: String): File = File(getRecordingDir(name, id), "recording.json")
    fun getRecordingPointsFile(name: String, id: String): File = File(getRecordingDir(name, id), "points.jsonl")
    fun deleteRecordingFolder(name: String, id: String): Boolean =
        File(getRecordingsDir(), recordingFolderName(name, id)).deleteRecursively()
    fun renameRecordingFolder(oldName: String, newName: String, id: String) {
        val oldDir = File(getRecordingsDir(), recordingFolderName(oldName, id))
        val newDir = File(getRecordingsDir(), recordingFolderName(newName, id))
        if (oldDir.exists() && oldDir.canonicalPath != newDir.canonicalPath) {
            if (oldDir.renameTo(newDir)) {
                // Rename succeeded; clean up any stale old dir left behind on some Android versions.
                if (oldDir.exists()) oldDir.deleteRecursively()
            }
            // If rename failed, leave oldDir intact — deleting it would permanently lose the data.
        }
    }

    fun getTempDir(): File = File(rootDir, "temp").also { it.mkdirs() }
    fun getExportsDir(): File = File(rootDir, "exports").also { it.mkdirs() }

    fun resolvePath(relativePath: String): File = File(rootDir, relativePath)
    fun toRelativePath(file: File): String =
        file.absolutePath.removePrefix(rootDir.absolutePath + File.separator)
}
