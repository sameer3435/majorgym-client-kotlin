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
         *
         * The owner app (see its QrUtils.onboardingPayload) puts out:
         * id, name, phone, plan, fee, joinedMillis, expiryMillis,
         * passwordHash, token, tokenExpiryMillis, gymName, historyJson.
         * Dates there are epoch-millisecond longs, NOT date strings — so
         * this parser reads joinedMillis/expiryMillis first. The older
         * string-keyed shape (joiningDate, renewalDate, durationDays,
         * planMonths, expiryDate) is still accepted as a fallback in case
         * some other source ever sends dates that way.
         *
         * [existing] is the member profile already cached on this device,
         * if any:
         *  - When null, this scan is the member's first-ever join.
         *  - When non-null, this scan is a RENEWAL of that same member.
         *
         * Field semantics, matching the owner app's Room entity:
         *  - joinedMillis never changes once a member is created (renewals
         *    only touch expiry/plan/fee/history), so it's trusted straight
         *    from the QR as the one true joining date.
         *  - expiryMillis is the owner app's own computed current expiry
         *    (base date + plan duration) — the source of truth, not
         *    something this app needs to (or should) recompute.
         *  - renewedDate isn't a field the owner app sends directly; it's
         *    taken from the most recent entry in historyJson (type
         *    "Joined"/"Renewed" with a "date" millis field), falling back
         *    to the joining date when there's no history yet.
         */
        fun fromQrJson(json: JSONObject, existing: Member? = null): Member {
            val map = HashMap<String, Any?>()
            json.keys().forEach { k -> map[k.lowercase()] = json.get(k) }

            val plan = (map["plan"] ?: map["planname"] ?: "").toString()

            val joining = parseDateOrMillis(map["joinedmillis"])
                ?: parseDate(map["joiningdate"])
                ?: existing?.joiningDate
                ?: LocalDate.now()

            val renewed = latestHistoryDate(map["historyjson"])
                ?: parseDateOrMillis(map["renewalmillis"])
                ?: parseDate(map["renewaldate"])
                ?: parseDate(map["reneweddate"])
                ?: joining

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

        /** Reads the "date" millis of the last (most recent) entry in the owner
         *  app's historyJson array — that's the actual last join/renewal date.
         *  Returns null if there's no historyJson, it's empty, or malformed. */
        private fun latestHistoryDate(value: Any?): LocalDate? {
            if (value == null) return null
            return try {
                val arr = org.json.JSONArray(value.toString())
                if (arr.length() == 0) return null
                var latestMillis = -1L
                for (i in 0 until arr.length()) {
                    val entry = arr.optJSONObject(i) ?: continue
                    val d = entry.optLong("date", -1L)
                    if (d > latestMillis) latestMillis = d
                }
                if (latestMillis < 0) null else millisToLocalDate(latestMillis)
            } catch (e: Exception) {
                null
            }
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

        /** Epoch millis (Long, Int, or numeric string) -> LocalDate in the device's
         *  default zone. Used for the owner app's joinedMillis/expiryMillis fields. */
        private fun millisToLocalDate(millis: Long): LocalDate =
            java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        /** Accepts either an epoch-millis number (the owner app's format) or an
         *  ISO date string, and resolves either to a LocalDate. A value is only
         *  treated as millis when it's a large enough number to plausibly be one
         *  (guards against a stray small integer being misread as a date). */
        private fun parseDateOrMillis(value: Any?): LocalDate? {
            if (value == null) return null
            val asLong = when (value) {
                is Long -> value
                is Int -> value.toLong()
                is Number -> value.toLong()
                else -> value.toString().toLongOrNull()
            }
            if (asLong != null && asLong > 1_000_000_000_000L) return millisToLocalDate(asLong)
            return parseDate(value)
        }

        /**
         * Resolves the member's expiry date. The owner app's expiryMillis is its
         * own already-computed, authoritative current expiry (base date + plan
         * duration, done on the owner's side using the real plan table) — so
         * when the QR carries it, that value is used directly rather than
         * re-derived here. Duration-based computation (explicit day/month
         * counts, or parsing the plan text, e.g. "1 Month") is kept only as a
         * fallback for payloads that don't include expiryMillis/expiryDate.
         */
        private fun resolveExpiry(map: Map<String, Any?>, base: LocalDate): LocalDate {
            val explicit = parseDateOrMillis(map["expirymillis"]) ?: parseDate(map["expirydate"])
            if (explicit != null) return explicit

            val days = asInt(map["durationdays"] ?: map["plandays"])
            if (days != null) return base.plusDays(days.toLong())

            val months = asInt(map["planmonths"] ?: map["months"])
            if (months != null) return base.plusMonths(months.toLong())

            val planText = (map["plan"] ?: "").toString()
            val compute = parsePlanDuration(planText)
            if (compute != null) return compute(base)

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
