package com.rimon.pdfmastersuite.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rimon.pdfmastersuite.model.ProcessingState
import com.rimon.pdfmastersuite.ui.components.DetailTopBar
import com.rimon.pdfmastersuite.ui.theme.Background
import com.rimon.pdfmastersuite.ui.theme.RedPrimary
import com.rimon.pdfmastersuite.ui.theme.RedTint
import com.rimon.pdfmastersuite.ui.theme.SuccessGreen
import com.rimon.pdfmastersuite.ui.theme.Surface
import com.rimon.pdfmastersuite.ui.theme.TextMuted
import com.rimon.pdfmastersuite.ui.theme.TextPrimary
import com.rimon.pdfmastersuite.ui.theme.TextSecondary

@Composable
fun ProcessingScreen(
    state: ProcessingState.Processing,
    onBackClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progressPercent / 100f,
        label = "progress"
    )

    Scaffold(
        topBar = {
            DetailTopBar(
                title = "Processing...",
                onBackClick = onBackClick
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular Progress Indicator with centered percentage
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFFDADA),
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round
                )
                // Active Progress Track
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = RedPrimary,
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round
                )
                // Percentage Text in center
                Text(
                    text = "${state.progressPercent}%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Current operation title & subtitle
            Text(
                text = state.currentStepText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Please don't close the app",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Step Progress Checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Completed Steps
                state.completedSteps.forEach { step ->
                    StepItem(
                        title = step,
                        status = "Completed",
                        state = StepState.COMPLETED
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // In Progress Step
                if (state.activeStep.isNotEmpty()) {
                    StepItem(
                        title = state.activeStep,
                        status = "In progress",
                        state = StepState.IN_PROGRESS
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Pending Steps
                state.pendingSteps.forEach { step ->
                    StepItem(
                        title = step,
                        status = "Pending",
                        state = StepState.PENDING
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

enum class StepState {
    COMPLETED,
    IN_PROGRESS,
    PENDING
}

@Composable
fun StepItem(
    title: String,
    status: String,
    state: StepState
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (state) {
            StepState.COMPLETED -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Surface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            StepState.IN_PROGRESS -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(RedTint)
                        .border(2.dp, RedPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RedPrimary)
                    )
                }
            }
            StepState.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFD0D4DA), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (state == StepState.IN_PROGRESS) FontWeight.SemiBold else FontWeight.Normal,
                color = if (state == StepState.PENDING) TextMuted else TextPrimary
            )
            Text(
                text = status,
                fontSize = 11.sp,
                color = when (state) {
                    StepState.COMPLETED -> SuccessGreen
                    StepState.IN_PROGRESS -> RedPrimary
                    StepState.PENDING -> TextMuted
                }
            )
        }
    }
}
