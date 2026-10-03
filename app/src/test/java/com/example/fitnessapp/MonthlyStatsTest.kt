package com.example.fitnessapp

import com.example.fitnessapp.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class MonthlyStatsTest {
    private val month = YearMonth.of(2026, 10)
    private fun item(vararg completed: Boolean) = WorkoutExerciseItem(exerciseId = 1, exerciseName = "Push", sets = completed.map { WorkoutSetItem(completed = it) })
    @Test fun totalsCountSessionsAndCompletedExerciseOccurrencesNotSetsOrUniqueIds() {
        val workouts = listOf(
            Workout(workoutDate = "2026-10-01", durationMinutes = 11, items = listOf(item(true, true), item(), item(true, false))),
            Workout(workoutDate = "2026-10-01", durationMinutes = 2, items = listOf(item(true))),
            Workout(workoutDate = "2025-10-01", durationMinutes = 100, items = listOf(item(true))),
            Workout(workoutDate = "2026-09-30", durationMinutes = 100))
        assertEquals(MonthlyStats(2, 13, 2), FitnessRules.monthStats(workouts, month))
        assertEquals(listOf(2, 0, 0, 0), FitnessRules.weeklyCounts(workouts, month))
    }
    @Test fun everyDayBelongsToExactlyOneOfFourBucketsIncluding29To31() {
        val workouts = (1..31).map { Workout(workoutDate = month.atDay(it).toString()) }
        assertEquals(listOf(7, 7, 7, 10), FitnessRules.weeklyCounts(workouts, month))
        assertEquals(FitnessRules.monthStats(workouts, month).totalWorkouts, FitnessRules.weeklyCounts(workouts, month).sum())
    }
    @Test fun leapFebruaryAndEmptyMonths() {
        val leap = YearMonth.of(2024, 2)
        val workouts = listOf(Workout(workoutDate = "2024-02-29"))
        assertEquals(listOf(0, 0, 0, 1), FitnessRules.weeklyCounts(workouts, leap))
        assertEquals(MonthlyStats(), FitnessRules.monthStats(workouts, month))
        assertEquals(listOf(0, 0, 0, 0), FitnessRules.weeklyCounts(emptyList(), month))
    }
    @Test fun malformedPersistedDateIsAnErrorNotAnEmptyMonth() {
        assertThrows(java.time.format.DateTimeParseException::class.java) {
            FitnessRules.monthStats(listOf(Workout(workoutDate = "invalid")), month)
        }
    }
}
