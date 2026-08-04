package com.majorgym.client.data

import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Port of the Flutter client's `lib/models/member.dart`, with one
 * deliberate addition to match the owner app's data model (see the
 * "MEMBER PROFILE" screen there, which shows "Joined" and "Renewed" as two
 * distinct fields): a [renewedDate] separate from [joiningDate]. Dates are
 * kept as [LocalDate] (day precision) since the source only ever
 * compares/stores whole days.
 *
 * Date fields, and the rules that govern them:
 *  - [joiningDate]: the member's ORIGINAL join date. Set once, on the very
 *    first scan, and never touched again — renewals must never move it.
 *  - [renewedDate]: the date of the LATEST renewal. On a first-ever scan
 *    this equals [joiningDate]; every renewal after that overwrites it with
 *    that renewal's date.
 *  - [expiryDate]: always derived as `renewedDate + plan duration`.
 */
data class Member(
    val name: String,
    val phone: String,
    val id: String,
    val joiningDate: LocalDate,
    val renewedDate: LocalDate,
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
        o.put("renewedDate", renewedDate.format(ISO))
        o.put("expiryDate", expiryDate.format(ISO))
        o.put("planLabel", planLabel)
        return o.toString()
    }

    companion object {
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun fromStorageJson(raw: String): Member {
            val map = JSONObject(raw)
            val joining = LocalDate.parse(map.getString("joiningDate"))
            // Back-compat: profiles saved before "renewedDate" existed
            // don't have it yet — fall back to joiningDate so old installs
            // don't crash on the first read after an app update.
            val renewed = if (map.has("renewedDate")) {
                LocalDate.parse(map.getString("renewedDate"))
            } else {
                joining
            }
            return Member(
                name = map.optString("name", ""),
                phone = map.optString("phone", ""),
                id = map.optString("id", ""),
                joiningDate = joining,
                renewedDate = renewed,
                expiryDate = LocalDate.parse(map.getString("expiryDate")),
                planLabel = map.optString("planLabel", ""),
            )
        }

        /**
         * Parses the JSON payload carried by the "join / renew" QR code.
         * Expected keys (case-insensitive): name, phone, id, joiningDate,
         * renewalDate, plus whatever the owner app includes to describe the
         * plan (plan, planDays, planMonths, durationDays, expiryDate).
         *
         * [existing] is the member profile already cached on this device,
         * if any:
         *  - When null, this scan is the member's first-ever join: the
         *    joining date comes from the QR (or today), and the renewed
         *    date starts out equal to it.
         *  - When non-null, this scan is a RENEWAL of that same member:
         *    [Member.joiningDate] is carried over unchanged, and the
         *    renewed date becomes this renewal's date (from the QR, or
         *    today if the QR doesn't say).
         *
         * Either way, expiry is always computed as
         * `renewedDate + plan duration`.
         */
        fun fromQrJson(json: JSONObject, existing: Member? = null): Member {
            val map = HashMap<String, Any?>()
            json.keys().forEach { k -> map[k.lowercase()] = json.get(k) }

            val plan = (map["plan"] ?: map["planname"] ?: "").toString()

            val joining = existing?.joiningDate
                ?: parseDate(map["joiningdate"])
                ?: LocalDate.now()

            val renewed = parseDate(map["renewaldate"])
                ?: parseDate(map["reneweddate"])
                ?: if (existing != null) LocalDate.now() else joining

            val expiry = resolveExpiry(map, renewed)

            return Member(
                name = strOrNull(map["name"]) ?: existing?.name ?: "",
                phone = strOrNull(map["phone"]) ?: existing?.phone ?: "",
                id = strOrNull(map["id"]) ?: existing?.id ?: "",
                joiningDate = joining,
                renewedDate = renewed,
                expiryDate = expiry,
                planLabel = plan.ifEmpty { existing?.planLabel ?: "" },
            )
        }

        private fun strOrNull(v: Any?): String? {
            if (v == null || v == JSONObject.NULL) return null
            val s = v.toString()
            return s.ifBlank { null }
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
         * trustworthy. Source of truth is [base] (the resolved renewal
         * date) + plan duration, computed from whatever plan info is
         * available (explicit day/month count, or the plan text, e.g.
         * "1 Month"). An explicit "expiryDate" field in the QR is only used
         * as a last resort, when the QR carries no plan/duration info at
         * all to compute from — it must NEVER override a real plan
         * duration, since that field has been observed to disagree with
         * the plan (e.g. plan "1 Month" but expiryDate over a year out).
         */
        private fun resolveExpiry(map: Map<String, Any?>, base: LocalDate): LocalDate {
            val days = asInt(map["durationdays"] ?: map["plandays"])
            if (days != null) return base.plusDays(days.toLong())

            val months = asInt(map["planmonths"] ?: map["months"])
            if (months != null) return base.plusMonths(months.toLong())

            val planText = (map["plan"] ?: "").toString()
            val compute = parsePlanDuration(planText)
            if (compute != null) return compute(base)

            val explicit = parseDate(map["expirydate"])
            if (explicit != null && explicit.isAfter(base)) return explicit

            return base.plusMonths(1)
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
