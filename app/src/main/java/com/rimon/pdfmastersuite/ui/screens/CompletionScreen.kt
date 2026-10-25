package com.rimon.pdfmastersuite.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimon.pdfmastersuite.model.PdfTool
import com.rimon.pdfmastersuite.model.ProcessedResult
import com.rimon.pdfmastersuite.ui.components.DetailTopBar
import com.rimon.pdfmastersuite.ui.components.soundClickable
import com.rimon.pdfmastersuite.ui.theme.Background
import com.rimon.pdfmastersuite.ui.theme.CardBorder
import com.rimon.pdfmastersuite.ui.theme.RedPrimary
import com.rimon.pdfmastersuite.ui.theme.RedTint
import com.rimon.pdfmastersuite.ui.theme.SuccessGreen
import com.rimon.pdfmastersuite.ui.theme.Surface
import com.rimon.pdfmastersuite.ui.theme.TextPrimary
import com.rimon.pdfmastersuite.ui.theme.TextSecondary

@Composable
fun CompletionScreen(
    tool: PdfTool,
    result: ProcessedResult,
    saveLocationName: String = "Downloads",
    onPreviewClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onChangeSaveLocationClick: () -> Unit = {},
    onShareClick: () -> Unit,
    onOpenAnotherAppClick: () -> Unit,
    onGoToDownloadsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = tool.title,
                onBackClick = onBackClick,
                showMoreOptions = true,
                onMoreClick = onShareClick
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Big Hero Checkmark Document Badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD4D4), RedTint, Color.Transparent),
                            radius = 160f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(RedPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Surface,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Success Title & Subtitle
            Text(
                text = "${tool.title} Successful!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your document is ready to view and share.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Result File Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Surface,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.fileName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${result.formattedSize}  ·  ${result.pageCount} ${if (result.pageCount > 1) "pages" else "page"}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            if (result.compressionPercentage != null && result.compressionPercentage!! > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "-${result.compressionPercentage}%",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action Buttons: Preview & Download/Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Outlined Preview Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Surface)
                        .border(1.5.dp, RedPrimary, RoundedCornerShape(25.dp))
                        .soundClickable(onClick = onPreviewClick),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Preview",
                            tint = RedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Preview",
                            color = RedPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Filled Download/Save Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(if (result.isSaved) SuccessGreen else RedPrimary)
                        .then(
                            if (!result.isSaved) Modifier.soundClickable(onClick = onDownloadClick)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (result.isSaved) Icons.Default.Check else Icons.Default.Download,
                            contentDescription = if (result.isSaved) "Saved" else "Save",
                            tint = Surface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (result.isSaved) "Saved" else "Save",
                            color = Surface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Secondary Actions List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(vertical = 4.dp)
            ) {
                Column {
                    if (!result.isSaved) {
                        ActionRow(
                            icon = Icons.Default.Folder,
                            title = "Save Location: $saveLocationName (tap to change)",
                            onClick = onChangeSaveLocationClick
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CardBorder)
                                .padding(horizontal = 16.dp)
                        )
                    }
                    ActionRow(
                        icon = Icons.Default.Share,
                        title = "Share",
                        onClick = onShareClick
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CardBorder)
                            .padding(horizontal = 16.dp)
                    )
                    ActionRow(
                        icon = Icons.Default.OpenInNew,
                        title = "Open in Another App",
                        onClick = onOpenAnotherAppClick
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CardBorder)
                            .padding(horizontal = 16.dp)
                    )
                    ActionRow(
                        icon = Icons.Default.Folder,
                        title = "Go to Downloads",
                        onClick = onGoToDownloadsClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Saved File Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (result.isSaved) Color(0xFFE8F5E9) else Color(0xFFFFF0F0))
                    .border(1.dp, if (result.isSaved) Color(0xFFC8E6C9) else Color(0xFFFFD9D9), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.isSaved) Icons.Default.Check else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (result.isSaved) SuccessGreen else RedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (result.isSaved) "File saved successfully to:" else "Ready to export:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isSaved) SuccessGreen else RedPrimary
                        )
                        Text(
                            text = if (result.isSaved) (result.savedLocationDisplay ?: result.filePath) else "Tap 'Save' above to save to $saveLocationName",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .soundClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
