package com.devsummit.scroll.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun HoldToConfirmButton(
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    durationMs: Long = 5000L
) {
    var isPressed by remember { mutableStateOf(false) }
    var progressMs by remember { mutableLongStateOf(0L) }

    val progressRatio by animateFloatAsState(
        targetValue = progressMs.toFloat() / durationMs.toFloat(),
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = "progressRatio"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            while (progressMs < durationMs) {
                delay(50)
                progressMs += 50
            }
            if (progressMs >= durationMs) {
                onConfirm()
                isPressed = false
                progressMs = 0L
            }
        } else {
            progressMs = 0L
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress background
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progressRatio.coerceIn(0f, 1f))
                .height(56.dp)
                .background(MaterialTheme.colorScheme.primary)
        )

        // Text
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val textColor = if (progressRatio > 0.5f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = if (isPressed) "Keep holding..." else "Hold 5s to Proceed",
                color = textColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
