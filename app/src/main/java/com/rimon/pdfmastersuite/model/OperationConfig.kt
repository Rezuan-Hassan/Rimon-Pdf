package com.rimon.pdfmastersuite.model

data class OperationConfig(
    // Common
    val outputFileName: String = "",
    val customSaveLocationUri: android.net.Uri? = null,
    val customSaveLocationName: String = "Downloads",

    // Split & Extract & Remove
    val pageRangeText: String = "1",
    val everyNPages: Int = 1,
    val splitMode: SplitMode = SplitMode.RANGE,

    // Rotate
    val rotationAngle: Int = 90, // 90, 180, 270

    // Reorder
    val customPageOrder: List<Int> = emptyList(),

    // Watermark
    val watermarkText: String = "CONFIDENTIAL",
    val watermarkFontSize: Float = 48f,
    val watermarkOpacity: Float = 0.35f,
    val watermarkRotation: Float = 45f,
    val watermarkColorHex: Long = 0xFFFF0000, // ARGB

    // Page Numbers
    val pageNumberStart: Int = 1,
    val pageNumberPosition: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER,
    val pageNumberFormat: PageNumberFormat = PageNumberFormat.PAGE_OF_TOTAL,
    val pageNumberFontSize: Float = 11f,

    // Images to PDF
    val imagePageSize: ImagePageSize = ImagePageSize.A4,
    val imageOrientation: ImageOrientation = ImageOrientation.PORTRAIT,

    // PDF to Images
    val imageOutputFormat: ImageFormat = ImageFormat.JPEG,
    val imageResolutionDpi: Int = 150,

    // Protect & Unlock
    val password: String = "",

    // Compress
    val compressionPreset: CompressionPreset = CompressionPreset.RECOMMENDED
)

enum class SplitMode {
    RANGE,
    EVERY_N_PAGES,
    ALL_PAGES
}

enum class PageNumberPosition {
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_CENTER,
    TOP_RIGHT
}

enum class PageNumberFormat {
    NUMBER_ONLY, // "1"
    PAGE_OF_TOTAL, // "1 of 5"
    PREFIX_PAGE // "Page 1"
}

enum class ImagePageSize {
    A4,
    LETTER,
    FIT_TO_IMAGE
}

enum class ImageOrientation {
    PORTRAIT,
    LANDSCAPE,
    AUTO
}

enum class ImageFormat {
    PNG,
    JPEG
}

enum class CompressionPreset(val label: String, val description: String, val imageQuality: Int) {
    EXTREME("Extreme Compression", "High compression, lower image quality", 35),
    RECOMMENDED("Recommended", "Good compression, high visual quality", 65),
    LOW("Low Compression", "Subtle compression, maximum quality", 85)
}
