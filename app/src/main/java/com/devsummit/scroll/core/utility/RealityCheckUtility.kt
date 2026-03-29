package com.devsummit.scroll.core.utility

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.graphics.vector.ImageVector

object RealityCheckUtility {

    data class Achievement(
        val type: String,
        val amount: Double,
        val description: String,
        val icon: ImageVector
    )

    fun getAchievements(milliseconds: Long): List<Achievement> {
        val minutes = milliseconds / (1000 * 60)
        val hours = minutes / 60.0

        return listOf(
            Achievement("Reading", (minutes / 30.0), "Book Chapters", Icons.Outlined.AutoStories),
            Achievement("Exercise", (hours * 500.0), "Calories Burned", Icons.Outlined.FitnessCenter),
            Achievement("Language", (minutes / 15.0), "Language Lessons", Icons.Outlined.Translate),
            Achievement("Walking", (minutes / 12.0), "Km Walked", Icons.Outlined.DirectionsWalk),
            Achievement("Learning", (minutes / 45.0), "Podcast Episodes", Icons.Outlined.Headphones),
            Achievement("Entertainment", (hours / 2.0), "Movies Watched", Icons.Outlined.Movie)
        )
    }

    fun getLifeLostProjection(dailyAverageMs: Long): String {
        val dailyHours = dailyAverageMs / (1000.0 * 60 * 60)
        val hoursPerYear = dailyHours * 365
        val daysPerYear = hoursPerYear / 24.0
        return "At this rate, you lose %.1f days per year to scrolling.".format(daysPerYear)
    }
}
