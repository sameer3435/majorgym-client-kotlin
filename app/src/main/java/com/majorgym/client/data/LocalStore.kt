package com.majorgym.client.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.LocalDate

/**
 * Port of the Flutter client's `lib/services/local_store.dart`.
 * Everything lives in SharedPreferences — no SQL, no cloud, matching the
 * original exactly:
 * - Member profile: one JSON blob, overwritten on every profile QR scan.
 * - Attendance: a map of "yyyy-MM-dd" -> "present". Any day not in the map
 *   is treated as absent. Only the last [HISTORY_DAYS] days are kept —
 *   older entries are pruned automatically.
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

    private fun writeMap(map: MutableMap<String, String>) {
        val cutoffDay = LocalDate.now().minusDays(HISTORY_DAYS.toLong())
        val it = map.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val d = runCatching { LocalDate.parse(entry.key) }.getOrNull()
            if (d != null && d.isBefore(cutoffDay)) it.remove()
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
        writeMap(map)
        return true
    }

    fun checkedInToday(): Boolean {
        val map = readMap()
        return map[dayKey(LocalDate.now())] == "present"
    }

    /** Present/absent status for the last [HISTORY_DAYS] days, most recent (today) first. */
    fun lastTwoMonths(): List<Pair<LocalDate, String>> {
        val map = readMap()
        val today = LocalDate.now()
        return (0 until HISTORY_DAYS).map { i ->
            val day = today.minusDays(i.toLong())
            val status = if (map[dayKey(day)] == "present") "present" else "absent"
            day to status
        }
    }

    /**
     * Consecutive days of "present" counting back from today. If today isn't
     * marked yet, counts back from yesterday instead (so the streak isn't
     * shown as broken before the day is even over).
     */
    fun currentStreak(): Int {
        val days = lastTwoMonths() // most recent first
        var streak = 0
        var startIndex = 0
        if (days.isNotEmpty() && days[0].second == "absent") {
            startIndex = 1
        }
        for (i in startIndex until days.size) {
            if (days[i].second == "present") {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    companion object {
        const val HISTORY_DAYS = 60
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
