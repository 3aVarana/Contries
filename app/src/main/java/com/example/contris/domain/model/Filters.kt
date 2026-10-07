package com.example.contris.domain.model

import kotlinx.serialization.Serializable

/** Serializable so it can survive process death inside a SavedStateHandle. */
@Serializable
data class CountryFilters(
    val regions: Set<String> = emptySet(),
    val subregions: Set<String> = emptySet(),
    val continents: Set<String> = emptySet(),
    /** null = any, true = landlocked only, false = coastal only. */
    val landlocked: Boolean? = null,
    /** null = any, true = UN members only, false = non-members only. */
    val unMember: Boolean? = null,
    val sovereignOnly: Boolean = false,
    val memberships: Set<Membership> = emptySet(),
    val favoritesOnly: Boolean = false,
) {
    val activeCount: Int
        get() = (if (regions.isNotEmpty()) 1 else 0) +
            (if (subregions.isNotEmpty()) 1 else 0) +
            (if (continents.isNotEmpty()) 1 else 0) +
            (if (landlocked != null) 1 else 0) +
            (if (unMember != null) 1 else 0) +
            (if (sovereignOnly) 1 else 0) +
            (if (memberships.isNotEmpty()) 1 else 0) +
            (if (favoritesOnly) 1 else 0)

    val isEmpty: Boolean get() = activeCount == 0
}

enum class CountrySort {
    NAME_ASC, NAME_DESC, POPULATION_DESC, POPULATION_ASC, AREA_DESC, AREA_ASC
}
