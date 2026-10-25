package com.rimon.pdfmastersuite.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.rimon.pdfmastersuite.audio.SoundManager

/**
 * Custom clickable modifier that plays a subtle click sound and triggers haptic feedback.
 */
fun Modifier.soundClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }
    val interactionSource = remember { MutableInteractionSource() }

    this.clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.material.ripple.rememberRipple(),
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role
    ) {
        soundManager.playClick()
        onClick()
    }
}
