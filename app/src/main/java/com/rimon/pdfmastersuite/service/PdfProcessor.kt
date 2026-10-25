package com.rimon.pdfmastersuite.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.rimon.pdfmastersuite.model.CompressionPreset
import com.rimon.pdfmastersuite.model.ImageFormat
import com.rimon.pdfmastersuite.model.ImageOrientation
import com.rimon.pdfmastersuite.model.ImagePageSize
import com.rimon.pdfmastersuite.model.OperationConfig
import com.rimon.pdfmastersuite.model.PageNumberFormat
import com.rimon.pdfmastersuite.model.PageNumberPosition
import com.rimon.pdfmastersuite.model.SplitMode
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.sin

class PdfProcessor(private val context: Context) {

    /**
     * Merge multiple PDF files into one output PDF.
     */
    suspend fun mergePdfs(
        inputFiles: List<File>,
        outputFile: File,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        require(inputFiles.isNotEmpty()) { "At least one PDF file must be selected for merging." }

        onProgress(15, "Reading input PDF files...")
        val merger = PDFMergerUtility().apply {
            destinationFileName = outputFile.absolutePath
        }

        var totalPages = 0
        inputFiles.forEachIndexed { index, file ->
            val percent = 15 + ((index.toFloat() / inputFiles.size) * 45).toInt()
            onProgress(percent, "Adding ${file.name}...")
            PDDocument.load(file).use { doc ->
                totalPages += doc.numberOfPages
            }
            merger.addSource(file)
        }

        onProgress(70, "Merging document pages...")
        merger.mergeDocuments(null)

        onProgress(95, "Verifying merged output...")
        val verifiedPages = PDDocument.load(outputFile).use { doc ->
            doc.numberOfPages
        }
        onProgress(100, "Merge completed successfully!")
        verifiedPages
    }

    /**
     * Split PDF by range, intervals, or into single pages.
     */
    suspend fun splitPdf(
        inputFile: File,
        outputDir: File,
        config: OperationConfig,
        onProgress: (percent: Int, step: String) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        onProgress(20, "Loading document...")
        val resultFiles = mutableListOf<File>()

        PDDocument.load(inputFile).use { doc ->
            val totalPages = doc.numberOfPages
            require(totalPages > 0) { "Document has no pages." }

            when (config.splitMode) {
                SplitMode.RANGE -> {
                    onProgress(40, "Parsing page ranges...")
                    val pages = parsePageRange(config.pageRangeText, totalPages)
                    require(pages.isNotEmpty()) { "No valid pages specified in range: '${config.pageRangeText}'" }

                    val outDoc = PDDocument()
                    pages.forEachIndexed { idx, pageNum ->
                        val percent = 40 + ((idx.toFloat() / pages.size) * 50).toInt()
                        onProgress(percent, "Extracting page $pageNum of $totalPages...")
                        outDoc.addPage(doc.getPage(pageNum - 1))
                    }
                    val outFile = File(outputDir, "Split_${inputFile.nameWithoutExtension}.pdf")
                    outDoc.save(outFile)
                    outDoc.close()
                    resultFiles.add(outFile)
                }

                SplitMode.EVERY_N_PAGES -> {
                    val n = config.everyNPages.coerceAtLeast(1)
                    val splitter = Splitter().apply {
                        setSplitAtPage(n)
                    }
                    onProgress(50, "Splitting every $n pages...")
                    val splitDocs = splitter.split(doc)
                    splitDocs.forEachIndexed { idx, subDoc ->
                        val outFile = File(outputDir, "${inputFile.nameWithoutExtension}_part_${idx + 1}.pdf")
                        subDoc.save(outFile)
                        subDoc.close()
                        resultFiles.add(outFile)
                    }
                }

                SplitMode.ALL_PAGES -> {
                    val splitter = Splitter().apply {
                        setSplitAtPage(1)
                    }
                    onProgress(50, "Splitting into individual pages...")
                    val splitDocs = splitter.split(doc)
                    splitDocs.forEachIndexed { idx, subDoc ->
                        val outFile = File(outputDir, "${inputFile.nameWithoutExtension}_page_${idx + 1}.pdf")
                        subDoc.save(outFile)
                        subDoc.close()
                        resultFiles.add(outFile)
                    }
                }
            }
        }

        onProgress(100, "Split complete!")
        resultFiles
    }

