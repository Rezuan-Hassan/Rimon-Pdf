package com.rimon.pdfmastersuite.model

import android.net.Uri

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int = 1,
    val mimeType: String? = null,
    val tempFilePath: String? = null
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 KB"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f KB", kb)
            }
        }

    val pageCountDisplay: String
        get() = if (pageCount > 1) "$pageCount pages" else "$pageCount page"
}
