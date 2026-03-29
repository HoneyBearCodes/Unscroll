package com.devsummit.scroll.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devsummit.scroll.ui.theme.Teal400
import com.devsummit.scroll.ui.theme.Teal700
import com.devsummit.scroll.ui.theme.ElevatedSlate
import com.devsummit.scroll.ui.theme.OffWhite
import com.devsummit.scroll.ui.theme.MutedGray
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

    // Pulse glow when holding
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
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

    val shape = RoundedCornerShape(28.dp)
    val elevation = if (isPressed) 12.dp else 4.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (isPressed) Teal400.copy(alpha = glowAlpha) else Teal400.copy(alpha = 0.15f),
                spotColor = if (isPressed) Teal400.copy(alpha = glowAlpha) else Teal400.copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(ElevatedSlate)
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
        // Gradient progress fill
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progressRatio.coerceIn(0f, 1f))
                .height(56.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Teal700, Teal400)
                    )
                )
        )

        // Text
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isPressed) "Keep holding..." else "Hold ${durationMs / 1000}s to Proceed",
                color = if (progressRatio > 0.4f) OffWhite else MutedGray,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