    /**
     * Extract selected pages to a new PDF.
     */
    suspend fun extractPages(
        inputFile: File,
        outputFile: File,
        pageRangeText: String,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(20, "Loading document...")
        PDDocument.load(inputFile).use { doc ->
            val total = doc.numberOfPages
            val pages = parsePageRange(pageRangeText, total)
            require(pages.isNotEmpty()) { "Invalid or empty page selection." }

            val outDoc = PDDocument()
            pages.forEachIndexed { index, pageNum ->
                val progress = 20 + ((index.toFloat() / pages.size) * 70).toInt()
                onProgress(progress, "Extracting page $pageNum...")
                outDoc.addPage(doc.getPage(pageNum - 1))
            }
            onProgress(95, "Saving extracted pages...")
            outDoc.save(outputFile)
            outDoc.close()
            pages.size
        }
    }

    /**
     * Remove specified pages and save remaining pages.
     */
    suspend fun removePages(
        inputFile: File,
        outputFile: File,
        pageRangeText: String,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(20, "Reading document...")
        PDDocument.load(inputFile).use { doc ->
            val total = doc.numberOfPages
            val toRemove = parsePageRange(pageRangeText, total).toSet()

            require(toRemove.size < total) { "Cannot remove all pages from document." }

            val outDoc = PDDocument()
            var remainingCount = 0
            for (p in 1..total) {
                if (!toRemove.contains(p)) {
                    outDoc.addPage(doc.getPage(p - 1))
                    remainingCount++
                }
            }

            onProgress(85, "Saving updated PDF with $remainingCount pages...")
            outDoc.save(outputFile)
            outDoc.close()
            remainingCount
        }
    }

    /**
     * Rotate pages by 90, 180, or 270 degrees.
     */
    suspend fun rotatePages(
        inputFile: File,
        outputFile: File,
        angle: Int,
        pageRangeText: String? = null,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(20, "Reading document...")
        PDDocument.load(inputFile).use { doc ->
            val total = doc.numberOfPages
            val targetPages = if (!pageRangeText.isNullOrBlank()) {
                parsePageRange(pageRangeText, total).toSet()
            } else {
                (1..total).toSet()
            }

            targetPages.forEachIndexed { idx, p ->
                val progress = 20 + ((idx.toFloat() / targetPages.size) * 70).toInt()
                onProgress(progress, "Rotating page $p...")
                val page = doc.getPage(p - 1)
                val currentRotation = page.rotation
                page.rotation = (currentRotation + angle) % 360
            }

            onProgress(95, "Saving rotated document...")
            doc.save(outputFile)
            total
        }
    }

    /**
     * Reorder pages into custom sequence.
     */
    suspend fun reorderPages(
        inputFile: File,
        outputFile: File,
        newSequence: List<Int>,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(20, "Loading document...")
        PDDocument.load(inputFile).use { doc ->
            val total = doc.numberOfPages
            val outDoc = PDDocument()

            newSequence.forEachIndexed { idx, pageNum ->
                val progress = 20 + ((idx.toFloat() / newSequence.size) * 70).toInt()
                onProgress(progress, "Placing page $pageNum...")
                if (pageNum in 1..total) {
                    outDoc.addPage(doc.getPage(pageNum - 1))
                }
            }

            onProgress(95, "Saving reordered document...")
            outDoc.save(outputFile)
            outDoc.close()
            newSequence.size
        }
    }

    /**
     * Add text watermark to all pages with customizable opacity, rotation, and size.
     */
    suspend fun addWatermark(
        inputFile: File,
        outputFile: File,
        config: OperationConfig,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(20, "Loading document...")
        PDDocument.load(inputFile).use { doc ->
            val total = doc.numberOfPages
            val font = PDType1Font.HELVETICA_BOLD
            val fontSize = config.watermarkFontSize
            val text = config.watermarkText.ifBlank { "CONFIDENTIAL" }
            val angle = config.watermarkRotation
            val opacity = config.watermarkOpacity.coerceIn(0.05f, 1.0f)

            // ARGB color parsing
            val colorHex = config.watermarkColorHex
            val r = ((colorHex shr 16) and 0xFF) / 255f
            val g = ((colorHex shr 8) and 0xFF) / 255f
            val b = (colorHex and 0xFF) / 255f

            for (i in 0 until total) {
                val progress = 20 + ((i.toFloat() / total) * 70).toInt()
                onProgress(progress, "Watermarking page ${i + 1} of $total...")
                val page = doc.getPage(i)
                val mediaBox = page.mediaBox
                val width = mediaBox.width
                val height = mediaBox.height

                val textWidth = (font.getStringWidth(text) / 1000f) * fontSize
                val textHeight = (font.fontDescriptor?.capHeight ?: 700f) / 1000f * fontSize

                // Content stream in append mode
                val cs = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
                val gState = PDExtendedGraphicsState().apply {
                    nonStrokingAlphaConstant = opacity
                }
                cs.setGraphicsStateParameters(gState)
                cs.setNonStrokingColor(r, g, b)

                val rad = Math.toRadians(angle.toDouble())
                val cosVal = cos(rad).toFloat()
                val sinVal = sin(rad).toFloat()

                val centerX = width / 2f
                val centerY = height / 2f

                // Offset to center of text
                val tx = centerX - (textWidth / 2f * cosVal - textHeight / 2f * sinVal)
                val ty = centerY - (textWidth / 2f * sinVal + textHeight / 2f * cosVal)

                cs.beginText()
                cs.setFont(font, fontSize)
                cs.setTextMatrix(cosVal.toDouble(), sinVal.toDouble(), -sinVal.toDouble(), cosVal.toDouble(), tx.toDouble(), ty.toDouble())
                cs.showText(text)
                cs.endText()
                cs.close()
            }

            onProgress(95, "Saving watermarked PDF...")
            doc.save(outputFile)
            total
        }
    }

