package com.majorgym.client.data

import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Port of the Flutter client's `lib/models/member.dart`. Dates are kept as
 * [LocalDate] (day precision) since the original only ever compares/stores
 * whole days.
 *
 * Matches the Dart source's logic exactly: every scan of the "join / renew"
 * QR is treated as a fresh, self-contained profile — [joiningDate] comes
 * straight from that QR's `joiningDate` field (or today, if it's missing),
 * and [expiryDate] is always derived from [joiningDate] + plan duration.
 * There is no separate "renewed date" and no merging with whatever profile
 * was cached before; a new scan simply overwrites it.
 */
data class Member(
    val name: String,
    val phone: String,
    val id: String,
    val joiningDate: LocalDate,
    val expiryDate: LocalDate,
    val planLabel: String = "",
) {
    /** True once today is strictly after the expiry date. */
    val isExpired: Boolean get() = daysRemaining < 0

    /** Days between today and [expiryDate]. Not clamped — can be negative. */
    val daysRemaining: Long
        get() = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)

    fun toStorageJson(): String {
        val o = JSONObject()
        o.put("name", name)
        o.put("phone", phone)
        o.put("id", id)
        o.put("joiningDate", joiningDate.format(ISO))
        o.put("expiryDate", expiryDate.format(ISO))
        o.put("planLabel", planLabel)
        return o.toString()
    }

    companion object {
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun fromStorageJson(raw: String): Member {
            val map = JSONObject(raw)
            return Member(
                name = map.optString("name", ""),
                phone = map.optString("phone", ""),
                id = map.optString("id", ""),
                joiningDate = LocalDate.parse(map.getString("joiningDate")),
                expiryDate = LocalDate.parse(map.getString("expiryDate")),
                planLabel = map.optString("planLabel", ""),
            )
        }

        /**
         * Parses the JSON payload carried by the "join / renew" QR code.
         * Expected keys (case-insensitive): name, phone, id, joiningDate,
         * plus whatever the owner app includes to describe the plan (plan,
         * planDays, planMonths, durationDays, expiryDate/renewalDate).
         */
        fun fromQrJson(json: JSONObject): Member {
            val map = HashMap<String, Any?>()
            json.keys().forEach { k -> map[k.lowercase()] = json.get(k) }

            val joining = parseDate(map["joiningdate"]) ?: LocalDate.now()
            val plan = (map["plan"] ?: map["planname"] ?: "").toString()
            val expiry = resolveExpiry(map, joining)

            return Member(
                name = (map["name"] ?: "").toString(),
                phone = (map["phone"] ?: "").toString(),
                id = (map["id"] ?: "").toString(),
                joiningDate = joining,
                expiryDate = expiry,
                planLabel = plan,
            )
        }

        private fun parseDate(value: Any?): LocalDate? {
            if (value == null) return null
            return try {
                LocalDate.parse(value.toString().substring(0, minOf(10, value.toString().length)))
            } catch (e: DateTimeParseException) {
                null
            } catch (e: Exception) {
                null
            }
        }

        /**
         * The expiry/renewal date coming straight from the QR isn't always
         * trustworthy (it was showing the same day as joining). Source of
         * truth is joining date + plan duration. We only trust an explicit
         * expiry date if it's actually after the joining date; otherwise we
         * compute it from whatever plan info is available, falling back to
         * 1 month.
         */
        private fun resolveExpiry(map: Map<String, Any?>, joining: LocalDate): LocalDate {
            val explicit = parseDate(map["expirydate"]) ?: parseDate(map["renewaldate"])
            if (explicit != null && explicit.isAfter(joining)) return explicit

            val days = asInt(map["durationdays"] ?: map["plandays"])
            if (days != null) return joining.plusDays(days.toLong())

            val months = asInt(map["planmonths"] ?: map["months"])
            if (months != null) return joining.plusMonths(months.toLong())

            val planText = (map["plan"] ?: "").toString()
            val compute = parsePlanDuration(planText)
            if (compute != null) return compute(joining)

            return joining.plusMonths(1)
        }

        private fun asInt(v: Any?): Int? {
            if (v == null) return null
            if (v is Int) return v
            if (v is Number) return v.toInt()
            return v.toString().toIntOrNull()
        }

        private val PLAN_DURATION_REGEX =
            Regex("""(\d+)\s*(day|week|month|year)""", RegexOption.IGNORE_CASE)

        private fun parsePlanDuration(text: String): ((LocalDate) -> LocalDate)? {
            val match = PLAN_DURATION_REGEX.find(text) ?: return null
            val n = match.groupValues[1].toLong()
            return when (match.groupValues[2].lowercase()) {
                "day" -> { d: LocalDate -> d.plusDays(n) }
                "week" -> { d: LocalDate -> d.plusDays(n * 7) }
                "month" -> { d: LocalDate -> d.plusMonths(n) }
                "year" -> { d: LocalDate -> d.plusYears(n) }
                else -> null
            }
        }
    }
}
