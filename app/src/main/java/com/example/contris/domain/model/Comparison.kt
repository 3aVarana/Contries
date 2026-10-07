package com.example.contris.domain.model

enum class Side { LEFT, RIGHT }

data class ComparisonRow(
    val label: String,
    val left: String?,
    val right: String?,
    /** Only set for numeric rows where a "larger" value is meaningful. */
    val winner: Side? = null,
)

data class Comparison(val rows: List<ComparisonRow>)
