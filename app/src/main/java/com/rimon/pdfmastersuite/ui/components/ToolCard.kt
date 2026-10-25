package com.rimon.pdfmastersuite.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimon.pdfmastersuite.model.PdfTool
import com.rimon.pdfmastersuite.ui.theme.CardBorder
import com.rimon.pdfmastersuite.ui.theme.RedPrimary
import com.rimon.pdfmastersuite.ui.theme.Surface
import com.rimon.pdfmastersuite.ui.theme.TextPrimary

@Composable
fun ToolCard(
    tool: PdfTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .soundClickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getToolIcon(tool),
                contentDescription = tool.title,
                tint = RedPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tool.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

fun getToolIcon(tool: PdfTool): ImageVector {
    return when (tool) {
        PdfTool.MERGE -> Icons.Default.Description
        PdfTool.SPLIT -> Icons.Default.CallSplit
        PdfTool.EXTRACT -> Icons.Default.VerticalAlignBottom
        PdfTool.REMOVE -> Icons.Default.DeleteOutline
        PdfTool.ROTATE -> Icons.Default.RotateRight
        PdfTool.REORDER -> Icons.Default.OpenWith
        PdfTool.WATERMARK -> Icons.Default.AutoFixHigh
        PdfTool.PAGE_NUMBERS -> Icons.Default.FormatListNumbered
        PdfTool.IMAGES_TO_PDF -> Icons.Default.PhotoLibrary
        PdfTool.PDF_TO_IMAGES -> Icons.Default.Image
        PdfTool.PDF_TO_TEXT -> Icons.Default.TextFields
        PdfTool.WORD_TO_PDF -> Icons.Default.Description
        PdfTool.EXCEL_TO_PDF -> Icons.Default.TableChart
        PdfTool.PROTECT -> Icons.Default.Lock
        PdfTool.UNLOCK -> Icons.Default.LockOpen
        PdfTool.COMPRESS -> Icons.Default.Compress
        PdfTool.OCR_IMAGE -> Icons.Default.DocumentScanner
        PdfTool.OCR_PDF -> Icons.Default.GridOn
    }
}
