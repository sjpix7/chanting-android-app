package com.sjpixels.mantramala.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DayKeys {
    fun of(date: LocalDate): Int = date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
    fun toDate(key: Int): LocalDate = LocalDate.of(key / 10000, (key / 100) % 100, key % 100)
    fun fromMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
        of(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())
}

/** Result of counting one bead. */
data class CountResult(val state: CounterState, val malaCompleted: Boolean, val roundStartedAt: Long)

/** Pure counting rules, kept free of Android so they can be unit tested. */
object CounterEngine {
    fun increment(current: CounterState, now: Long): CountResult {
        val start = if (current.count == 0 || current.roundStartedAt == 0L) now else current.roundStartedAt
        val next = current.count + 1
        return if (next >= MALA_SIZE) {
            CountResult(CounterState(0, 0L), malaCompleted = true, roundStartedAt = start)
        } else {
            CountResult(CounterState(next, start), malaCompleted = false, roundStartedAt = start)
        }
    }

    fun decrement(current: CounterState): CounterState = when {
        current.count <= 0 -> current
        current.count == 1 -> CounterState(0, 0L)
        else -> current.copy(count = current.count - 1)
    }
}

data class DayBar(val date: LocalDate, val malas: Int)

/** What "Clear past history" removes. */
enum class ClearRange { BEFORE_TODAY, OLDER_THAN_30_DAYS, OLDER_THAN_1_YEAR, EVERYTHING }

object HistoryPruning {
    /** First day that is KEPT for [range], or null when everything is deleted. */
    fun keepFrom(range: ClearRange, today: LocalDate): LocalDate? = when (range) {
        ClearRange.BEFORE_TODAY -> today
        ClearRange.OLDER_THAN_30_DAYS -> today.minusDays(30)
        ClearRange.OLDER_THAN_1_YEAR -> today.minusYears(1)
        ClearRange.EVERYTHING -> null
    }

    /** How many malas [range] would delete. */
    fun countToDelete(groups: List<DayGroup>, range: ClearRange, today: LocalDate): Int {
        val keep = keepFrom(range, today)?.let(DayKeys::of)
        return groups.filter { keep == null || it.dayKey < keep }.sumOf { it.records.size }
    }
}

data class DayGroup(val dayKey: Int, val date: LocalDate, val records: List<JapaRecord>)

/** Lifetime total for one mantra. [label] is the newest name snapshot (used for custom or deleted mantras). */
data class MantraTotal(val mantraKey: String, val label: String, val malas: Int, val lastCompletedAt: Long) {
    val japa: Long get() = malas.toLong() * MALA_SIZE
}

data class PracticeStats(
    val todayMalas: Int = 0,
    val totalMalas: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val activeDays: Int = 0,
    /** Average minutes per mala, ignoring rounds left open for hours. */
    val avgMalaMillis: Long = 0L,
    val last7Days: List<DayBar> = emptyList(),
    val groups: List<DayGroup> = emptyList(),
    /** The earliest day with a completed mala, or null if there is no history. */
    val firstDay: LocalDate? = null,
    /** Lifetime malas per mantra, most-chanted first. */
    val byMantra: List<MantraTotal> = emptyList(),
) {
    val totalJapa: Long get() = totalMalas.toLong() * MALA_SIZE
}

object StatsCalculator {
    private const val MAX_REASONABLE_ROUND_MS = 3L * 60 * 60 * 1000

    /** [records] must be sorted newest first (as the DAO returns them). */
    fun compute(records: List<JapaRecord>, today: LocalDate): PracticeStats {
        val todayKey = DayKeys.of(today)
        val byDay = LinkedHashMap<Int, MutableList<JapaRecord>>()
        for (r in records) byDay.getOrPut(r.dayKey) { mutableListOf() }.add(r)

        val days = byDay.keys.map(DayKeys::toDate).toSortedSet()
        val durations = records.asSequence()
            .filter { it.startedAt > 0 }
            .map { it.completedAt - it.startedAt }
            .filter { it in 1..MAX_REASONABLE_ROUND_MS }
            .toList()

        val last7 = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            DayBar(d, byDay[DayKeys.of(d)]?.size ?: 0)
        }

        return PracticeStats(
            todayMalas = byDay[todayKey]?.size ?: 0,
            totalMalas = records.size,
            currentStreak = currentStreak(days, today),
            bestStreak = bestStreak(days.toList()),
            activeDays = days.size,
            avgMalaMillis = if (durations.isEmpty()) 0L else durations.average().toLong(),
            last7Days = last7,
            groups = byDay.entries
                .sortedByDescending { it.key }
                .map { (key, list) -> DayGroup(key, DayKeys.toDate(key), list) },
            firstDay = days.firstOrNull(),
            byMantra = mantraTotals(records),
        )
    }

    /** [records] newest first, so the first record of each mantra carries its latest name. */
    fun mantraTotals(records: List<JapaRecord>): List<MantraTotal> =
        records.groupBy { it.mantraKey }
            .map { (key, list) -> MantraTotal(key, list.first().mantraLabel, list.size, list.first().completedAt) }
            .sortedWith(compareByDescending<MantraTotal> { it.malas }.thenByDescending { it.lastCompletedAt })

    /** Consecutive days ending today — or yesterday, so the streak isn't "lost" before today's practice. */
    fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var cursor = when {
            today in days -> today
            today.minusDays(1) in days -> today.minusDays(1)
            else -> return 0
        }
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** [sortedDays] ascending, distinct. */
    fun bestStreak(sortedDays: List<LocalDate>): Int {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in sortedDays) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        return best
    }
}
