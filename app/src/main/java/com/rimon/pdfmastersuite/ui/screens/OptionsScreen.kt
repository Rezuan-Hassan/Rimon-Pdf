package com.rimon.pdfmastersuite.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimon.pdfmastersuite.model.CompressionPreset
import com.rimon.pdfmastersuite.model.ImageFormat
import com.rimon.pdfmastersuite.model.ImageOrientation
import com.rimon.pdfmastersuite.model.ImagePageSize
import com.rimon.pdfmastersuite.model.OperationConfig
import com.rimon.pdfmastersuite.model.PageNumberFormat
import com.rimon.pdfmastersuite.model.PageNumberPosition
import com.rimon.pdfmastersuite.model.PdfTool
import com.rimon.pdfmastersuite.model.SelectedFile
import com.rimon.pdfmastersuite.model.SplitMode
import com.rimon.pdfmastersuite.ui.components.DetailTopBar
import com.rimon.pdfmastersuite.ui.components.soundClickable
import com.rimon.pdfmastersuite.ui.theme.Background
import com.rimon.pdfmastersuite.ui.theme.CardBorder
import com.rimon.pdfmastersuite.ui.theme.RedPrimary
import com.rimon.pdfmastersuite.ui.theme.RedTint
import com.rimon.pdfmastersuite.ui.theme.Surface
import com.rimon.pdfmastersuite.ui.theme.TextMuted
import com.rimon.pdfmastersuite.ui.theme.TextPrimary
import com.rimon.pdfmastersuite.ui.theme.TextSecondary

