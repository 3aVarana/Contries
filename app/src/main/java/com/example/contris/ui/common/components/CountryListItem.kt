package com.example.contris.ui.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.contris.domain.model.CountrySummary
import com.example.contris.ui.common.Formatters

@Composable
fun CountryListItem(
    country: CountrySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    containerColor: Color = ListItemDefaults.containerColor,
) {
    val subtitle = listOfNotNull(country.capital, country.region).joinToString(" · ")
    ListItem(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = containerColor),
        leadingContent = {
            FlagImage(
                pngUrl = country.flagPng,
                emoji = country.flagEmoji,
                contentDescription = null,
            )
        },
        headlineContent = {
            Text(country.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = if (subtitle.isNotBlank()) {
            { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else null,
        trailingContent = trailing ?: {
            Row {
                Text(
                    text = Formatters.formatPopulationCompact(country.population),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        },
    )
}