    /**
     * Add page numbers to pages with format and position.
     */
    suspend fun addPageNumbers(
        inputFile: File,
        outputFile: File,
        config: OperationConfig,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(20, "Loading document...")
        PDDocument.load(inputFile).use { doc ->
            val total = doc.numberOfPages
            val font = PDType1Font.HELVETICA
            val fontSize = config.pageNumberFontSize
            val startNum = config.pageNumberStart

            for (i in 0 until total) {
                val progress = 20 + ((i.toFloat() / total) * 70).toInt()
                onProgress(progress, "Numbering page ${i + 1} of $total...")
                val page = doc.getPage(i)
                val mediaBox = page.mediaBox
                val width = mediaBox.width
                val height = mediaBox.height

                val currentNum = startNum + i
                val text = when (config.pageNumberFormat) {
                    PageNumberFormat.NUMBER_ONLY -> "$currentNum"
                    PageNumberFormat.PAGE_OF_TOTAL -> "$currentNum of $total"
                    PageNumberFormat.PREFIX_PAGE -> "Page $currentNum"
                }

                val textWidth = (font.getStringWidth(text) / 1000f) * fontSize
                val margin = 36f // 0.5 inch

                val (x, y) = when (config.pageNumberPosition) {
                    PageNumberPosition.BOTTOM_CENTER -> Pair((width - textWidth) / 2f, margin)
                    PageNumberPosition.BOTTOM_RIGHT -> Pair(width - textWidth - margin, margin)
                    PageNumberPosition.BOTTOM_LEFT -> Pair(margin, margin)
                    PageNumberPosition.TOP_CENTER -> Pair((width - textWidth) / 2f, height - margin)
                    PageNumberPosition.TOP_RIGHT -> Pair(width - textWidth - margin, height - margin)
                }

                val cs = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
                cs.setNonStrokingColor(0.2f, 0.2f, 0.2f)
                cs.beginText()
                cs.setFont(font, fontSize)
                cs.newLineAtOffset(x, y)
                cs.showText(text)
                cs.endText()
                cs.close()
            }

            onProgress(95, "Saving numbered PDF...")
            doc.save(outputFile)
            total
        }
    }