@Composable
fun OptionsScreen(
    tool: PdfTool,
    selectedFiles: List<SelectedFile>,
    config: OperationConfig,
    onConfigChange: (OperationConfig) -> Unit,
    onSelectSaveLocationClick: () -> Unit,
    onStartOperation: () -> Unit,
    onBackClick: () -> Unit
) {
    var showOriginalFiles by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputText by remember { mutableStateOf(config.outputFileName) }

    if (showRenameDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Output File Name") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("File Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        focusedLabelColor = RedPrimary
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (renameInputText.isNotBlank()) {
                            onConfigChange(config.copy(outputFileName = renameInputText.trim()))
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Done", color = RedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = tool.title,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(RedPrimary)
                        .soundClickable(onClick = onStartOperation),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            tint = Surface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start ${tool.title}",
                            color = Surface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Output Options",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // File Name Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .soundClickable {
                        renameInputText = config.outputFileName
                        showRenameDialog = true
                    }
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "File Name (tap to edit)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = config.outputFileName.ifBlank { "Output_Document" } +
                                    if (tool == PdfTool.PDF_TO_TEXT || tool == PdfTool.OCR_IMAGE) ".txt" else ".pdf",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = RedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Location Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .soundClickable(onClick = onSelectSaveLocationClick)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Save Location (tap to choose folder)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Folder",
                                tint = RedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = config.customSaveLocationName.ifBlank { "Downloads" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Select",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Original Files Accordion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .soundClickable { showOriginalFiles = !showOriginalFiles },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Original Files (${selectedFiles.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = if (showOriginalFiles) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = showOriginalFiles) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            selectedFiles.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = file.name,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = file.formattedSize,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tool-Specific Config Options
            when (tool) {
                PdfTool.WATERMARK -> {
                    Text(
                        text = "Watermark Settings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = config.watermarkText,
                        onValueChange = { onConfigChange(config.copy(watermarkText = it)) },
                        label = { Text("Watermark Text") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            focusedLabelColor = RedPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Opacity: ${(config.watermarkOpacity * 100).toInt()}%",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Slider(
                        value = config.watermarkOpacity,
                        onValueChange = { onConfigChange(config.copy(watermarkOpacity = it)) },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = RedPrimary, activeTrackColor = RedPrimary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Rotation Angle: ${config.watermarkRotation.toInt()}°",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0f, 45f, 90f).forEach { angle ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (config.watermarkRotation == angle) RedPrimary else Surface)
                                    .border(1.dp, if (config.watermarkRotation == angle) RedPrimary else CardBorder, RoundedCornerShape(8.dp))
                                    .soundClickable { onConfigChange(config.copy(watermarkRotation = angle)) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${angle.toInt()}°",
                                    color = if (config.watermarkRotation == angle) Surface else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                PdfTool.PAGE_NUMBERS -> {
                    Text(
                        text = "Numbering Style & Position",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Format", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PageNumberFormat.values().forEach { fmt ->
                            val label = when (fmt) {
                                PageNumberFormat.PAGE_OF_TOTAL -> "1 of N"
                                PageNumberFormat.PREFIX_PAGE -> "Page 1"
                                PageNumberFormat.NUMBER_ONLY -> "1"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (config.pageNumberFormat == fmt) RedPrimary else Surface)
                                    .border(1.dp, if (config.pageNumberFormat == fmt) RedPrimary else CardBorder, RoundedCornerShape(8.dp))
                                    .soundClickable { onConfigChange(config.copy(pageNumberFormat = fmt)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (config.pageNumberFormat == fmt) Surface else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Position", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PageNumberPosition.BOTTOM_CENTER to "Bottom Center",
                            PageNumberPosition.BOTTOM_RIGHT to "Bottom Right",
                            PageNumberPosition.TOP_CENTER to "Top Center"
                        ).forEach { (pos, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (config.pageNumberPosition == pos) RedPrimary else Surface)
                                    .border(1.dp, if (config.pageNumberPosition == pos) RedPrimary else CardBorder, RoundedCornerShape(8.dp))
                                    .soundClickable { onConfigChange(config.copy(pageNumberPosition = pos)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (config.pageNumberPosition == pos) Surface else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                PdfTool.SPLIT, PdfTool.EXTRACT, PdfTool.REMOVE -> {
                    Text(
                        text = "Page Selection",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = config.pageRangeText,
                        onValueChange = { onConfigChange(config.copy(pageRangeText = it)) },
                        label = { Text("Page Ranges (e.g. 1-3, 5)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            focusedLabelColor = RedPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Total pages: ${selectedFiles.firstOrNull()?.pageCount ?: 1}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                PdfTool.ROTATE -> {
                    Text(
                        text = "Rotation Angle",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(90, 180, 270).forEach { angle ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (config.rotationAngle == angle) RedPrimary else Surface)
                                    .border(1.dp, if (config.rotationAngle == angle) RedPrimary else CardBorder, RoundedCornerShape(8.dp))
                                    .soundClickable { onConfigChange(config.copy(rotationAngle = angle)) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$angle°",
                                    color = if (config.rotationAngle == angle) Surface else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                PdfTool.PROTECT, PdfTool.UNLOCK -> {
                    Text(
                        text = if (tool == PdfTool.PROTECT) "Set Password" else "Enter Password",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = config.password,
                        onValueChange = { onConfigChange(config.copy(password = it)) },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            focusedLabelColor = RedPrimary
                        )
                    )
                }

                PdfTool.COMPRESS -> {
                    Text(
                        text = "Compression Level",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CompressionPreset.values().forEach { preset ->
                        val isSelected = config.compressionPreset == preset
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) RedTint else Surface)
                                .border(1.dp, if (isSelected) RedPrimary else CardBorder, RoundedCornerShape(12.dp))
                                .soundClickable { onConfigChange(config.copy(compressionPreset = preset)) }
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = preset.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) RedPrimary else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.description,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                PdfTool.IMAGES_TO_PDF -> {
                    Text(
                        text = "Page Format & Orientation",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Page Size", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImagePageSize.values().forEach { size ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (config.imagePageSize == size) RedPrimary else Surface)
                                    .border(1.dp, if (config.imagePageSize == size) RedPrimary else CardBorder, RoundedCornerShape(8.dp))
                                    .soundClickable { onConfigChange(config.copy(imagePageSize = size)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = size.name.replace("_", " "),
                                    color = if (config.imagePageSize == size) Surface else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Orientation", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImageOrientation.values().forEach { orient ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (config.imageOrientation == orient) RedPrimary else Surface)
                                    .border(1.dp, if (config.imageOrientation == orient) RedPrimary else CardBorder, RoundedCornerShape(8.dp))
                                    .soundClickable { onConfigChange(config.copy(imageOrientation = orient)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = orient.name,
                                    color = if (config.imageOrientation == orient) Surface else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                PdfTool.PDF_TO_IMAGES -> {
                    Text(
                        text = "Image Output Format",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ImageFormat.values().forEach { fmt ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (config.imageOutputFormat == fmt) RedPrimary else Surface)
                                    .border(1.dp, if (config.imageOutputFormat == fmt) RedPrimary else CardBorder, RoundedCornerShape(8.dp))
                                    .soundClickable { onConfigChange(config.copy(imageOutputFormat = fmt)) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fmt.name,
                                    color = if (config.imageOutputFormat == fmt) Surface else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
