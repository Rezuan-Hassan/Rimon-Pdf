package com.rimon.pdfmastersuite.model

import android.net.Uri

sealed class ProcessingState {
    object Idle : ProcessingState()

    data class Processing(
        val progressPercent: Int,
        val currentStepText: String,
        val completedSteps: List<String>,
        val activeStep: String,
        val pendingSteps: List<String>
    ) : ProcessingState()

    data class Success(val result: ProcessedResult) : ProcessingState()

    data class Error(val message: String) : ProcessingState()
}

data class ProcessedResult(
    val fileUri: Uri?,
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val originalSizeBytes: Long? = null,
    val pageCount: Int = 1,
    val mimeType: String = "application/pdf",
    val extractedText: String? = null,
    val outputFilesCount: Int = 1,
    val isSaved: Boolean = false,
    val savedLocationDisplay: String? = null
) {
    val formattedSize: String
        get() {
            if (fileSizeBytes <= 0) return "0 KB"
            val kb = fileSizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f KB", kb)
            }
        }

    val formattedOriginalSize: String?
        get() {
            val bytes = originalSizeBytes ?: return null
            if (bytes <= 0) return "0 KB"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f KB", kb)
            }
        }

    val compressionPercentage: Int?
        get() {
            val orig = originalSizeBytes ?: return null
            if (orig <= 0) return null
            val diff = orig - fileSizeBytes
            return ((diff.toDouble() / orig.toDouble()) * 100).toInt()
        }
}
