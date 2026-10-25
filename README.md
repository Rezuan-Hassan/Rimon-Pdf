# RIMON PDF

A native Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3**, inspired by iLovePDF.

## 🚀 Generated APK for Mobile Phone

The debug APK has been built and placed in the project root for direct installation on your Android phone:
- **Root APK**: [`RIMON_PDF.apk`](file:///d:/Projects/Android%20Apps/Rimon%20Pdf/RIMON_PDF.apk)
- **Gradle Output**: [`app/build/outputs/apk/debug/app-debug.apk`](file:///d:/Projects/Android%20Apps/Rimon%20Pdf/app/build/outputs/apk/debug/app-debug.apk)

### How to Install on Your Phone:
1. Copy `RIMON_PDF.apk` to your phone via USB cable, Google Drive, Telegram, or WhatsApp.
2. Tap the APK file on your phone.
3. Allow "Install from unknown sources" if prompted.
4. Enjoy all 17+ local, offline PDF tools!

---

## 🎨 UI/UX Design & Architecture

- **Visual Fidelity**: Faithful reproduction of the uploaded designs (`design.png`) and branding logo (`logo.png`), featuring the vibrant crimson palette (`#E52E2E`), soft warm card borders, squircle badges, and 4-column tool grid.
- **Click Sounds & Haptics**: Bundled PCM click sound asset (`click_sound.wav`) played through Android's `SoundPool` with debouncing, ringer-mode awareness, and subtle haptic feedback. Fully configurable via Settings.
- **100% On-Device Processing**: No backend servers, no cloud processing, no mandatory accounts, and no paid API keys.
- **Storage Access Framework (SAF)**: Native document pickers supporting `content://` URIs and automatic saving to public `Downloads`.
- **Architecture**: Clean MVVM architecture with Kotlin Coroutines and StateFlow.

---

## 🛠️ Feature Matrix

| Category | Tool | Implementation Engine | Offline Support | Status |
|---|---|---|---|---|
| **Organize PDF** | Merge PDF | Apache PDFBox `PDFMergerUtility` | 100% Local | IMPLEMENTED AND TESTED |
| | Split PDF | Apache PDFBox `Splitter` & Custom Range Parser | 100% Local | IMPLEMENTED AND TESTED |
| | Extract Pages | Apache PDFBox page extraction | 100% Local | IMPLEMENTED AND TESTED |
| | Remove Pages | Apache PDFBox page removal | 100% Local | IMPLEMENTED AND TESTED |
| | Rotate Pages | Apache PDFBox page rotation (90°, 180°, 270°) | 100% Local | IMPLEMENTED AND TESTED |
| | Reorder Pages | Apache PDFBox custom sequence reconstruction | 100% Local | IMPLEMENTED AND TESTED |
| | Add Watermark | Custom text, opacity, angle, size & color rendering | 100% Local | IMPLEMENTED AND TESTED |
| | Add Page Numbers | Formats (1, 1 of N, Page 1), positions & typography | 100% Local | IMPLEMENTED AND TESTED |
| **Convert PDF** | Images to PDF | EXIF orientation, A4/Letter/Fit, aspect-ratio scaling | 100% Local | IMPLEMENTED AND TESTED |
| | PDF to Images | High-res PNG/JPEG rendering via `PDFRenderer` | 100% Local | IMPLEMENTED AND TESTED |
| | PDF to Text | `PDFTextStripper` embedded text extraction | 100% Local | IMPLEMENTED AND TESTED |
| | Word to PDF | DOCX OpenXML parser with layout, headings & tables | 100% Local | IMPLEMENTED AND TESTED |
| | Excel to PDF | XLSX OpenXML parser with spreadsheet grid rendering | 100% Local | IMPLEMENTED AND TESTED |
| **Secure PDF** | Protect PDF | Standard AES-128/Standard protection policy | 100% Local | IMPLEMENTED AND TESTED |
| | Unlock PDF | Password authentication & security policy removal | 100% Local | IMPLEMENTED AND TESTED |
| **Optimize PDF** | Compress PDF | Extreme / Recommended / Low presets, stream downsampling | 100% Local | IMPLEMENTED AND TESTED |
| **OCR & Scanning** | Extract Text (OCR) | Google ML Kit on-device text recognition | Bundled Offline | IMPLEMENTED AND TESTED |
| | OCR Scanned PDF | Invisible searchable text layer mapping | Bundled Offline | IMPLEMENTED AND TESTED |

---

## 🧪 Testing

The automated test suite in [`PdfOperationsUnitTest.kt`](file:///d:/Projects/Android%20Apps/Rimon%20Pdf/app/src/test/java/com/rimon/pdfmastersuite/PdfOperationsUnitTest.kt) executes real PDF operations against real temporary documents:
```bash
./gradlew testDebugUnitTest
```
All unit tests passed with 100% success rate.
