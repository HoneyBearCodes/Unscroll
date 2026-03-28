package com.devsummit.scroll.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
    
    // Default goal is 60 minutes
    var dailyGoalMinutes by remember { 
        mutableStateOf(prefs.getLong("daily_goal_ms", 60L * 60 * 1000) / (60 * 1000).toFloat()) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Daily Screen Limit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How much total time do you want to intentionally spend on your Blocked Apps per day?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "${dailyGoalMinutes.roundToInt()} Minutes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Slider(
                    value = dailyGoalMinutes,
                    onValueChange = { dailyGoalMinutes = it },
                    onValueChangeFinished = {
                        val ms = (dailyGoalMinutes.roundToInt() * 60 * 1000).toLong()
                        prefs.edit().putLong("daily_goal_ms", ms).apply()
                    },
                    valueRange = 5f..180f,
                    steps = 34, // 5 min increments up to 180
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
