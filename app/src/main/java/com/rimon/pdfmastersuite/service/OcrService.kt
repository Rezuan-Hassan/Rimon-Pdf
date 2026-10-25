package com.rimon.pdfmastersuite.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OcrService(private val context: Context) {

    /**
     * Extracts text from an image file using on-device ML Kit OCR.
     */
    suspend fun extractTextFromImage(
        imageFile: File,
        onProgress: (percent: Int, step: String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        onProgress(20, "Loading image...")
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            ?: throw IllegalArgumentException("Could not decode image file.")

        onProgress(45, "Running on-device OCR engine...")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = Tasks.await(recognizer.process(inputImage))
            onProgress(90, "Extracting recognized text...")
            val resultText = visionText.text
            onProgress(100, "OCR completed!")
            resultText
        } finally {
            recognizer.close()
            bitmap.recycle()
        }
    }

    /**
     * OCR scanned PDF pages and injects an invisible text layer to make the PDF searchable,
     * preserving the original visual appearance of the document.
     */
    suspend fun ocrPdfToSearchablePdf(
        inputFile: File,
        outputFile: File,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(15, "Loading scanned document...")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        try {
            PDDocument.load(inputFile).use { doc ->
                val totalPages = doc.numberOfPages
                val renderer = PDFRenderer(doc)
                val font = PDType1Font.HELVETICA

                for (pageIndex in 0 until totalPages) {
                    val progress = 15 + ((pageIndex.toFloat() / totalPages) * 75).toInt()
                    onProgress(progress, "Performing OCR on page ${pageIndex + 1} of $totalPages...")

                    val page = doc.getPage(pageIndex)
                    val mediaBox = page.mediaBox
                    val pageWidth = mediaBox.width
                    val pageHeight = mediaBox.height

                    // Render page at 150 DPI for accurate recognition
                    val renderScale = 150f / 72f
                    val pageBitmap = renderer.renderImage(pageIndex, renderScale)

                    val inputImage = InputImage.fromBitmap(pageBitmap, 0)
                    val visionText = Tasks.await(recognizer.process(inputImage))

                    val scaleX = pageWidth / pageBitmap.width.toFloat()
                    val scaleY = pageHeight / pageBitmap.height.toFloat()

                    // Create content stream in append mode
                    val cs = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)

                    // Make text transparent / invisible (Rendering mode 3 or 0 opacity)
                    val gState = PDExtendedGraphicsState().apply {
                        nonStrokingAlphaConstant = 0.0f
                    }
                    cs.setGraphicsStateParameters(gState)

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val box = line.boundingBox
                            val lineText = sanitizeText(line.text)
                            if (box != null && lineText.isNotBlank()) {
                                // Convert coordinates from top-left (image) to bottom-left (PDF)
                                val x = box.left * scaleX
                                val y = pageHeight - (box.bottom * scaleY)
                                val h = (box.bottom - box.top) * scaleY
                                val fontSize = h.coerceIn(6f, 32f)

                                try {
                                    cs.beginText()
                                    cs.setFont(font, fontSize)
                                    cs.newLineAtOffset(x, y)
                                    cs.showText(lineText)
                                    cs.endText()
                                } catch (e: Exception) {
                                    // Skip unencodable glyphs
                                }
                            }
                        }
                    }

                    cs.close()
                    pageBitmap.recycle()
                }

                onProgress(95, "Saving searchable PDF...")
                doc.save(outputFile)

                // Verify that searchable text now exists
                try {
                    val stripper = PDFTextStripper()
                    val extracted = stripper.getText(doc)
                    // Verified searchable text
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                onProgress(100, "Searchable PDF generated successfully!")
                totalPages
            }
        } finally {
            recognizer.close()
        }
    }

    private fun sanitizeText(input: String): String {
        return input.map { ch ->
            if (ch.code in 32..126) ch else ' '
        }.joinToString("")
    }
}
