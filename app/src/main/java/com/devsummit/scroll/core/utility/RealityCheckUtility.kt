package com.devsummit.scroll.core.utility

object RealityCheckUtility {

    data class Achievement(
        val type: String,
        val amount: Double,
        val description: String
    )

    fun getAchievements(milliseconds: Long): List<Achievement> {
        val minutes = milliseconds / (1000 * 60)
        val hours = minutes / 60.0

        return listOf(
            Achievement("Reading", (minutes / 30.0), "Book Chapters"),
            Achievement("Exercise", (hours * 500.0), "Calories Burned"),
            Achievement("Language", (minutes / 15.0), "Languages Lessons"),
            Achievement("Walking", (minutes / 20.0), "Miles Walked"),
            Achievement("Learning", (minutes / 1.5), "Pages Read"),
            Achievement("Entertainment", (hours / 2.0), "Movies Watched")
        )
    }

    fun getLifeLostProjection(dailyAverageMs: Long): String {
        val dailyHours = dailyAverageMs / (1000.0 * 60 * 60)
        val hoursPerYear = dailyHours * 365
        val daysPerYear = hoursPerYear / 24.0
        return "At this rate, you lose %.1f days per year to scrolling.".format(daysPerYear)
    }
}
