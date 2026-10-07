package com.example.contris.ui.common

import com.example.contris.domain.model.UnitSystem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

/** Pure formatting helpers; locale is a parameter so tests are deterministic. */
object Formatters {

    private const val KM2_TO_MI2 = 0.386102159

    /** Compact population, e.g. 41_798_407 → "41.8M", 9_500 → "9.5K", 812 → "812". */
    fun formatPopulationCompact(value: Long?, locale: Locale = Locale.getDefault()): String {
        if (value == null) return DASH
        val abs = abs(value.toDouble())
        val symbols = DecimalFormatSymbols.getInstance(locale)
        fun one(v: Double, suffix: String): String {
            val df = DecimalFormat(if (v >= 100) "#" else "#.#", symbols)
            return df.format(v) + suffix
        }
        return when {
            abs >= 1_000_000_000 -> one(value / 1_000_000_000.0, "B")
            abs >= 1_000_000 -> one(value / 1_000_000.0, "M")
            abs >= 1_000 -> one(value / 1_000.0, "K")
            else -> NumberFormat.getIntegerInstance(locale).format(value)
        }
    }

    /** Full population with grouping, e.g. "41,798,407". */
    fun formatPopulationFull(value: Long?, locale: Locale = Locale.getDefault()): String =
        value?.let { NumberFormat.getIntegerInstance(locale).format(it) } ?: DASH

    fun formatArea(km2: Double?, unitSystem: UnitSystem, locale: Locale = Locale.getDefault()): String {
        if (km2 == null) return DASH
        val (value, unit) = when (unitSystem) {
            UnitSystem.METRIC -> km2 to "km²"
            UnitSystem.IMPERIAL -> km2 * KM2_TO_MI2 to "mi²"
        }
        val nf = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = if (value < 10) 2 else if (value < 100) 1 else 0
        }
        return "${nf.format(value)} $unit"
    }

    fun formatDensity(perKm2: Double?, unitSystem: UnitSystem, locale: Locale = Locale.getDefault()): String {
        if (perKm2 == null) return DASH
        val (value, unit) = when (unitSystem) {
            UnitSystem.METRIC -> perKm2 to "/km²"
            UnitSystem.IMPERIAL -> perKm2 / KM2_TO_MI2 to "/mi²"
        }
        val nf = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = if (value < 10) 1 else 0 }
        return "${nf.format(value)}$unit"
    }

    fun formatDecimal(value: Double?, digits: Int = 1, locale: Locale = Locale.getDefault()): String {
        if (value == null) return DASH
        return NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = digits
            minimumFractionDigits = 0
        }.format(value)
    }

    fun formatCoordinates(lat: Double?, lng: Double?, locale: Locale = Locale.getDefault()): String {
        if (lat == null || lng == null) return DASH
        val nf = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }
        val ns = if (lat >= 0) "N" else "S"
        val ew = if (lng >= 0) "E" else "W"
        return "${nf.format(abs(lat))}° $ns, ${nf.format(abs(lng))}° $ew"
    }

    /** "just now", "5 minutes ago", "2 hours ago", "3 days ago" or a short date beyond 7 days. */
    fun formatRelativeTime(instant: Instant?, now: Instant, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
        if (instant == null) return "never"
        val d = Duration.between(instant, now)
        val minutes = d.toMinutes()
        val hours = d.toHours()
        val days = d.toDays()
        return when {
            d.isNegative || minutes < 1 -> "just now"
            minutes < 60 -> plural(minutes, "minute") + " ago"
            hours < 24 -> plural(hours, "hour") + " ago"
            days <= 7 -> plural(days, "day") + " ago"
            else -> formatDate(instant, zone, locale)
        }
    }

    fun formatDate(instant: Instant?, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
        if (instant == null) return DASH
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(instant.atZone(zone))
    }

    private fun plural(n: Long, unit: String) = "$n $unit" + if (n == 1L) "" else "s"

    const val DASH = "—"
}
