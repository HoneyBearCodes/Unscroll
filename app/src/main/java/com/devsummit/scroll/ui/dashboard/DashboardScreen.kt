package com.devsummit.scroll.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devsummit.scroll.core.utility.RealityCheckUtility
import java.util.Calendar

// Chart colors
private val BarColorGoodStart = Color(0xFF66BB6A)
private val BarColorGoodEnd = Color(0xFF43A047)
private val BarColorBadStart = Color(0xFFEF5350)
private val BarColorBadEnd = Color(0xFFC62828)
private val GoalLineColor = Color(0xFFFFB300)
private val ChartLabelColor = Color(0xFF9E9E9E)

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
                android.util.Log.e("UnscrollDebug", "Dashboard Coroutine Crash", e)
            }
        }
        isLoading = false
    }

    val achievements = remember(todayUsageMs) { RealityCheckUtility.getAchievements(if (todayUsageMs == 0L) 1L else todayUsageMs) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Unscroll Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔥", style = MaterialTheme.typography.displayMedium)
                Text(
                    text = "$currentStreak Day Streak!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Daily Limit: ${dailyGoalMs / (60 * 1000)} mins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Button(
            onClick = onTestOverlayClick,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Preview Friction Overlay")
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Screen Time This Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Goal legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawLine(
                            color = GoalLineColor,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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

        Text(
            text = "What You Missed Today",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        achievements.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    }
}

@Composable
fun UsageBarChart(dataVals: List<Float>, dailyGoalMs: Long, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    
    // Animate bars entrance
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dataVals) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }
    
    // Compute day labels for last 7 days
    val dayLabels = remember {
        val labels = mutableListOf<String>()
        val cal = Calendar.getInstance()
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
    
    // Determine max Y value for scaling, ensuring at least the goal line fits
    val maxDataVal = if (dataVals.isEmpty()) goalHours else maxOf(dataVals.max(), goalHours)
    // Add 20% headroom so bars/goal don't touch the top
    val yMax = if (maxDataVal == 0f) 1f else maxDataVal * 1.2f

    val onSurface = MaterialTheme.colorScheme.onSurface
    
    Canvas(modifier = modifier) {
        val chartLeft = 0f
        val chartBottom = size.height - 28.dp.toPx() // space for labels
        val chartTop = 20.dp.toPx() // space for value labels above bars
        val chartHeight = chartBottom - chartTop
        val barCount = if (dataVals.isEmpty()) 7 else dataVals.size
        val totalBarArea = size.width - chartLeft
        val barSlotWidth = totalBarArea / barCount
        val barWidth = barSlotWidth * 0.55f
        val cornerRadius = 6.dp.toPx()
        
        // Draw light horizontal grid lines
        val gridLineCount = 3
        for (i in 1..gridLineCount) {
            val y = chartBottom - (chartHeight * i / (gridLineCount + 1))
            drawLine(
                color = onSurface.copy(alpha = 0.06f),
                start = Offset(chartLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw goal line (dashed)
        val goalY = chartBottom - (goalHours / yMax) * chartHeight
        if (goalY in chartTop..chartBottom) {
            drawLine(
                color = GoalLineColor,
                start = Offset(chartLeft, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )
        }
        
        // Draw bars and labels
        val values = if (dataVals.isEmpty()) List(7) { 0f } else dataVals
        values.forEachIndexed { index, hours ->
            val animatedHours = hours * animationProgress.value
            val barHeight = (animatedHours / yMax) * chartHeight
            val barX = chartLeft + barSlotWidth * index + (barSlotWidth - barWidth) / 2
            val barTop = chartBottom - barHeight
            
            val isOverGoal = hours > goalHours
            val barBrush = if (isOverGoal) {
                Brush.verticalGradient(
                    colors = listOf(BarColorBadStart, BarColorBadEnd),
                    startY = barTop,
                    endY = chartBottom
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(BarColorGoodStart, BarColorGoodEnd),
                    startY = barTop,
                    endY = chartBottom
                )
            }
            
            // Draw bar with rounded top corners
            if (barHeight > 0.5f) {
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(barX, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            } else {
                // Draw a tiny dot for zero-value days so they're still visible
                drawCircle(
                    color = onSurface.copy(alpha = 0.15f),
                    radius = 2.dp.toPx(),
                    center = Offset(barX + barWidth / 2, chartBottom - 2.dp.toPx())
                )
            }
            
            // Draw value label above bar (in minutes)
            val minutes = (hours * 60).toInt()
            val valueText = if (minutes == 0) "" else if (minutes < 60) "${minutes}m" else String.format("%.1fh", hours)
            if (valueText.isNotEmpty() && animationProgress.value > 0.8f) {
                val textLayoutResult = textMeasurer.measure(
                    text = valueText,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOverGoal) BarColorBadEnd else BarColorGoodEnd
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        barX + barWidth / 2 - textLayoutResult.size.width / 2,
                        barTop - textLayoutResult.size.height - 2.dp.toPx()
                    )
                )
            }
            
            // Draw day label below
            val dayLabel = dayLabels.getOrElse(index) { "" }
            val dayTextLayout = textMeasurer.measure(
                text = dayLabel,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = if (index == values.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == values.lastIndex) onSurface.copy(alpha = 0.9f) else ChartLabelColor
                )
            )
            drawText(
                textLayoutResult = dayTextLayout,
                topLeft = Offset(
                    barX + barWidth / 2 - dayTextLayout.size.width / 2,
                    chartBottom + 6.dp.toPx()
                )
            )
        }
    }
}

@Composable
fun AchievementCard(achievement: RealityCheckUtility.Achievement) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%.1f", achievement.amount),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
