package com.example.contris.ui

import com.example.contris.domain.model.UnitSystem
import com.example.contris.ui.common.Formatters
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Test

class FormattersTest {
    private val us = Locale.US

    @Test
    fun `compact population uses K M B suffixes`() {
        assertThat(Formatters.formatPopulationCompact(41_798_407, us)).isEqualTo("41.8M")
        assertThat(Formatters.formatPopulationCompact(1_412_000_000, us)).isEqualTo("1.4B")
        assertThat(Formatters.formatPopulationCompact(9_500, us)).isEqualTo("9.5K")
        assertThat(Formatters.formatPopulationCompact(812, us)).isEqualTo("812")
        assertThat(Formatters.formatPopulationCompact(345_426_571, us)).isEqualTo("345M")
        assertThat(Formatters.formatPopulationCompact(null, us)).isEqualTo(Formatters.DASH)
    }

    @Test
    fun `full population is grouped`() {
        assertThat(Formatters.formatPopulationFull(41_798_407, us)).isEqualTo("41,798,407")
    }

    @Test
    fun `area respects unit system`() {
        assertThat(Formatters.formatArea(357_114.0, UnitSystem.METRIC, us)).isEqualTo("357,114 km²")
        assertThat(Formatters.formatArea(357_114.0, UnitSystem.IMPERIAL, us)).isEqualTo("137,882 mi²")
        assertThat(Formatters.formatArea(2.02, UnitSystem.METRIC, us)).isEqualTo("2.02 km²")
        assertThat(Formatters.formatArea(null, UnitSystem.METRIC, us)).isEqualTo(Formatters.DASH)
    }

    @Test
    fun `density converts for imperial`() {
        assertThat(Formatters.formatDensity(236.8, UnitSystem.METRIC, us)).isEqualTo("237/km²")
        assertThat(Formatters.formatDensity(236.8, UnitSystem.IMPERIAL, us)).isEqualTo("613/mi²")
    }

    @Test
    fun `coordinates use hemispheres`() {
        assertThat(Formatters.formatCoordinates(60.0, -95.0, us)).isEqualTo("60° N, 95° W")
        assertThat(Formatters.formatCoordinates(-33.87, 151.21, us)).isEqualTo("33.87° S, 151.21° E")
    }

    @Test
    fun `relative time buckets`() {
        val now = Instant.parse("2026-10-07T12:00:00Z")
        assertThat(Formatters.formatRelativeTime(null, now)).isEqualTo("never")
        assertThat(Formatters.formatRelativeTime(now.minusSeconds(20), now)).isEqualTo("just now")
        assertThat(Formatters.formatRelativeTime(now.minusSeconds(60 * 5), now)).isEqualTo("5 minutes ago")
        assertThat(Formatters.formatRelativeTime(now.minusSeconds(3600), now)).isEqualTo("1 hour ago")
        assertThat(Formatters.formatRelativeTime(now.minusSeconds(3600 * 24 * 3), now)).isEqualTo("3 days ago")
        assertThat(Formatters.formatRelativeTime(now.minusSeconds(3600 * 24 * 30), now, ZoneOffset.UTC, us)).isEqualTo("Sep 7, 2026")
    }
}
