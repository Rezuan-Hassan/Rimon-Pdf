package com.rimon.pdfmastersuite.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimon.pdfmastersuite.model.OperationConfig
import com.rimon.pdfmastersuite.model.PdfTool
import com.rimon.pdfmastersuite.model.ProcessedResult
import com.rimon.pdfmastersuite.model.ProcessingState
import com.rimon.pdfmastersuite.model.SelectedFile
import com.rimon.pdfmastersuite.service.FileManager
import com.rimon.pdfmastersuite.service.OcrService
import com.rimon.pdfmastersuite.service.OfficeConverter
import com.rimon.pdfmastersuite.service.PdfProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class Screen {
    DASHBOARD,
    TOOL_INTRO,
    SELECTED_FILES,
    OPTIONS,
    PROCESSING,
    COMPLETION,
    PREVIEW,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val fileManager = FileManager(application)
    private val pdfProcessor = PdfProcessor(application)
    private val officeConverter = OfficeConverter(application)
    private val ocrService = OcrService(application)

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedTool = MutableStateFlow<PdfTool?>(null)
    val selectedTool: StateFlow<PdfTool?> = _selectedTool.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<SelectedFile>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedFile>> = _selectedFiles.asStateFlow()

    private val _operationConfig = MutableStateFlow(OperationConfig())
    val operationConfig: StateFlow<OperationConfig> = _operationConfig.asStateFlow()

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    private val _lastResult = MutableStateFlow<ProcessedResult?>(null)
    val lastResult: StateFlow<ProcessedResult?> = _lastResult.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectTool(tool: PdfTool) {
        _selectedTool.value = tool
        _selectedFiles.value = emptyList()
        val defaultName = "${tool.title.replace(" ", "_")}_Document"
        _operationConfig.value = OperationConfig(outputFileName = defaultName)
        _processingState.value = ProcessingState.Idle
        _currentScreen.value = Screen.TOOL_INTRO
    }

    fun addSelectedUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedList = uris.map { fileManager.resolveSelectedFile(it) }
            val updated = if (_selectedTool.value?.acceptMultipleFiles == true) {
                _selectedFiles.value + resolvedList
            } else {
                resolvedList
            }
            _selectedFiles.value = updated

            if (updated.isNotEmpty()) {
                val base = updated.first().name.substringBeforeLast(".")
                val toolName = _selectedTool.value?.title?.replace(" ", "_") ?: "Processed"
                _operationConfig.value = _operationConfig.value.copy(
                    outputFileName = "${toolName}_$base"
                )
                _currentScreen.value = Screen.SELECTED_FILES
            }
        }
    }

    fun removeSelectedFile(index: Int) {
        val current = _selectedFiles.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedFiles.value = current
            if (current.isEmpty()) {
                _currentScreen.value = Screen.TOOL_INTRO
            }
        }
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
        _currentScreen.value = Screen.TOOL_INTRO
    }

    fun updateConfig(config: OperationConfig) {
        _operationConfig.value = config
    }

    fun startOperation() {
        val tool = _selectedTool.value ?: return
        val files = _selectedFiles.value
        if (files.isEmpty()) return

        val config = _operationConfig.value
        val outBaseName = config.outputFileName.ifBlank { "${tool.title.replace(" ", "_")}_Document" }

        _currentScreen.value = Screen.PROCESSING
        _processingState.value = ProcessingState.Processing(
            progressPercent = 10,
            currentStepText = "Preparing operation...",
            completedSteps = emptyList(),
            activeStep = "Reading files",
            pendingSteps = listOf("Processing document", "Finalizing & Saving")
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var outputFile: File
                var originalSize: Long? = files.firstOrNull()?.sizeBytes
                var mimeType = "application/pdf"
                var pageCount = 1
                var extractedTextResult: String? = null

                fun updateProgress(percent: Int, stepText: String) {
                    val completed = mutableListOf<String>()
                    var active = "Reading files"
                    val pending = mutableListOf<String>()

                    if (percent >= 35) {
                        completed.add("Reading files")
                        active = "Processing pages"
                    }
                    if (percent >= 85) {
                        completed.add("Processing pages")
                        active = "Finalizing document"
                    }
                    if (percent < 35) {
                        pending.addAll(listOf("Processing pages", "Finalizing document"))
                    } else if (percent < 85) {
                        pending.add("Finalizing document")
                    }

                    _processingState.value = ProcessingState.Processing(
                        progressPercent = percent,
                        currentStepText = stepText,
                        completedSteps = completed,
                        activeStep = active,
                        pendingSteps = pending
                    )
                }

                val inputFiles = files.map { File(it.tempFilePath ?: it.uri.path ?: "") }

                when (tool) {
                    PdfTool.MERGE -> {
                        outputFile = fileManager.createTempCacheFile("merged_", ".pdf")
                        pageCount = pdfProcessor.mergePdfs(inputFiles, outputFile, ::updateProgress)
                        originalSize = files.sumOf { it.sizeBytes }
                    }

                    PdfTool.SPLIT -> {
                        val outDir = fileManager.createTempCacheFile("split_dir_", "").apply {
                            delete()
                            mkdirs()
                        }
                        val splitFiles = pdfProcessor.splitPdf(inputFiles.first(), outDir, config, ::updateProgress)
                        outputFile = splitFiles.firstOrNull() ?: throw IllegalStateException("No split pages created")
                        pageCount = splitFiles.size
                    }

                    PdfTool.EXTRACT -> {
                        outputFile = fileManager.createTempCacheFile("extracted_", ".pdf")
                        pageCount = pdfProcessor.extractPages(inputFiles.first(), outputFile, config.pageRangeText, ::updateProgress)
                    }

                    PdfTool.REMOVE -> {
                        outputFile = fileManager.createTempCacheFile("removed_", ".pdf")
                        pageCount = pdfProcessor.removePages(inputFiles.first(), outputFile, config.pageRangeText, ::updateProgress)
                    }

                    PdfTool.ROTATE -> {
                        outputFile = fileManager.createTempCacheFile("rotated_", ".pdf")
                        pageCount = pdfProcessor.rotatePages(inputFiles.first(), outputFile, config.rotationAngle, config.pageRangeText.ifBlank { null }, ::updateProgress)
                    }

                    PdfTool.REORDER -> {
                        outputFile = fileManager.createTempCacheFile("reordered_", ".pdf")
                        val seq = if (config.customPageOrder.isNotEmpty()) config.customPageOrder else (1..files.first().pageCount).reversed().toList()
                        pageCount = pdfProcessor.reorderPages(inputFiles.first(), outputFile, seq, ::updateProgress)
                    }

                    PdfTool.WATERMARK -> {
                        outputFile = fileManager.createTempCacheFile("watermarked_", ".pdf")
                        pageCount = pdfProcessor.addWatermark(inputFiles.first(), outputFile, config, ::updateProgress)
                    }

                    PdfTool.PAGE_NUMBERS -> {
                        outputFile = fileManager.createTempCacheFile("numbered_", ".pdf")
                        pageCount = pdfProcessor.addPageNumbers(inputFiles.first(), outputFile, config, ::updateProgress)
                    }

                    PdfTool.IMAGES_TO_PDF -> {
                        outputFile = fileManager.createTempCacheFile("images_to_pdf_", ".pdf")
                        pageCount = pdfProcessor.imagesToPdf(inputFiles, outputFile, config, ::updateProgress)
                        originalSize = files.sumOf { it.sizeBytes }
                    }

                    PdfTool.PDF_TO_IMAGES -> {
                        val outDir = fileManager.createTempCacheFile("pdf_images_dir_", "").apply {
                            delete()
                            mkdirs()
                        }
                        val images = pdfProcessor.pdfToImages(inputFiles.first(), outDir, config, ::updateProgress)
                        outputFile = images.firstOrNull() ?: throw IllegalStateException("No images rendered")
                        mimeType = if (config.imageOutputFormat.name == "PNG") "image/png" else "image/jpeg"
                        pageCount = images.size
                    }

                    PdfTool.PDF_TO_TEXT -> {
                        outputFile = fileManager.createTempCacheFile("pdf_text_", ".txt")
                        extractedTextResult = pdfProcessor.extractText(inputFiles.first(), outputFile, ::updateProgress)
                        mimeType = "text/plain"
                    }

                    PdfTool.WORD_TO_PDF -> {
                        outputFile = fileManager.createTempCacheFile("word_to_pdf_", ".pdf")
                        pageCount = officeConverter.wordToPdf(inputFiles.first(), outputFile, ::updateProgress)
                    }

                    PdfTool.EXCEL_TO_PDF -> {
                        outputFile = fileManager.createTempCacheFile("excel_to_pdf_", ".pdf")
                        pageCount = officeConverter.excelToPdf(inputFiles.first(), outputFile, ::updateProgress)
                    }

                    PdfTool.PROTECT -> {
                        outputFile = fileManager.createTempCacheFile("protected_", ".pdf")
                        pageCount = pdfProcessor.protectPdf(inputFiles.first(), outputFile, config.password, ::updateProgress)
                    }

                    PdfTool.UNLOCK -> {
                        outputFile = fileManager.createTempCacheFile("unlocked_", ".pdf")
                        pageCount = pdfProcessor.unlockPdf(inputFiles.first(), outputFile, config.password, ::updateProgress)
                    }

                    PdfTool.COMPRESS -> {
                        outputFile = fileManager.createTempCacheFile("compressed_", ".pdf")
                        val (orig, compressed) = pdfProcessor.compressPdf(inputFiles.first(), outputFile, config.compressionPreset, ::updateProgress)
                        originalSize = orig
                    }

                    PdfTool.OCR_IMAGE -> {
                        outputFile = fileManager.createTempCacheFile("ocr_text_", ".txt")
                        extractedTextResult = ocrService.extractTextFromImage(inputFiles.first(), ::updateProgress)
                        outputFile.writeText(extractedTextResult)
                        mimeType = "text/plain"
                    }

                    PdfTool.OCR_PDF -> {
                        outputFile = fileManager.createTempCacheFile("ocr_pdf_", ".pdf")
                        pageCount = ocrService.ocrPdfToSearchablePdf(inputFiles.first(), outputFile, ::updateProgress)
                    }
                }

                currentTempFile = outputFile

                val extension = when {
                    mimeType == "application/pdf" -> ".pdf"
                    mimeType.startsWith("image/png") -> ".png"
                    mimeType.startsWith("image/jpeg") -> ".jpg"
                    mimeType.startsWith("text/") -> ".txt"
                    else -> ""
                }
                val resultFileName = if (outBaseName.endsWith(extension, ignoreCase = true)) outBaseName else "$outBaseName$extension"

                val result = ProcessedResult(
                    fileUri = Uri.fromFile(outputFile),
                    filePath = outputFile.absolutePath,
                    fileName = resultFileName,
                    fileSizeBytes = outputFile.length(),
                    originalSizeBytes = originalSize,
                    pageCount = pageCount,
                    mimeType = mimeType,
                    extractedText = extractedTextResult,
                    isSaved = false,
                    savedLocationDisplay = null
                )

                _lastResult.value = result
                _processingState.value = ProcessingState.Success(result)
                _currentScreen.value = Screen.COMPLETION

            } catch (e: Exception) {
                e.printStackTrace()
                _processingState.value = ProcessingState.Error(e.message ?: "An unexpected error occurred during processing.")
            }
        }
    }

    private var currentTempFile: File? = null

    fun setCustomSaveLocation(uri: Uri?) {
        if (uri == null) {
            _operationConfig.value = _operationConfig.value.copy(
                customSaveLocationUri = null,
                customSaveLocationName = "Downloads"
            )
        } else {
            val folderName = fileManager.getDirectoryDisplayName(uri)
            _operationConfig.value = _operationConfig.value.copy(
                customSaveLocationUri = uri,
                customSaveLocationName = folderName
            )
        }
    }

    fun saveCurrentResult(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val res = _lastResult.value ?: return
        if (res.isSaved) {
            viewModelScope.launch(Dispatchers.Main) {
                onComplete(true, res.savedLocationDisplay ?: res.filePath)
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempFile = currentTempFile ?: File(res.filePath)
                val config = _operationConfig.value
                val outBaseName = config.outputFileName.ifBlank { "Document" }
                val (savedUri, savedDisplayPath) = if (config.customSaveLocationUri != null) {
                    fileManager.saveToCustomFolder(tempFile, config.customSaveLocationUri, outBaseName, res.mimeType)
                } else {
                    fileManager.saveToDownloads(tempFile, outBaseName, res.mimeType)
                }

                val updated = res.copy(
                    fileUri = savedUri,
                    filePath = savedDisplayPath,
                    isSaved = true,
                    savedLocationDisplay = savedDisplayPath
                )
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    _lastResult.value = updated
                    _processingState.value = ProcessingState.Success(updated)
                    onComplete(true, savedDisplayPath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(false, e.message ?: "Failed to save file")
                }
            }
        }
    }

    fun shareCurrentResult() {
        val res = _lastResult.value ?: return
        val temp = currentTempFile ?: File(res.filePath)
        fileManager.shareResult(res.fileUri, temp, res.mimeType)
    }

    fun openCurrentResult() {
        val res = _lastResult.value ?: return
        val temp = currentTempFile ?: File(res.filePath)
        fileManager.openResult(res.fileUri, temp, res.mimeType)
    }

    fun openDownloads() {
        fileManager.openDownloadsFolder()
    }
}
