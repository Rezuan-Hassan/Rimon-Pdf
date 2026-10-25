package com.rimon.pdfmastersuite.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimon.pdfmastersuite.model.PdfCategory
import com.rimon.pdfmastersuite.model.PdfTool
import com.rimon.pdfmastersuite.ui.components.AppTopBar
import com.rimon.pdfmastersuite.ui.components.CategorySection
import com.rimon.pdfmastersuite.ui.components.ToolCard
import com.rimon.pdfmastersuite.ui.components.ToolSearchBar
import com.rimon.pdfmastersuite.ui.theme.Background
import com.rimon.pdfmastersuite.ui.theme.TextPrimary
import com.rimon.pdfmastersuite.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    onToolClick: (PdfTool) -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Map to track expanded states for categories (Organize & Convert open by default like in design)
    val expandedStates = remember {
        mutableStateMapOf(
            PdfCategory.ORGANIZE to true,
            PdfCategory.CONVERT to true,
            PdfCategory.SECURE to false,
            PdfCategory.OPTIMIZE to false,
            PdfCategory.OCR to false
        )
    }

    val allTools = remember { PdfTool.values().toList() }
    val filteredTools = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allTools.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                onSettingsClick = onSettingsClick,
                onMenuClick = onAboutClick
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar
            item {
                ToolSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // If user is searching, show direct search results grid
            if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "Search Results (${filteredTools.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (filteredTools.isEmpty()) {
                    item {
                        Text(
                            text = "No tools found matching '$searchQuery'",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    val chunks = filteredTools.chunked(4)
                    items(chunks) { rowTools ->
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (colIndex in 0 until 4) {
                                if (colIndex < rowTools.size) {
                                    val tool = rowTools[colIndex]
                                    ToolCard(
                                        tool = tool,
                                        onClick = { onToolClick(tool) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                // Category Accordions matching design.png
                PdfCategory.values().forEach { category ->
                    val toolsInCategory = allTools.filter { it.category == category }
                    val isExpanded = expandedStates[category] ?: false

                    item {
                        CategorySection(
                            category = category,
                            tools = toolsInCategory,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedStates[category] = !isExpanded
                            },
                            onToolClick = onToolClick
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
