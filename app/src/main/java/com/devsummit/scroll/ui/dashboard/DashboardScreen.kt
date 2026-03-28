package com.devsummit.scroll.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.devsummit.scroll.core.utility.RealityCheckUtility
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

@Composable
fun DashboardScreen(blacklistedApps: Set<String>, onTestOverlayClick: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var todayUsageMs by androidx.compose.runtime.mutableStateOf(0L)
    var weeklyData by androidx.compose.runtime.mutableStateOf<List<Float>>(emptyList())
    var currentStreak by androidx.compose.runtime.mutableStateOf(0)
    var dailyGoalMs by androidx.compose.runtime.mutableStateOf(60L * 60 * 1000)

    androidx.compose.runtime.LaunchedEffect(blacklistedApps) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("unscroll_prefs", android.content.Context.MODE_PRIVATE)
                val goal = prefs.getLong("daily_goal_ms", 60L * 60 * 1000)
                dailyGoalMs = goal

                val engine = com.devsummit.scroll.core.usage.UsageEngine(context)
                todayUsageMs = engine.getTodayUsageInMilliseconds(blacklistedApps)
                weeklyData = engine.getWeeklyUsage(blacklistedApps)
                currentStreak = engine.calculateCurrentStreak(blacklistedApps, goal)
            } catch (e: Exception) {
                android.util.Log.e("UnscrollDebug", "Dashboard Coroutine Crash", e)
                // Ignore UsageStatsManager occasional transaction too large errors in extreme cases
            }
        }
    }

    val achievements = remember(todayUsageMs) { RealityCheckUtility.getAchievements(if (todayUsageMs == 0L) 1L else todayUsageMs) }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                .height(250.dp)
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Life Spent on Blocked Apps (Last 7 Days)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (weeklyData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    UsageBarChart(dataVals = weeklyData, dailyGoalMs = dailyGoalMs, modifier = Modifier.fillMaxSize())
                }
            }
        }

        Text(
            text = "What You Missed Today",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(achievements) { achievement ->
                AchievementCard(achievement)
            }
        }
    }
}

@Composable
fun UsageBarChart(dataVals: List<Float>, dailyGoalMs: Long, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                
                axisLeft.axisMinimum = 0f  // Force Y-axis to start at 0
                axisRight.isEnabled = false // Hide the secondary Y-axis on the right
                
                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f     // Force X-axis labels to strictly be integer days
            }
        },
        update = { chart ->
            if (dataVals.isEmpty()) return@AndroidView
            
            val entries = ArrayList<BarEntry>()
            dataVals.forEachIndexed { index, hours ->
                entries.add(BarEntry((index + 1).toFloat(), hours))
            }

            val dataSet = BarDataSet(entries, "Hours Spent")
            
            val goalHours = dailyGoalMs / (1000f * 60f * 60f)
            val colors = dataVals.map { 
                if (it > goalHours) android.graphics.Color.parseColor("#E53935") // Red
                else android.graphics.Color.parseColor("#43A047") // Green
            }
            dataSet.colors = colors

            chart.data = BarData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun AchievementCard(achievement: RealityCheckUtility.Achievement) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
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
