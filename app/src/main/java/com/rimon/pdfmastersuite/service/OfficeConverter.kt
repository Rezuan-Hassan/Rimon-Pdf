package com.rimon.pdfmastersuite.service

import android.content.Context
import android.util.Xml
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

class OfficeConverter(private val context: Context) {

    /**
     * Convert Word (.docx) document to formatted PDF.
     */
    suspend fun wordToPdf(
        docxFile: File,
        outputPdfFile: File,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(20, "Extracting Word document structure...")
        val elements = parseDocxElements(docxFile)

        onProgress(50, "Rendering document layout into PDF...")
        val doc = PDDocument()
        val margin = 54f // 0.75 in
        val pageWidth = PDRectangle.A4.width
        val pageHeight = PDRectangle.A4.height
        val contentWidth = pageWidth - (margin * 2)

        var currentPage = PDPage(PDRectangle.A4)
        doc.addPage(currentPage)
        var cs = PDPageContentStream(doc, currentPage)
        var cursorY = pageHeight - margin

        fun newPage() {
            cs.close()
            currentPage = PDPage(PDRectangle.A4)
            doc.addPage(currentPage)
            cs = PDPageContentStream(doc, currentPage)
            cursorY = pageHeight - margin
        }

        elements.forEachIndexed { idx, el ->
            val progress = 50 + ((idx.toFloat() / elements.size) * 40).toInt()
            onProgress(progress, "Rendering element ${idx + 1} of ${elements.size}...")

            when (el) {
                is DocElement.Heading -> {
                    val font = if (el.level == 1) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA_BOLD
                    val size = if (el.level == 1) 18f else 14f
                    val lineHeight = size * 1.4f

                    if (cursorY - lineHeight < margin) newPage()

                    cursorY -= 8f
                    cs.beginText()
                    cs.setFont(font, size)
                    cs.setNonStrokingColor(0.12f, 0.12f, 0.15f)
                    cs.newLineAtOffset(margin, cursorY)
                    cs.showText(sanitizeText(el.text))
                    cs.endText()
                    cursorY -= lineHeight
                }

                is DocElement.Paragraph -> {
                    val font = if (el.isBold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
                    val size = 11f
                    val lineHeight = size * 1.35f

                    val lines = wrapText(el.text, font, size, contentWidth)
                    for (line in lines) {
                        if (cursorY - lineHeight < margin) newPage()
                        cs.beginText()
                        cs.setFont(font, size)
                        cs.setNonStrokingColor(0.2f, 0.2f, 0.2f)
                        cs.newLineAtOffset(margin, cursorY)
                        cs.showText(sanitizeText(line))
                        cs.endText()
                        cursorY -= lineHeight
                    }
                    cursorY -= 6f // paragraph spacing
                }

                is DocElement.Table -> {
                    val rowCount = el.rows.size
                    if (rowCount > 0) {
                        val colCount = el.rows.maxOf { it.cells.size }.coerceAtLeast(1)
                        val colWidth = contentWidth / colCount
                        val rowHeight = 22f

                        for (row in el.rows) {
                            if (cursorY - rowHeight < margin) newPage()

                            // Draw row background / border
                            for ((cIdx, cellText) in row.cells.withIndex()) {
                                val cellX = margin + (cIdx * colWidth)
                                val cellY = cursorY - rowHeight

                                // Cell border
                                cs.setStrokingColor(0.8f, 0.8f, 0.85f)
                                cs.setLineWidth(0.75f)
                                cs.addRect(cellX, cellY, colWidth, rowHeight)
                                cs.stroke()

                                // Cell text
                                cs.beginText()
                                cs.setFont(PDType1Font.HELVETICA, 10f)
                                cs.setNonStrokingColor(0.15f, 0.15f, 0.15f)
                                cs.newLineAtOffset(cellX + 5f, cellY + 6f)
                                val trimmedText = if (cellText.length > 25) cellText.take(22) + "..." else cellText
                                cs.showText(sanitizeText(trimmedText))
                                cs.endText()
                            }
                            cursorY -= rowHeight
                        }
                        cursorY -= 10f
                    }
                }
            }
        }

        cs.close()
        onProgress(95, "Saving PDF output...")
        doc.save(outputPdfFile)
        val pagesCount = doc.numberOfPages
        doc.close()
        onProgress(100, "Word conversion completed!")
        pagesCount
    }

    /**
     * Convert Excel (.xlsx) spreadsheet to formatted PDF.
     */
    suspend fun excelToPdf(
        xlsxFile: File,
        outputPdfFile: File,
        onProgress: (percent: Int, step: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        onProgress(20, "Extracting Excel worksheet data...")
        val sheetData = parseXlsxData(xlsxFile)

        onProgress(50, "Rendering spreadsheet grid into PDF...")
        val doc = PDDocument()
        val margin = 40f
        // Landscape A4 for spreadsheets
        val pageWidth = PDRectangle.A4.height
        val pageHeight = PDRectangle.A4.width
        val contentWidth = pageWidth - (margin * 2)

        var currentPage = PDPage(PDRectangle(pageWidth, pageHeight))
        doc.addPage(currentPage)
        var cs = PDPageContentStream(doc, currentPage)
        var cursorY = pageHeight - margin

        fun newPage() {
            cs.close()
            currentPage = PDPage(PDRectangle(pageWidth, pageHeight))
            doc.addPage(currentPage)
            cs = PDPageContentStream(doc, currentPage)
            cursorY = pageHeight - margin
        }

        // Title Header
        cs.beginText()
        cs.setFont(PDType1Font.HELVETICA_BOLD, 16f)
        cs.setNonStrokingColor(0.1f, 0.45f, 0.25f) // Excel Greenish
        cs.newLineAtOffset(margin, cursorY)
        cs.showText(sanitizeText(sheetData.sheetName))
        cs.endText()
        cursorY -= 28f

        if (sheetData.rows.isNotEmpty()) {
            val maxCols = sheetData.rows.maxOf { it.size }.coerceIn(1, 10)
            val colWidth = contentWidth / maxCols
            val rowHeight = 20f

            sheetData.rows.forEachIndexed { rowIdx, rowCells ->
                if (cursorY - rowHeight < margin) newPage()

                val isHeaderRow = rowIdx == 0
                for (colIdx in 0 until maxCols) {
                    val cellText = if (colIdx < rowCells.size) rowCells[colIdx] else ""
                    val cellX = margin + (colIdx * colWidth)
                    val cellY = cursorY - rowHeight

                    // Header row background
                    if (isHeaderRow) {
                        cs.setNonStrokingColor(0.92f, 0.95f, 0.92f)
                        cs.addRect(cellX, cellY, colWidth, rowHeight)
                        cs.fill()
                    }

                    // Border
                    cs.setStrokingColor(0.82f, 0.82f, 0.85f)
                    cs.setLineWidth(0.6f)
                    cs.addRect(cellX, cellY, colWidth, rowHeight)
                    cs.stroke()

                    // Cell text
                    cs.beginText()
                    val font = if (isHeaderRow) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
                    val fontSize = 9f
                    cs.setFont(font, fontSize)
                    cs.setNonStrokingColor(if (isHeaderRow) 0.1f else 0.2f, if (isHeaderRow) 0.35f else 0.2f, if (isHeaderRow) 0.15f else 0.2f)
                    cs.newLineAtOffset(cellX + 4f, cellY + 5.5f)
                    val trimmed = if (cellText.length > 20) cellText.take(18) + "..." else cellText
                    cs.showText(sanitizeText(trimmed))
                    cs.endText()
                }
                cursorY -= rowHeight
            }
        }

        cs.close()
        onProgress(95, "Saving spreadsheet PDF...")
        doc.save(outputPdfFile)
        val pagesCount = doc.numberOfPages
        doc.close()
        onProgress(100, "Excel conversion completed!")
        pagesCount
    }

    // --- DOCX Parser ---

    private sealed class DocElement {
        data class Heading(val text: String, val level: Int) : DocElement()
        data class Paragraph(val text: String, val isBold: Boolean = false) : DocElement()
        data class Table(val rows: List<TableRow>) : DocElement()
    }

    private data class TableRow(val cells: List<String>)

    private fun parseDocxElements(docxFile: File): List<DocElement> {
        val elements = mutableListOf<DocElement>()
        try {
            ZipFile(docxFile).use { zip ->
                val entry = zip.getEntry("word/document.xml") ?: return emptyList()
                zip.getInputStream(entry).use { stream ->
                    val parser = Xml.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                        setInput(stream, "UTF-8")
                    }

                    var eventType = parser.eventType
                    var inParagraph = false
                    var inTable = false
                    var inRow = false
                    var inCell = false
                    var pText = StringBuilder()
                    var isHeading = false
                    var headingLevel = 1
                    var isBold = false

                    var currentTableRows = mutableListOf<TableRow>()
                    var currentRowCells = mutableListOf<String>()
                    var currentCellText = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val name = parser.name
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (name) {
                                    "w:tbl" -> {
                                        inTable = true
                                        currentTableRows = mutableListOf()
                                    }
                                    "w:tr" -> {
                                        inRow = true
                                        currentRowCells = mutableListOf()
                                    }
                                    "w:tc" -> {
                                        inCell = true
                                        currentCellText = StringBuilder()
                                    }
                                    "w:p" -> {
                                        inParagraph = true
                                        pText = StringBuilder()
                                        isHeading = false
                                        isBold = false
                                    }
                                    "w:pStyle" -> {
                                        val styleVal = parser.getAttributeValue(null, "w:val") ?: ""
                                        if (styleVal.contains("Heading", ignoreCase = true)) {
                                            isHeading = true
                                            headingLevel = if (styleVal.contains("1")) 1 else 2
                                        }
                                    }
                                    "w:b" -> isBold = true
                                    "w:t" -> {
                                        val text = parser.nextText()
                                        if (inCell) {
                                            currentCellText.append(text)
                                        } else if (inParagraph) {
                                            pText.append(text)
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                when (name) {
                                    "w:p" -> {
                                        val t = pText.toString().trim()
                                        if (t.isNotEmpty() && !inCell) {
                                            if (isHeading) {
                                                elements.add(DocElement.Heading(t, headingLevel))
                                            } else {
                                                elements.add(DocElement.Paragraph(t, isBold))
                                            }
                                        }
                                        inParagraph = false
                                    }
                                    "w:tc" -> {
                                        currentRowCells.add(currentCellText.toString().trim())
                                        inCell = false
                                    }
                                    "w:tr" -> {
                                        currentTableRows.add(TableRow(currentRowCells))
                                        inRow = false
                                    }
                                    "w:tbl" -> {
                                        elements.add(DocElement.Table(currentTableRows))
                                        inTable = false
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            elements.add(DocElement.Paragraph("Document content extraction completed with warnings: ${e.message}"))
        }
        return elements
    }

    // --- XLSX Parser ---

    private data class SheetData(val sheetName: String, val rows: List<List<String>>)

    private fun parseXlsxData(xlsxFile: File): SheetData {
        val sharedStrings = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()
        var sheetName = xlsxFile.nameWithoutExtension

        try {
            ZipFile(xlsxFile).use { zip ->
                // 1. Shared Strings
                val ssEntry = zip.getEntry("xl/sharedStrings.xml")
                if (ssEntry != null) {
                    zip.getInputStream(ssEntry).use { stream ->
                        val parser = Xml.newPullParser().apply {
                            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                            setInput(stream, "UTF-8")
                        }
                        var ev = parser.eventType
                        var curStr = StringBuilder()
                        while (ev != XmlPullParser.END_DOCUMENT) {
                            if (ev == XmlPullParser.START_TAG && parser.name == "t") {
                                curStr.append(parser.nextText())
                            } else if (ev == XmlPullParser.END_TAG && parser.name == "si") {
                                sharedStrings.add(curStr.toString())
                                curStr = StringBuilder()
                            }
                            ev = parser.next()
                        }
                    }
                }

                // 2. Sheet 1 XML
                val sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml") ?: zip.entries().asSequence().firstOrNull {
                    it.name.startsWith("xl/worksheets/sheet") && it.name.endsWith(".xml")
                }

                if (sheetEntry != null) {
                    zip.getInputStream(sheetEntry).use { stream ->
                        val parser = Xml.newPullParser().apply {
                            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                            setInput(stream, "UTF-8")
                        }
                        var ev = parser.eventType
                        var currentRow = mutableListOf<String>()
                        var cellType = ""
                        var cellVal = ""

                        while (ev != XmlPullParser.END_DOCUMENT) {
                            when (ev) {
                                XmlPullParser.START_TAG -> {
                                    when (parser.name) {
                                        "row" -> currentRow = mutableListOf()
                                        "c" -> cellType = parser.getAttributeValue(null, "t") ?: ""
                                        "v" -> {
                                            val raw = parser.nextText()
                                            cellVal = if (cellType == "s") {
                                                val sIdx = raw.toIntOrNull()
                                                if (sIdx != null && sIdx in sharedStrings.indices) sharedStrings[sIdx] else raw
                                            } else {
                                                raw
                                            }
                                            currentRow.add(cellVal)
                                        }
                                        "t" -> {
                                            // inline string
                                            currentRow.add(parser.nextText())
                                        }
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    if (parser.name == "row") {
                                        if (currentRow.any { it.isNotBlank() }) {
                                            rows.add(currentRow)
                                        }
                                    }
                                }
                            }
                            ev = parser.next()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            rows.add(listOf("Error reading sheet", e.message ?: ""))
        }

        return SheetData(sheetName, rows)
    }

    // --- Text Utilities ---

    private fun wrapText(text: String, font: PDType1Font, fontSize: Float, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val width = try {
                (font.getStringWidth(sanitizeText(candidate)) / 1000f) * fontSize
            } catch (e: Exception) {
                candidate.length * fontSize * 0.5f
            }

            if (width <= maxWidth) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun sanitizeText(input: String): String {
        return input.map { ch ->
            if (ch.code in 32..126) ch else ' '
        }.joinToString("")
    }
}
