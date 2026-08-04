package com.majorgym.client.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.LocalDate

/**
 * Port of the Flutter client's `lib/services/local_store.dart`.
 * Everything lives in SharedPreferences — no SQL, no cloud, matching the
 * original:
 * - Member profile: one JSON blob, overwritten on every profile QR scan.
 * - Attendance: a map of "yyyy-MM-dd" -> "present". Any day not in the map
 *   is treated as absent, UNLESS it's a Sunday, in which case it's treated
 *   as a "rest" day — never absent, and never breaks the streak.
 *
 * One deliberate change from the Dart source (a 60-day rolling window):
 * history here is kept from the member's [Member.joiningDate] (the day the
 * app was set up / they joined) up to a maximum of [MAX_HISTORY_DAYS]
 * (1 year) — older entries are only pruned once they fall outside both the
 * joining date AND the 1-year cap.
 */
class LocalStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveMember(member: Member) {
        prefs.edit().putString(MEMBER_KEY, member.toStorageJson()).apply()
    }

    fun getMember(): Member? {
        val raw = prefs.getString(MEMBER_KEY, null) ?: return null
        return Member.fromStorageJson(raw)
    }

    fun deleteMember() {
        prefs.edit().remove(MEMBER_KEY).apply()
    }

    private fun dayKey(d: LocalDate): String = d.toString() // yyyy-MM-dd

    private fun readMap(): LinkedHashMap<String, String> {
        val raw = prefs.getString(ATTENDANCE_KEY, null) ?: return LinkedHashMap()
        val decoded = JSONObject(raw)
        val map = LinkedHashMap<String, String>()
        decoded.keys().forEach { k -> map[k] = decoded.getString(k) }
        return map
    }

    /** Earliest day still kept: the later of the joining date and 1 year ago. */
    private fun cutoffDay(joiningDate: LocalDate?): LocalDate {
        val yearCutoff = LocalDate.now().minusDays((MAX_HISTORY_DAYS - 1).toLong())
        return if (joiningDate != null && joiningDate.isAfter(yearCutoff)) joiningDate else yearCutoff
    }

    private fun writeMap(map: MutableMap<String, String>, joiningDate: LocalDate?) {
        val cutoff = cutoffDay(joiningDate)
        val it = map.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val d = runCatching { LocalDate.parse(entry.key) }.getOrNull()
            if (d != null && d.isBefore(cutoff)) it.remove()
        }
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString(ATTENDANCE_KEY, o.toString()).apply()
    }

    /** Records today's attendance as present. Returns false if already marked today. */
    fun markAttendanceToday(): Boolean {
        val map = readMap()
        val key = dayKey(LocalDate.now())
        if (map[key] == "present") return false
        map[key] = "present"
        writeMap(map, getMember()?.joiningDate)
        return true
    }

    fun checkedInToday(): Boolean {
        val map = readMap()
        return map[dayKey(LocalDate.now())] == "present"
    }

    /**
     * Present/absent/rest status from [joiningDate] (or 1 year ago,
     * whichever is later) through today, most recent (today) first.
     * Sundays are always "rest" unless the member actually checked in.
     */
    fun attendanceHistory(joiningDate: LocalDate): List<Pair<LocalDate, String>> {
        val map = readMap()
        val today = LocalDate.now()
        val start = cutoffDay(joiningDate)
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt() + 1
        return (0 until totalDays).map { i ->
            val day = today.minusDays(i.toLong())
            val status = when {
                map[dayKey(day)] == "present" -> "present"
                day.dayOfWeek == java.time.DayOfWeek.SUNDAY -> "rest"
                else -> "absent"
            }
            day to status
        }
    }

    /**
     * Consecutive days counting back from today where the member was either
     * "present" or the day was a Sunday ("rest" — doesn't require a
     * check-in and never breaks the streak). If today isn't marked yet and
     * isn't a Sunday, counting starts from yesterday instead (so the streak
     * isn't shown as broken before the day is even over). Stops at
     * [joiningDate] since there's no attendance before that.
     */
    fun currentStreak(joiningDate: LocalDate): Int {
        val map = readMap()
        val today = LocalDate.now()
        var day = today
        if (day.dayOfWeek != java.time.DayOfWeek.SUNDAY && map[dayKey(day)] != "present") {
            day = day.minusDays(1)
        }
        var streak = 0
        while (!day.isBefore(joiningDate)) {
            val isRestDay = day.dayOfWeek == java.time.DayOfWeek.SUNDAY
            val present = map[dayKey(day)] == "present"
            if (present || isRestDay) {
                streak++
                day = day.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    companion object {
        /** Hard cap on how far back history is kept/shown, even if joining date is older. */
        const val MAX_HISTORY_DAYS = 365
        private const val PREFS_NAME = "majorgym_client_prefs"
        private const val MEMBER_KEY = "member_profile"
        private const val ATTENDANCE_KEY = "attendance_map"

        @Volatile private var INSTANCE: LocalStore? = null

        fun getInstance(context: Context): LocalStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalStore(context).also { INSTANCE = it }
            }
    }
}
