package com.rimon.pdfmastersuite.model

enum class PdfCategory(val title: String, val subtitle: String) {
    ORGANIZE("Organize PDF", "Merge, split, extract, remove, rotate and more"),
    CONVERT("Convert PDF", "Convert to different formats"),
    SECURE("Secure PDF", "Protect and unlock your files"),
    OPTIMIZE("Optimize PDF", "Compress your PDF files"),
    OCR("OCR and Scanning", "Extract text and make searchable")
}

enum class PdfTool(
    val id: String,
    val title: String,
    val category: PdfCategory,
    val description: String,
    val acceptMultipleFiles: Boolean = false,
    val mimeTypes: List<String> = listOf("application/pdf"),
    val steps: List<String> = listOf(
        "Select your file",
        "Configure options",
        "Process and download"
    )
) {
    // Organize
    MERGE(
        id = "merge",
        title = "Merge PDF",
        category = PdfCategory.ORGANIZE,
        description = "Combine multiple PDF files into one document.",
        acceptMultipleFiles = true,
        steps = listOf(
            "Select multiple PDF files",
            "Arrange the order (optional)",
            "Merge and download the new file"
        )
    ),
    SPLIT(
        id = "split",
        title = "Split PDF",
        category = PdfCategory.ORGANIZE,
        description = "Separate one page or a whole set for easy conversion into independent PDF files.",
        steps = listOf(
            "Select a PDF file",
            "Specify page ranges or intervals",
            "Split and download the new file"
        )
    ),
    EXTRACT(
        id = "extract",
        title = "Extract Pages",
        category = PdfCategory.ORGANIZE,
        description = "Extract selected pages from your PDF document into a new PDF.",
        steps = listOf(
            "Select a PDF file",
            "Enter page numbers to extract",
            "Generate extracted PDF"
        )
    ),
    REMOVE(
        id = "remove",
        title = "Remove Pages",
        category = PdfCategory.ORGANIZE,
        description = "Delete unwanted pages from your PDF file.",
        steps = listOf(
            "Select a PDF file",
            "Select pages to delete",
            "Save the updated PDF"
        )
    ),
    ROTATE(
        id = "rotate",
        title = "Rotate Pages",
        category = PdfCategory.ORGANIZE,
        description = "Rotate your PDF pages 90, 180 or 270 degrees.",
        steps = listOf(
            "Select a PDF file",
            "Choose rotation angle (90°, 180°, 270°)",
            "Rotate and download the new file"
        )
    ),
    REORDER(
        id = "reorder",
        title = "Reorder Pages",
        category = PdfCategory.ORGANIZE,
        description = "Sort and rearrange the page sequence of your PDF.",
        steps = listOf(
            "Select a PDF file",
            "Reorder pages with custom sequence",
            "Save reordered PDF"
        )
    ),
    WATERMARK(
        id = "watermark",
        title = "Add Watermark",
        category = PdfCategory.ORGANIZE,
        description = "Stamp an image or text over your PDF in seconds.",
        steps = listOf(
            "Select a PDF file",
            "Configure watermark text, color & opacity",
            "Apply watermark to all pages"
        )
    ),
    PAGE_NUMBERS(
        id = "page_numbers",
        title = "Add Page Numbers",
        category = PdfCategory.ORGANIZE,
        description = "Add page numbers into PDFs with ease. Choose positions, typography, and size.",
        steps = listOf(
            "Select a PDF file",
            "Choose numbering style and position",
            "Add numbers to pages"
        )
    ),

    // Convert
    IMAGES_TO_PDF(
        id = "images_to_pdf",
        title = "Images to PDF",
        category = PdfCategory.CONVERT,
        description = "Convert JPG, JPEG, and PNG images to PDF.",
        acceptMultipleFiles = true,
        mimeTypes = listOf("image/jpeg", "image/png", "image/jpg", "image/*"),
        steps = listOf(
            "Select one or more images",
            "Choose page size and orientation",
            "Convert to high quality PDF"
        )
    ),
    PDF_TO_IMAGES(
        id = "pdf_to_images",
        title = "PDF to Images",
        category = PdfCategory.CONVERT,
        description = "Extract pages of your PDF document into PNG or JPEG images.",
        steps = listOf(
            "Select a PDF file",
            "Choose image format and resolution",
            "Extract pages as images"
        )
    ),
    PDF_TO_TEXT(
        id = "pdf_to_text",
        title = "PDF to Text",
        category = PdfCategory.CONVERT,
        description = "Extract text from PDF file and save as text file.",
        steps = listOf(
            "Select a PDF file",
            "Extract embedded text",
            "Copy or export text file"
        )
    ),
    WORD_TO_PDF(
        id = "word_to_pdf",
        title = "Word to PDF",
        category = PdfCategory.CONVERT,
        description = "Convert Microsoft Word DOCX documents to PDF format.",
        mimeTypes = listOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/msword"),
        steps = listOf(
            "Select a Word DOCX file",
            "Parse document headings, text & tables",
            "Generate formatted PDF"
        )
    ),
    EXCEL_TO_PDF(
        id = "excel_to_pdf",
        title = "Excel to PDF",
        category = PdfCategory.CONVERT,
        description = "Convert Microsoft Excel XLSX spreadsheets to PDF format.",
        mimeTypes = listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel"),
        steps = listOf(
            "Select an Excel XLSX spreadsheet",
            "Parse worksheets, columns & cell tables",
            "Generate spreadsheet PDF"
        )
    ),

    // Secure
    PROTECT(
        id = "protect",
        title = "Protect PDF",
        category = PdfCategory.SECURE,
        description = "Protect PDF files with a password. Encrypt PDF documents to prevent unauthorized access.",
        steps = listOf(
            "Select a PDF file",
            "Set a strong password",
            "Encrypt and secure your PDF"
        )
    ),
    UNLOCK(
        id = "unlock",
        title = "Unlock PDF",
        category = PdfCategory.SECURE,
        description = "Remove PDF password security, giving you the freedom to use your PDFs as you want.",
        steps = listOf(
            "Select an encrypted PDF file",
            "Enter current password to decrypt",
            "Save unlocked PDF file"
        )
    ),

    // Optimize
    COMPRESS(
        id = "compress",
        title = "Compress PDF",
        category = PdfCategory.OPTIMIZE,
        description = "Compress PDF file to get the same PDF quality but less filesize.",
        steps = listOf(
            "Select a PDF file",
            "Choose compression quality level",
            "Optimize file and compare sizes"
        )
    ),

    // OCR & Scanning
    OCR_IMAGE(
        id = "ocr_image",
        title = "Extract Text",
        category = PdfCategory.OCR,
        description = "Extract text from photos and scanned documents using on-device OCR.",
        mimeTypes = listOf("image/jpeg", "image/png", "image/jpg", "image/*"),
        steps = listOf(
            "Select a photo or scanned image",
            "Run offline OCR text recognition",
            "Copy or export extracted text"
        )
    ),
    OCR_PDF(
        id = "ocr_pdf",
        title = "OCR Scanned PDF",
        category = PdfCategory.OCR,
        description = "Convert scanned non-searchable PDF into a searchable PDF with text layer.",
        steps = listOf(
            "Select a scanned PDF document",
            "Recognize text on each page",
            "Generate searchable PDF"
        )
    )
}
