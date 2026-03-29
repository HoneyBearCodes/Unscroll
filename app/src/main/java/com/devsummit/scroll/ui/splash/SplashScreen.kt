package com.devsummit.scroll.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devsummit.scroll.R
import com.devsummit.scroll.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Logo scale + fade in (concurrent with alpha)
        kotlinx.coroutines.coroutineScope {
            launch {
                logoScale.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
            }
            launch {
                logoAlpha.animateTo(1f, animationSpec = tween(800))
            }
        }
        
        delay(200)
        
        // App name fade in
        textAlpha.animateTo(1f, animationSpec = tween(600))
        
        delay(100)
        
        // Subtitle fade in
        subtitleAlpha.animateTo(1f, animationSpec = tween(500))
        
        delay(800)
        
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepNavy, DarkSlate)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Unscroll Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "Unscroll",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = OffWhite,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Reclaim your time",
                style = MaterialTheme.typography.bodyLarge,
                color = Teal400,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }

        // Bottom credit
        Text(
            text = "by Team StrangeX",
            style = MaterialTheme.typography.labelSmall,
            color = SubtleGray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(subtitleAlpha.value)
        )
    }
}
