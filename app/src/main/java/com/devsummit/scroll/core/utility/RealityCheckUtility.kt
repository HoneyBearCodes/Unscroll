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

        val chapters = minutes / 30.0
        val calories = hours * 500.0

        return listOf(
            Achievement("Reading", chapters, "Book Chapters"),
            Achievement("Exercise", calories, "Calories Burned")
        )
    }

    fun getLifeLostProjection(dailyAverageMs: Long): String {
        val dailyHours = dailyAverageMs / (1000.0 * 60 * 60)
        val hoursPerYear = dailyHours * 365
        val daysPerYear = hoursPerYear / 24.0
        return "At this rate, you lose %.1f days per year to scrolling.".format(daysPerYear)
    }
}
