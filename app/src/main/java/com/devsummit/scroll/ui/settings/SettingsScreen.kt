package com.devsummit.scroll.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devsummit.scroll.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("unscroll_prefs", Context.MODE_PRIVATE)
    
    var dailyGoalMinutes by remember { 
        mutableStateOf(prefs.getLong("daily_goal_ms", 60L * 60 * 1000) / (60 * 1000).toFloat()) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Teal400.copy(alpha = 0.3f), Teal400.copy(alpha = 0.08f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = "Timer",
                            tint = Teal400,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Daily Screen Limit",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OffWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total time allowed on blocked apps per day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // Big number display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElevatedSlate)
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${dailyGoalMinutes.roundToInt()}",
                            style = MaterialTheme.typography.displayMedium,
                            color = Teal400,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "minutes",
                            style = MaterialTheme.typography.titleMedium,
                            color = MutedGray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Slider(
                    value = dailyGoalMinutes,
                    onValueChange = { dailyGoalMinutes = it },
                    onValueChangeFinished = {
                        val ms = (dailyGoalMinutes.roundToInt() * 60 * 1000).toLong()
                        prefs.edit().putLong("daily_goal_ms", ms).apply()
                    },
                    valueRange = 5f..180f,
                    steps = 34,
                    colors = SliderDefaults.colors(
                        thumbColor = Teal400,
                        activeTrackColor = Teal400,
                        inactiveTrackColor = ElevatedSlate,
                        activeTickColor = Teal700,
                        inactiveTickColor = SubtleGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5 min", style = MaterialTheme.typography.labelSmall, color = SubtleGray)
                    Text("3 hours", style = MaterialTheme.typography.labelSmall, color = SubtleGray)
                }
            }
        }
    }
}
