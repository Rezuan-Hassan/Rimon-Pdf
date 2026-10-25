package com.rimon.pdfmastersuite

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PdfOperationsUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var samplePdf1: File
    private lateinit var samplePdf2: File

    @Before
    fun setup() {
        // Create real PDF 1 with 3 pages
        samplePdf1 = tempFolder.newFile("sample1.pdf")
        PDDocument().use { doc ->
            for (i in 1..3) {
                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)
                // Draw a simple rectangle on the page
                PDPageContentStream(doc, page).use { cs ->
                    cs.setNonStrokingColor(0.8f, 0.2f, 0.2f)
                    cs.addRect(100f, 100f * i, 200f, 50f)
                    cs.fill()
                }
            }
            doc.save(samplePdf1)
        }

        // Create real PDF 2 with 2 pages
        samplePdf2 = tempFolder.newFile("sample2.pdf")
        PDDocument().use { doc ->
            for (i in 1..2) {
                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.setNonStrokingColor(0.2f, 0.6f, 0.2f)
                    cs.addRect(50f, 150f * i, 150f, 60f)
                    cs.fill()
                }
            }
            doc.save(samplePdf2)
        }
    }

    @Test
    fun testMergePdfs() {
        val mergedOutput = tempFolder.newFile("merged_out.pdf")
        val merger = com.tom_roush.pdfbox.multipdf.PDFMergerUtility().apply {
            destinationFileName = mergedOutput.absolutePath
            addSource(samplePdf1)
            addSource(samplePdf2)
        }
        merger.mergeDocuments(null)

        assertTrue(mergedOutput.exists())
        PDDocument.load(mergedOutput).use { doc ->
            assertEquals(5, doc.numberOfPages) // 3 + 2 = 5 pages
        }
    }

    @Test
    fun testSplitAndExtractPages() {
        val extractOutput = tempFolder.newFile("extracted_out.pdf")
        PDDocument.load(samplePdf1).use { srcDoc ->
            val outDoc = PDDocument()
            // Extract pages 1 and 3 (0-indexed: 0, 2)
            outDoc.addPage(srcDoc.getPage(0))
            outDoc.addPage(srcDoc.getPage(2))
            outDoc.save(extractOutput)
            outDoc.close()
        }

        assertTrue(extractOutput.exists())
        PDDocument.load(extractOutput).use { doc ->
            assertEquals(2, doc.numberOfPages)
        }
    }

    @Test
    fun testRemovePages() {
        val removedOutput = tempFolder.newFile("removed_out.pdf")
        PDDocument.load(samplePdf1).use { srcDoc ->
            val outDoc = PDDocument()
            // Keep only pages 1 and 2
            outDoc.addPage(srcDoc.getPage(0))
            outDoc.addPage(srcDoc.getPage(1))
            outDoc.save(removedOutput)
            outDoc.close()
        }

        PDDocument.load(removedOutput).use { doc ->
            assertEquals(2, doc.numberOfPages)
        }
    }

    @Test
    fun testRotatePages() {
        val rotateOutput = tempFolder.newFile("rotated_out.pdf")
        PDDocument.load(samplePdf1).use { doc ->
            val page = doc.getPage(0)
            page.rotation = 90
            doc.save(rotateOutput)
        }

        PDDocument.load(rotateOutput).use { doc ->
            assertEquals(90, doc.getPage(0).rotation)
            assertEquals(0, doc.getPage(1).rotation)
        }
    }

    @Test
    fun testReorderPages() {
        val reorderOutput = tempFolder.newFile("reordered_out.pdf")
        PDDocument.load(samplePdf1).use { srcDoc ->
            val outDoc = PDDocument()
            // Order: 3, 1, 2
            outDoc.addPage(srcDoc.getPage(2))
            outDoc.addPage(srcDoc.getPage(0))
            outDoc.addPage(srcDoc.getPage(1))
            outDoc.save(reorderOutput)
            outDoc.close()
        }

        PDDocument.load(reorderOutput).use { doc ->
            assertEquals(3, doc.numberOfPages)
        }
    }

    @Test
    fun testAddWatermarkRect() {
        val watermarkOutput = tempFolder.newFile("watermarked_out.pdf")
        PDDocument.load(samplePdf1).use { doc ->
            val page = doc.getPage(0)
            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                cs.setNonStrokingColor(1f, 0f, 0f)
                cs.addRect(200f, 400f, 150f, 50f)
                cs.fill()
            }
            doc.save(watermarkOutput)
        }

        assertTrue(watermarkOutput.exists())
        assertTrue(watermarkOutput.length() > 0)
    }

    @Test
    fun testEncryptionAndDecryption() {
        val encryptedOutput = tempFolder.newFile("encrypted_out.pdf")
        val decryptedOutput = tempFolder.newFile("decrypted_out.pdf")
        val password = "SecretMasterPassword123"

        // 1. Encrypt
        PDDocument.load(samplePdf1).use { doc ->
            val ap = AccessPermission().apply {
                setCanPrint(true)
                setCanExtractContent(false)
            }
            val spp = StandardProtectionPolicy(password, password, ap).apply {
                encryptionKeyLength = 128
            }
            doc.protect(spp)
            doc.save(encryptedOutput)
        }

        // 2. Verify encrypted file cannot be read without password
        var failedWithoutPassword = false
        try {
            PDDocument.load(encryptedOutput).use { doc ->
                if (doc.isEncrypted) {
                    failedWithoutPassword = true
                }
            }
        } catch (e: Exception) {
            failedWithoutPassword = true
        }
        assertTrue("Document should be encrypted", failedWithoutPassword)

        // 3. Decrypt with correct password
        PDDocument.load(encryptedOutput, password).use { doc ->
            assertTrue(doc.isEncrypted)
            doc.isAllSecurityToBeRemoved = true
            doc.save(decryptedOutput)
        }

        // 4. Verify decrypted document opens freely
        PDDocument.load(decryptedOutput).use { doc ->
            assertFalse(doc.isEncrypted)
            assertEquals(3, doc.numberOfPages)
        }
    }

    @Test(expected = Exception::class)
    fun testDecryptionWithWrongPasswordFails() {
        val encryptedOutput = tempFolder.newFile("encrypted_test.pdf")
        PDDocument.load(samplePdf1).use { doc ->
            val ap = AccessPermission()
            val spp = StandardProtectionPolicy("CorrectPassword", "CorrectPassword", ap)
            doc.protect(spp)
            doc.save(encryptedOutput)
        }

        // Should throw exception with wrong password
        PDDocument.load(encryptedOutput, "WrongPasswordWrongPassword").use { doc ->
            doc.getPage(0)
        }
    }

    @Test
    fun testPageRangeParsingLogic() {
        fun parse(rangeText: String, total: Int): List<Int> {
            val pages = mutableListOf<Int>()
            rangeText.split(",", ";", " ").map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
                if (part.contains("-")) {
                    val r = part.split("-").map { it.trim().toIntOrNull() }
                    if (r.size == 2 && r[0] != null && r[1] != null) {
                        for (p in r[0]!!.coerceIn(1, total)..r[1]!!.coerceIn(1, total)) pages.add(p)
                    }
                } else {
                    part.toIntOrNull()?.let { if (it in 1..total) pages.add(it) }
                }
            }
            return pages.distinct()
        }

        val result1 = parse("1-3, 5, 8-10", 10)
        assertEquals(listOf(1, 2, 3, 5, 8, 9, 10), result1)

        val result2 = parse("2, 4", 5)
        assertEquals(listOf(2, 4), result2)
    }

    @Test
    fun testCorruptAndEmptyHandling() {
        val emptyFile = tempFolder.newFile("empty.pdf")
        var caught = false
        try {
            PDDocument.load(emptyFile)
        } catch (e: Exception) {
            caught = true
        }
        assertTrue("Loading empty file should throw exception", caught)
    }
}
