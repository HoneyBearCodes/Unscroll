package com.devsummit.scroll.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devsummit.scroll.core.utility.RealityCheckUtility
import com.devsummit.scroll.ui.theme.*
import java.util.Calendar

@Composable
fun DashboardScreen(blacklistedApps: Set<String>, onTestOverlayClick: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var todayUsageMs by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
    var weeklyData by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<Float>>(emptyList()) }
    var currentStreak by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    var dailyGoalMs by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(60L * 60 * 1000L) }
    var isLoading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var errorMessage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    
    androidx.compose.runtime.LaunchedEffect(blacklistedApps) {
        isLoading = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                errorMessage = ""
                val prefs = context.getSharedPreferences("unscroll_prefs", android.content.Context.MODE_PRIVATE)
                val goal = prefs.getLong("daily_goal_ms", 60L * 60 * 1000)
                dailyGoalMs = goal

                val engine = com.devsummit.scroll.core.usage.UsageEngine(context)
                todayUsageMs = engine.getTodayUsageInMilliseconds(blacklistedApps)
                weeklyData = engine.getWeeklyUsage(blacklistedApps)
                currentStreak = engine.calculateCurrentStreak(blacklistedApps, goal)
            } catch (e: Exception) {
                errorMessage = "${e.javaClass.simpleName}: ${e.message}"
            }
        }
        isLoading = false
    }

    val achievements = remember(todayUsageMs) { RealityCheckUtility.getAchievements(if (todayUsageMs == 0L) 1L else todayUsageMs) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ── Header ──
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 20.dp, top = 8.dp)
        )

        // ── Streak Card ──
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepTeal, CardSurface)
                        )
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Streak icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(StreakOrange, StreakOrangeDeep)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Whatshot,
                            contentDescription = "Streak",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "$currentStreak Day Streak",
                            style = MaterialTheme.typography.headlineMedium,
                            color = OffWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Teal400)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Limit: ${dailyGoalMs / (60 * 1000)} mins",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedGray
                            )
                        }
                    }
                }
            }
        }

        // ── Preview Overlay Button ──
        OutlinedButton(
            onClick = onTestOverlayClick,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal400),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(listOf(Teal400.copy(alpha = 0.5f), Teal400.copy(alpha = 0.2f)))
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Preview Friction Overlay",
                style = MaterialTheme.typography.labelLarge
            )
        }

        // ── Chart Card ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Screen Time This Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OffWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Goal legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawLine(
                            color = Amber400,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Daily limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Teal400)
                    }
                } else if (errorMessage.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(errorMessage, color = LimitRed, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    UsageBarChart(
                        dataVals = weeklyData,
                        dailyGoalMs = dailyGoalMs,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            }
        }

        // ── What You Missed Section ──
        Text(
            text = "What You Missed Today",
            style = MaterialTheme.typography.titleLarge,
            color = OffWhite,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        achievements.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { achievement ->
                    Box(modifier = Modifier.weight(1f)) {
                        AchievementCard(achievement)
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun UsageBarChart(dataVals: List<Float>, dailyGoalMs: Long, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dataVals) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }
    
    val dayLabels = remember {
        val labels = mutableListOf<String>()
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            labels.add(
                when (dayCal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    Calendar.SUNDAY -> "Sun"
                    else -> ""
                }
            )
        }
        labels
    }
    
    val goalHours = dailyGoalMs / (1000f * 60f * 60f)
    val maxDataVal = if (dataVals.isEmpty()) goalHours else maxOf(dataVals.max(), goalHours)
    val yMax = if (maxDataVal == 0f) 1f else maxDataVal * 1.2f

    Canvas(modifier = modifier) {
        val chartBottom = size.height - 28.dp.toPx()
        val chartTop = 20.dp.toPx()
        val chartHeight = chartBottom - chartTop
        val barCount = if (dataVals.isEmpty()) 7 else dataVals.size
        val barSlotWidth = size.width / barCount
        val barWidth = barSlotWidth * 0.5f
        val cornerRadius = 6.dp.toPx()
        
        // Subtle grid lines
        for (i in 1..3) {
            val y = chartBottom - (chartHeight * i / 4)
            drawLine(
                color = Color.White.copy(alpha = 0.04f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Goal line
        val goalY = chartBottom - (goalHours / yMax) * chartHeight
        if (goalY in chartTop..chartBottom) {
            drawLine(
                color = Amber400,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )
        }
        
        val values = if (dataVals.isEmpty()) List(7) { 0f } else dataVals
        values.forEachIndexed { index, hours ->
            val animatedHours = hours * animationProgress.value
            val barHeight = (animatedHours / yMax) * chartHeight
            val barX = barSlotWidth * index + (barSlotWidth - barWidth) / 2
            val barTop = chartBottom - barHeight
            
            val isOverGoal = hours > goalHours
            val barBrush = if (isOverGoal) {
                Brush.verticalGradient(
                    colors = listOf(GradientRedStart, GradientRedEnd),
                    startY = barTop, endY = chartBottom
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(GradientGreenStart, GradientGreenEnd),
                    startY = barTop, endY = chartBottom
                )
            }
            
            if (barHeight > 0.5f) {
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(barX, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            } else {
                drawCircle(
                    color = MutedGray.copy(alpha = 0.3f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(barX + barWidth / 2, chartBottom - 2.dp.toPx())
                )
            }
            
            // Value label
            val minutes = (hours * 60).toInt()
            val valueText = if (minutes == 0) "" else if (minutes < 60) "${minutes}m" else String.format("%.1fh", hours)
            if (valueText.isNotEmpty() && animationProgress.value > 0.8f) {
                val textLayout = textMeasurer.measure(
                    text = valueText,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOverGoal) LimitRed else SafeGreen
                    )
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        barX + barWidth / 2 - textLayout.size.width / 2,
                        barTop - textLayout.size.height - 2.dp.toPx()
                    )
                )
            }
            
            // Day label
            val dayLabel = dayLabels.getOrElse(index) { "" }
            val isToday = index == values.lastIndex
            val dayLayout = textMeasurer.measure(
                text = dayLabel,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) OffWhite else MutedGray
                )
            )
            drawText(
                textLayoutResult = dayLayout,
                topLeft = Offset(
                    barX + barWidth / 2 - dayLayout.size.width / 2,
                    chartBottom + 6.dp.toPx()
                )
            )
        }
    }
}

@Composable
fun AchievementCard(achievement: RealityCheckUtility.Achievement) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with tinted background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Teal400.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = achievement.type,
                    tint = Teal400,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = String.format("%.1f", achievement.amount),
                style = MaterialTheme.typography.headlineMedium,
                color = OffWhite
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray
            )
        }
    }
}
