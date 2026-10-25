package com.rimon.pdfmastersuite.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.rimon.pdfmastersuite.model.SelectedFile
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class FileManager(private val context: Context) {

    /**
     * Resolves a Uri to a SelectedFile model, copying to temporary cache if necessary
     * and reading page count if it's a PDF.
     */
    suspend fun resolveSelectedFile(uri: Uri): SelectedFile = withContext(Dispatchers.IO) {
        var displayName = "document"
        var sizeBytes: Long = 0

        // 1. Query ContentResolver metadata
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: "document"
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val mimeType = context.contentResolver.getType(uri) ?: when {
            displayName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            displayName.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            displayName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            displayName.endsWith(".png", ignoreCase = true) -> "image/png"
            displayName.endsWith(".jpg", ignoreCase = true) || displayName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            else -> "application/octet-stream"
        }

        // 2. Copy content stream to private cache file
        val tempFile = createTempCacheFile("input_", "_${displayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}")
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (sizeBytes <= 0) {
                sizeBytes = tempFile.length()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Inspect page count if PDF
        var pageCount = 1
        if (mimeType == "application/pdf" || displayName.endsWith(".pdf", ignoreCase = true)) {
            try {
                PDDocument.load(tempFile).use { doc ->
                    pageCount = doc.numberOfPages
                }
            } catch (e: Exception) {
                // If encrypted or corrupt, default to 1
                pageCount = 1
            }
        }

        SelectedFile(
            uri = uri,
            name = displayName,
            sizeBytes = sizeBytes,
            pageCount = pageCount,
            mimeType = mimeType,
            tempFilePath = tempFile.absolutePath
        )
    }

    /**
     * Creates a temporary working file in app cache.
     */
    fun createTempCacheFile(prefix: String, suffix: String): File {
        val dir = File(context.cacheDir, "pdf_ops").apply { mkdirs() }
        return File.createTempFile(prefix, suffix, dir)
    }

    /**
     * Saves a processed file to public Downloads directory using MediaStore on API 29+
     * or direct filesystem on older versions.
     */
    suspend fun saveToDownloads(sourceFile: File, desiredFileName: String, mimeType: String): Pair<Uri?, String> =
        withContext(Dispatchers.IO) {
            val extension = if (desiredFileName.contains(".")) "" else {
                when {
                    mimeType == "application/pdf" -> ".pdf"
                    mimeType.startsWith("image/png") -> ".png"
                    mimeType.startsWith("image/jpeg") -> ".jpg"
                    mimeType.startsWith("text/") -> ".txt"
                    else -> ""
                }
            }
            val finalName = "$desiredFileName$extension"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val itemUri = context.contentResolver.insert(collectionUri, contentValues)

                if (itemUri != null) {
                    context.contentResolver.openOutputStream(itemUri)?.use { outStream ->
                        FileInputStream(sourceFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(itemUri, contentValues, null, null)

                    val displayPath = "/storage/emulated/0/Download/$finalName"
                    return@withContext Pair(itemUri, displayPath)
                }
            }

            // Fallback for API < 29 or if MediaStore insertion failed
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            var destFile = File(downloadsDir, finalName)
            var counter = 1
            val baseName = finalName.substringBeforeLast(".")
            val ext = if (finalName.contains(".")) ".${finalName.substringAfterLast(".")}" else ""
            while (destFile.exists()) {
                destFile = File(downloadsDir, "$baseName ($counter)$ext")
                counter++
            }

            FileInputStream(sourceFile).use { inStream ->
                FileOutputStream(destFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            val fileUri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
            } catch (e: Exception) {
                Uri.fromFile(destFile)
            }

            Pair(fileUri, destFile.absolutePath)
        }

    /**
     * Saves file to a user-selected SAF directory tree URI.
     */
    suspend fun saveToCustomFolder(
        sourceFile: File,
        treeUri: Uri,
        desiredFileName: String,
        mimeType: String
    ): Pair<Uri?, String> = withContext(Dispatchers.IO) {
        val extension = if (desiredFileName.contains(".")) "" else {
            when {
                mimeType == "application/pdf" -> ".pdf"
                mimeType.startsWith("image/png") -> ".png"
                mimeType.startsWith("image/jpeg") -> ".jpg"
                mimeType.startsWith("text/") -> ".txt"
                else -> ""
            }
        }
        val finalName = "$desiredFileName$extension"

        val treeDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Could not access selected directory.")

        val folderName = treeDir.name ?: "Selected Folder"
        var destDoc = treeDir.createFile(mimeType, finalName)
            ?: throw IllegalStateException("Could not create file in selected directory.")

        context.contentResolver.openOutputStream(destDoc.uri)?.use { outStream ->
            FileInputStream(sourceFile).use { inStream ->
                inStream.copyTo(outStream)
            }
        }

        Pair(destDoc.uri, "$folderName/$finalName")
    }

    /**
     * Retrieves human-readable name of a selected directory URI.
     */
    fun getDirectoryDisplayName(treeUri: Uri): String {
        return try {
            val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            doc?.name ?: "Selected Folder"
        } catch (e: Exception) {
            "Selected Folder"
        }
    }

    /**
     * Share file Intent supporting both content Uri and FileProvider.
     */
    fun shareResult(uri: Uri?, file: File?, mimeType: String, title: String = "Share File") {
        try {
            val contentUri = when {
                uri != null && uri.scheme == "content" -> uri
                file != null && file.exists() -> FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                uri != null && uri.scheme == "file" -> {
                    val f = File(uri.path ?: "")
                    if (f.exists()) FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f) else uri
                }
                else -> uri
            } ?: return

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Open file with external viewer supporting both content Uri and FileProvider.
     */
    fun openResult(uri: Uri?, file: File?, mimeType: String) {
        try {
            val contentUri = when {
                uri != null && uri.scheme == "content" -> uri
                file != null && file.exists() -> FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                uri != null && uri.scheme == "file" -> {
                    val f = File(uri.path ?: "")
                    if (f.exists()) FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f) else uri
                }
                else -> uri
            } ?: return

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Share file Intent.
     */
    fun shareFile(file: File, mimeType: String, title: String = "Share File") {
        shareResult(null, file, mimeType, title)
    }

    /**
     * Open file with external viewer.
     */
    fun openFile(file: File, mimeType: String) {
        openResult(null, file, mimeType)
    }

    /**
     * Open Downloads folder.
     */
    fun openDownloadsFolder() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path), "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Open files app
            try {
                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * Cleans up temporary files in cache.
     */
    fun clearTempFiles() {
        try {
            val dir = File(context.cacheDir, "pdf_ops")
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
