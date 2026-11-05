package com.sjpixels.mantramala.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CounterEngineTest {
    @Test fun firstBeadStartsTheRound() {
        val r = CounterEngine.increment(CounterState(), now = 1_000)
        assertEquals(CounterState(1, 1_000), r.state)
        assertFalse(r.malaCompleted)
    }

    @Test fun roundStartIsKeptWhileCounting() {
        val r = CounterEngine.increment(CounterState(50, 1_000), now = 9_000)
        assertEquals(CounterState(51, 1_000), r.state)
    }

    @Test fun bead108CompletesMalaAndResetsToZero() {
        val r = CounterEngine.increment(CounterState(107, 1_000), now = 600_000)
        assertTrue(r.malaCompleted)
        assertEquals(CounterState(0, 0), r.state)
        assertEquals(1_000, r.roundStartedAt)
    }

    @Test fun counting108TimesFromZeroCompletesExactlyOnce() {
        var s = CounterState()
        var completions = 0
        repeat(108 * 3 + 5) { i ->
            val r = CounterEngine.increment(s, now = i.toLong() + 1)
            if (r.malaCompleted) completions++
            s = r.state
        }
        assertEquals(3, completions)
        assertEquals(5, s.count)
    }

    @Test fun decrementNeverGoesBelowZero() {
        assertEquals(CounterState(0, 0), CounterEngine.decrement(CounterState(0, 0)))
        assertEquals(CounterState(0, 0), CounterEngine.decrement(CounterState(1, 500)))
        assertEquals(CounterState(9, 500), CounterEngine.decrement(CounterState(10, 500)))
    }
}

class StatsCalculatorTest {
    private val today = LocalDate.of(2026, 11, 5)

    private fun rec(date: LocalDate, id: Long, minutes: Long = 10) = JapaRecord(
        id = id, mantraKey = "om", mantraLabel = "Om",
        startedAt = 1_000_000, completedAt = 1_000_000 + minutes * 60_000, dayKey = DayKeys.of(date),
    )

    @Test fun dayKeyRoundTrips() {
        assertEquals(20261105, DayKeys.of(today))
        assertEquals(today, DayKeys.toDate(20261105))
    }

    @Test fun streakCountsBackFromToday() {
        val days = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, StatsCalculator.currentStreak(days, today))
    }

    @Test fun streakSurvivesUntilTodaysPractice() {
        val days = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, StatsCalculator.currentStreak(days, today))
    }

    @Test fun streakBreaksAfterAMissedDay() {
        assertEquals(0, StatsCalculator.currentStreak(setOf(today.minusDays(2)), today))
    }

    @Test fun bestStreakFindsLongestRun() {
        val d = listOf(1, 2, 3, 7, 8, 9, 10, 15).map { LocalDate.of(2026, 1, it) }
        assertEquals(4, StatsCalculator.bestStreak(d))
    }

    @Test fun computeAggregatesTodayTotalsAndWeek() {
        val records = listOf(
            rec(today, 5), rec(today, 4), rec(today.minusDays(1), 3), rec(today.minusDays(10), 2, minutes = 600),
        ).sortedByDescending { it.dayKey }
        val s = StatsCalculator.compute(records, today)
        assertEquals(2, s.todayMalas)
        assertEquals(4, s.totalMalas)
        assertEquals(4L * 108, s.totalJapa)
        assertEquals(2, s.currentStreak)
        assertEquals(3, s.activeDays)
        assertEquals(7, s.last7Days.size)
        assertEquals(2, s.last7Days.last().malas)
        assertEquals(10L * 60_000, s.avgMalaMillis) // the 10-hour round is ignored as an outlier
        assertEquals(listOf(DayKeys.of(today), DayKeys.of(today.minusDays(1)), DayKeys.of(today.minusDays(10))), s.groups.map { it.dayKey })
    }
}

class MantrasTest {
    @Test fun defaultMantraIsOmNamahShivayaAndListedFirst() {
        assertEquals("om_namah_shivaya", Mantras.DEFAULT_KEY)
        assertEquals(Mantras.DEFAULT_KEY, Mantras.builtIn.first().key)
        assertEquals("ॐ नमः शिवाय", Mantras.builtIn(Mantras.DEFAULT_KEY)?.devanagari)
        assertEquals(UserSettings().selectedMantraKey, Mantras.DEFAULT_KEY)
    }

    @Test fun builtInKeysAreUnique() {
        assertEquals(Mantras.builtIn.size, Mantras.builtIn.map { it.key }.toSet().size)
    }
}

class HistoryPruningTest {
    private val today = LocalDate.of(2026, 11, 5)
    private fun group(daysAgo: Long, malas: Int): DayGroup {
        val d = today.minusDays(daysAgo)
        val key = DayKeys.of(d)
        return DayGroup(key, d, List(malas) { i ->
            JapaRecord(id = key * 10L + i, mantraKey = "om", mantraLabel = "Om", startedAt = 0, completedAt = 0, dayKey = key)
        })
    }
    private val groups = listOf(group(0, 3), group(1, 4), group(29, 2), group(31, 5), group(400, 6))

    @Test fun beforeTodayKeepsToday() = assertEquals(4 + 2 + 5 + 6, HistoryPruning.countToDelete(groups, ClearRange.BEFORE_TODAY, today))
    @Test fun olderThan30Days() = assertEquals(5 + 6, HistoryPruning.countToDelete(groups, ClearRange.OLDER_THAN_30_DAYS, today))
    @Test fun olderThanAYear() = assertEquals(6, HistoryPruning.countToDelete(groups, ClearRange.OLDER_THAN_1_YEAR, today))
    @Test fun everything() = assertEquals(20, HistoryPruning.countToDelete(groups, ClearRange.EVERYTHING, today))

    @Test fun firstDayIsEarliestPractice() {
        val records = groups.flatMap { it.records }.sortedByDescending { it.dayKey }
        assertEquals(today.minusDays(400), StatsCalculator.compute(records, today).firstDay)
    }
}

class MantraTotalsTest {
    private fun rec(key: String, label: String, at: Long) =
        JapaRecord(mantraKey = key, mantraLabel = label, startedAt = 0, completedAt = at, dayKey = 20261105)

    @Test fun groupsByMantraMostChantedFirst() {
        // Newest first, as the DAO returns them.
        val records = listOf(
            rec("om", "Om", 50),
            rec("custom:3", "My mantra (renamed)", 40),
            rec("om_namah_shivaya", "Shiva", 30),
            rec("custom:3", "My mantra", 20),
            rec("custom:3", "My mantra", 10),
        )
        val totals = StatsCalculator.mantraTotals(records)
        assertEquals(listOf("custom:3", "om", "om_namah_shivaya"), totals.map { it.mantraKey })
        assertEquals(3, totals[0].malas)
        assertEquals(324L, totals[0].japa)
        assertEquals("My mantra (renamed)", totals[0].label)
        // Ties are broken by the most recently chanted.
        assertEquals(50L, totals[1].lastCompletedAt)
    }

    @Test fun emptyHistoryHasNoTotals() {
        assertTrue(StatsCalculator.mantraTotals(emptyList()).isEmpty())
    }
}