    /**
     * Convert images to PDF.
     */
    suspend fun imagesToPdf(
        imageFiles: List<File>,
        outputFile: File,
        config: OperationConfig,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        require(imageFiles.isNotEmpty()) { "No images selected." }
        onProgress(15, "Starting Image to PDF conversion...")

        val doc = PDDocument()
        imageFiles.forEachIndexed { index, imgFile ->
            val progress = 15 + ((index.toFloat() / imageFiles.size) * 75).toInt()
            onProgress(progress, "Processing image ${index + 1} of ${imageFiles.size}...")

            // Decode bitmap safely with EXIF orientation correction
            val bitmap = decodeSampledBitmap(imgFile, 2048, 2048)
            val correctedBitmap = rotateBitmapIfRequired(bitmap, imgFile)

            val (pageWidth, pageHeight) = when (config.imagePageSize) {
                ImagePageSize.A4 -> {
                    if (config.imageOrientation == ImageOrientation.LANDSCAPE ||
                        (config.imageOrientation == ImageOrientation.AUTO && correctedBitmap.width > correctedBitmap.height)
                    ) {
                        Pair(PDRectangle.A4.height, PDRectangle.A4.width)
                    } else {
                        Pair(PDRectangle.A4.width, PDRectangle.A4.height)
                    }
                }
                ImagePageSize.LETTER -> {
                    if (config.imageOrientation == ImageOrientation.LANDSCAPE ||
                        (config.imageOrientation == ImageOrientation.AUTO && correctedBitmap.width > correctedBitmap.height)
                    ) {
                        Pair(PDRectangle.LETTER.height, PDRectangle.LETTER.width)
                    } else {
                        Pair(PDRectangle.LETTER.width, PDRectangle.LETTER.height)
                    }
                }
                ImagePageSize.FIT_TO_IMAGE -> {
                    Pair(correctedBitmap.width.toFloat(), correctedBitmap.height.toFloat())
                }
            }

            val page = PDPage(PDRectangle(pageWidth, pageHeight))
            doc.addPage(page)

            // Convert Bitmap to PDImageXObject
            val pdImage = JPEGFactory.createFromImage(doc, correctedBitmap, 0.85f)

            // Compute scaling to fit with aspect ratio preserved
            val margin = if (config.imagePageSize == ImagePageSize.FIT_TO_IMAGE) 0f else 36f
            val printableW = pageWidth - (margin * 2)
            val printableH = pageHeight - (margin * 2)

            val imgW = correctedBitmap.width.toFloat()
            val imgH = correctedBitmap.height.toFloat()
            val scale = minOf(printableW / imgW, printableH / imgH)

            val drawW = imgW * scale
            val drawH = imgH * scale
            val drawX = margin + (printableW - drawW) / 2f
            val drawY = margin + (printableH - drawH) / 2f

            val cs = PDPageContentStream(doc, page)
            cs.drawImage(pdImage, drawX, drawY, drawW, drawH)
            cs.close()

            correctedBitmap.recycle()
        }

        onProgress(95, "Finalizing PDF file...")
        doc.save(outputFile)
        doc.close()
        imageFiles.size
    }

    /**
     * Render PDF pages to PNG or JPEG images.
     */
    suspend fun pdfToImages(
        inputFile: File,
        outputDir: File,
        config: OperationConfig,
        onProgress: (percent: Int, step: String) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        onProgress(20, "Loading document...")
        val imageFiles = mutableListOf<File>()

        PDDocument.load(inputFile).use { doc ->
            val total = doc.numberOfPages
            val renderer = PDFRenderer(doc)
            val dpi = config.imageResolutionDpi.coerceIn(72, 300)
            val scale = dpi / 72f
            val isPng = config.imageOutputFormat == ImageFormat.PNG
            val ext = if (isPng) "png" else "jpg"
            val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            for (i in 0 until total) {
                val progress = 20 + ((i.toFloat() / total) * 75).toInt()
                onProgress(progress, "Rendering page ${i + 1} of $total...")

                val bitmap = renderer.renderImage(i, scale)
                val outFile = File(outputDir, "${inputFile.nameWithoutExtension}_page_${i + 1}.$ext")

                FileOutputStream(outFile).use { out ->
                    bitmap.compress(compressFormat, 90, out)
                }
                bitmap.recycle()
                imageFiles.add(outFile)
            }
        }

        onProgress(100, "Rendered ${imageFiles.size} images!")
        imageFiles
    }

