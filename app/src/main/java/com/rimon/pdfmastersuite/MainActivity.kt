package com.rimon.pdfmastersuite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rimon.pdfmastersuite.model.ProcessingState
import com.rimon.pdfmastersuite.ui.screens.CompletionScreen
import com.rimon.pdfmastersuite.ui.screens.DashboardScreen
import com.rimon.pdfmastersuite.ui.screens.OptionsScreen
import com.rimon.pdfmastersuite.ui.screens.PreviewScreen
import com.rimon.pdfmastersuite.ui.screens.ProcessingScreen
import com.rimon.pdfmastersuite.ui.screens.SelectedFilesScreen
import com.rimon.pdfmastersuite.ui.screens.SettingsScreen
import com.rimon.pdfmastersuite.ui.screens.ToolIntroScreen
import com.rimon.pdfmastersuite.ui.theme.PDFMasterSuiteTheme
import com.rimon.pdfmastersuite.ui.theme.RedPrimary
import com.rimon.pdfmastersuite.ui.viewmodel.MainViewModel
import com.rimon.pdfmastersuite.ui.viewmodel.Screen
import com.rimon.pdfmastersuite.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PDFMasterSuiteTheme {
                val currentScreen by mainViewModel.currentScreen.collectAsState()
                val selectedTool by mainViewModel.selectedTool.collectAsState()
                val selectedFiles by mainViewModel.selectedFiles.collectAsState()
                val operationConfig by mainViewModel.operationConfig.collectAsState()
                val processingState by mainViewModel.processingState.collectAsState()
                val lastResult by mainViewModel.lastResult.collectAsState()

                val soundEnabled by settingsViewModel.soundEnabled.collectAsState()
                val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState()

                // File Picker Launchers
                val singlePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let { mainViewModel.addSelectedUris(listOf(it)) }
                }

                val multiPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris: List<Uri> ->
                    if (uris.isNotEmpty()) {
                        mainViewModel.addSelectedUris(uris)
                    }
                }

                // Storage Access Framework Directory Picker Launcher for Save Location
                val saveDirectoryPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        mainViewModel.setCustomSaveLocation(uri)
                        Toast.makeText(this@MainActivity, "Save location set to: ${mainViewModel.operationConfig.value.customSaveLocationName}", Toast.LENGTH_SHORT).show()
                    }
                }

                fun launchFilePicker() {
                    val tool = selectedTool ?: return
                    val mimeTypes = tool.mimeTypes.toTypedArray()
                    if (tool.acceptMultipleFiles) {
                        multiPickerLauncher.launch(mimeTypes)
                    } else {
                        singlePickerLauncher.launch(mimeTypes)
                    }
                }

                // Handle Back Button navigation
                BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
                    when (currentScreen) {
                        Screen.SETTINGS -> mainViewModel.navigateTo(Screen.DASHBOARD)
                        Screen.TOOL_INTRO -> mainViewModel.navigateTo(Screen.DASHBOARD)
                        Screen.SELECTED_FILES -> mainViewModel.navigateTo(Screen.TOOL_INTRO)
                        Screen.OPTIONS -> mainViewModel.navigateTo(Screen.SELECTED_FILES)
                        Screen.PROCESSING -> {
                            // Don't interrupt processing directly; ask or ignore
                            Toast.makeText(this@MainActivity, "Processing in background...", Toast.LENGTH_SHORT).show()
                        }
                        Screen.COMPLETION -> mainViewModel.navigateTo(Screen.DASHBOARD)
                        Screen.PREVIEW -> mainViewModel.navigateTo(Screen.COMPLETION)
                        Screen.DASHBOARD -> finish()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        Screen.DASHBOARD -> {
                            DashboardScreen(
                                onToolClick = { tool ->
                                    mainViewModel.selectTool(tool)
                                },
                                onSettingsClick = {
                                    mainViewModel.navigateTo(Screen.SETTINGS)
                                },
                                onAboutClick = {
                                    mainViewModel.navigateTo(Screen.SETTINGS)
                                }
                            )
                        }

                        Screen.TOOL_INTRO -> {
                            selectedTool?.let { tool ->
                                ToolIntroScreen(
                                    tool = tool,
                                    onBackClick = { mainViewModel.navigateTo(Screen.DASHBOARD) },
                                    onSelectFilesClick = { launchFilePicker() }
                                )
                            }
                        }

                        Screen.SELECTED_FILES -> {
                            selectedTool?.let { tool ->
                                SelectedFilesScreen(
                                    tool = tool,
                                    selectedFiles = selectedFiles,
                                    outputFileName = operationConfig.outputFileName,
                                    onOutputFileNameChange = {
                                        mainViewModel.updateConfig(operationConfig.copy(outputFileName = it))
                                    },
                                    onRemoveFile = { index ->
                                        mainViewModel.removeSelectedFile(index)
                                    },
                                    onClearAll = {
                                        mainViewModel.clearSelectedFiles()
                                    },
                                    onAddMoreFiles = { launchFilePicker() },
                                    onContinue = { mainViewModel.navigateTo(Screen.OPTIONS) },
                                    onBackClick = { mainViewModel.navigateTo(Screen.TOOL_INTRO) }
                                )
                            }
                        }

                        Screen.OPTIONS -> {
                            selectedTool?.let { tool ->
                                OptionsScreen(
                                    tool = tool,
                                    selectedFiles = selectedFiles,
                                    config = operationConfig,
                                    onConfigChange = { mainViewModel.updateConfig(it) },
                                    onSelectSaveLocationClick = { saveDirectoryPickerLauncher.launch(null) },
                                    onStartOperation = { mainViewModel.startOperation() },
                                    onBackClick = { mainViewModel.navigateTo(Screen.SELECTED_FILES) }
                                )
                            }
                        }

                        Screen.PROCESSING -> {
                            when (val state = processingState) {
                                is ProcessingState.Processing -> {
                                    ProcessingScreen(
                                        state = state,
                                        onBackClick = {
                                            // Confirm cancel or navigate to dashboard
                                            mainViewModel.navigateTo(Screen.DASHBOARD)
                                        }
                                    )
                                }
                                is ProcessingState.Error -> {
                                    AlertDialog(
                                        onDismissRequest = { mainViewModel.navigateTo(Screen.OPTIONS) },
                                        title = { Text("Processing Error") },
                                        text = { Text(state.message) },
                                        confirmButton = {
                                            TextButton(
                                                onClick = { mainViewModel.navigateTo(Screen.OPTIONS) }
                                            ) {
                                                Text("Retry", color = RedPrimary)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(
                                                onClick = { mainViewModel.navigateTo(Screen.DASHBOARD) }
                                            ) {
                                                Text("Dashboard")
                                            }
                                        }
                                    )
                                }
                                else -> {}
                            }
                        }

                        Screen.COMPLETION -> {
                            selectedTool?.let { tool ->
                                lastResult?.let { res ->
                                    CompletionScreen(
                                        tool = tool,
                                        result = res,
                                        saveLocationName = operationConfig.customSaveLocationName,
                                        onPreviewClick = { mainViewModel.navigateTo(Screen.PREVIEW) },
                                        onDownloadClick = {
                                            mainViewModel.saveCurrentResult { success, pathOrError ->
                                                runOnUiThread {
                                                    if (success) {
                                                        Toast.makeText(this@MainActivity, "Saved to $pathOrError", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(this@MainActivity, "Error saving: $pathOrError", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        onChangeSaveLocationClick = {
                                            saveDirectoryPickerLauncher.launch(null)
                                        },
                                        onShareClick = { mainViewModel.shareCurrentResult() },
                                        onOpenAnotherAppClick = { mainViewModel.openCurrentResult() },
                                        onGoToDownloadsClick = { mainViewModel.openDownloads() },
                                        onBackClick = { mainViewModel.navigateTo(Screen.DASHBOARD) }
                                    )
                                }
                            }
                        }

                        Screen.PREVIEW -> {
                            lastResult?.let { res ->
                                PreviewScreen(
                                    result = res,
                                    onOpenExternal = { mainViewModel.openCurrentResult() },
                                    onBackClick = { mainViewModel.navigateTo(Screen.COMPLETION) }
                                )
                            }
                        }

                        Screen.SETTINGS -> {
                            SettingsScreen(
                                soundEnabled = soundEnabled,
                                hapticsEnabled = hapticsEnabled,
                                onSoundToggle = { settingsViewModel.toggleSound(it) },
                                onHapticsToggle = { settingsViewModel.toggleHaptics(it) },
                                onBackClick = { mainViewModel.navigateTo(Screen.DASHBOARD) }
                            )
                        }
                    }
                }
            }
        }
    }
}