    /**
     * Extract embedded text from PDF.
     */
    suspend fun extractText(
        inputFile: File,
        outputFile: File,
        onProgress: (percent: Int, step: String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        onProgress(30, "Reading document text...")
        val text = PDDocument.load(inputFile).use { doc ->
            val stripper = PDFTextStripper()
            stripper.getText(doc)
        }

        onProgress(80, "Writing text file...")
        outputFile.writeText(text)
        onProgress(100, "Text extracted successfully!")
        text
    }

    /**
     * Password-protect PDF with AES / Standard encryption.
     */
    suspend fun protectPdf(
        inputFile: File,
        outputFile: File,
        password: String,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        require(password.isNotBlank()) { "Password cannot be empty." }
        onProgress(30, "Loading document for encryption...")

        PDDocument.load(inputFile).use { doc ->
            val total = doc.numberOfPages
            onProgress(60, "Applying security policy...")
            val ap = AccessPermission().apply {
                setCanPrint(true)
                setCanExtractContent(false)
            }
            val spp = StandardProtectionPolicy(password, password, ap).apply {
                encryptionKeyLength = 128
            }
            doc.protect(spp)

            onProgress(90, "Saving encrypted document...")
            doc.save(outputFile)
            total
        }
    }

    /**
     * Decrypt password-protected PDF.
     */
    suspend fun unlockPdf(
        inputFile: File,
        outputFile: File,
        password: String,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(30, "Authenticating with password...")
        try {
            PDDocument.load(inputFile, password).use { doc ->
                require(!doc.isEncrypted || doc.currentAccessPermission.canExtractContent()) {
                    "Invalid password or document permissions do not permit unlocking."
                }
                onProgress(60, "Removing security protection...")
                doc.isAllSecurityToBeRemoved = true
                onProgress(90, "Saving decrypted document...")
                doc.save(outputFile)
                doc.numberOfPages
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to unlock document: Check if password is correct. (${e.message})", e)
        }
    }

    /**
     * Genuine PDF optimization/compression:
     * - Downsamples high-res embedded images based on preset
     * - Recompresses image streams
     * - Reports actual input and output sizes
     */
    suspend fun compressPdf(
        inputFile: File,
        outputFile: File,
        preset: CompressionPreset,
        onProgress: (percent: Int, step: String) -> Unit
    ): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val originalSize = inputFile.length()
        onProgress(20, "Analyzing PDF structure and streams...")

        PDDocument.load(inputFile).use { doc ->
            val total = doc.numberOfPages
            val targetDpi = when (preset) {
                CompressionPreset.EXTREME -> 100
                CompressionPreset.RECOMMENDED -> 150
                CompressionPreset.LOW -> 200
            }
            val quality = preset.imageQuality

            for (i in 0 until total) {
                val progress = 20 + ((i.toFloat() / total) * 65).toInt()
                onProgress(progress, "Optimizing page ${i + 1} of $total...")
                val page = doc.getPage(i)
                val resources = page.resources ?: continue

                for (name in resources.xObjectNames) {
                    if (resources.isImageXObject(name)) {
                        try {
                            val pdImage = resources.getXObject(name) as? PDImageXObject ?: continue
                            val origBmp = pdImage.image ?: continue

                            // Calculate target max dimensions
                            val maxDimension = (targetDpi * 8.5).toInt() // e.g. 150*8.5 = 1275px
                            val (newW, newH) = if (origBmp.width > maxDimension || origBmp.height > maxDimension) {
                                val s = minOf(maxDimension.toFloat() / origBmp.width, maxDimension.toFloat() / origBmp.height)
                                Pair((origBmp.width * s).toInt().coerceAtLeast(1), (origBmp.height * s).toInt().coerceAtLeast(1))
                            } else {
                                Pair(origBmp.width, origBmp.height)
                            }

                            val scaledBmp = if (newW != origBmp.width || newH != origBmp.height) {
                                Bitmap.createScaledBitmap(origBmp, newW, newH, true)
                            } else {
                                origBmp
                            }

                            // Recompress as JPEG
                            val compressedPdImage = JPEGFactory.createFromImage(doc, scaledBmp, quality / 100f)
                            resources.put(name, compressedPdImage)

                            if (scaledBmp !== origBmp) {
                                scaledBmp.recycle()
                            }
                            origBmp.recycle()
                        } catch (e: Exception) {
                            // Non-fatal for individual image
                            e.printStackTrace()
                        }
                    }
                }
            }

            onProgress(90, "Writing compressed document...")
            doc.save(outputFile)
        }

        val outputSize = outputFile.length()
        onProgress(100, "Optimization complete!")
        Pair(originalSize, outputSize)
    }

    // --- Helper Utilities ---

    fun parsePageRange(rangeText: String, totalPages: Int): List<Int> {
        val pages = mutableListOf<Int>()
        val parts = rangeText.split(",", ";", " ").map { it.trim() }.filter { it.isNotEmpty() }
        for (part in parts) {
            if (part.contains("-")) {
                val range = part.split("-").map { it.trim().toIntOrNull() }
                if (range.size == 2 && range[0] != null && range[1] != null) {
                    val start = range[0]!!.coerceIn(1, totalPages)
                    val end = range[1]!!.coerceIn(1, totalPages)
                    if (start <= end) {
                        for (p in start..end) pages.add(p)
                    } else {
                        for (p in start downTo end) pages.add(p)
                    }
                }
            } else {
                val p = part.toIntOrNull()
                if (p != null && p in 1..totalPages) {
                    pages.add(p)
                }
            }
        }
        return pages.distinct()
    }

    private fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, file: File): Bitmap {
        try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            return rotated
        } catch (e: Exception) {
            return bitmap
        }
    }
}
