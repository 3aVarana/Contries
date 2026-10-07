package com.example.contris.ui.common.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Flag thumbnail: PNG (or SVG when [preferSvg]) via Coil with an emoji placeholder / fallback.
 * Rounded 4 dp with a subtle outline so mostly-white flags remain visible.
 */
@Composable
fun FlagImage(
    pngUrl: String?,
    emoji: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    svgUrl: String? = null,
    preferSvg: Boolean = false,
    width: Dp = 56.dp,
    height: Dp = 40.dp,
    cornerRadius: Dp = 4.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val url = if (preferSvg) svgUrl ?: pngUrl else pngUrl ?: svgUrl
    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (url == null) {
            EmojiFallback(emoji, height)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                loading = { EmojiFallback(emoji, height) },
                error = { EmojiFallback(emoji, height) },
            )
        }
    }
}

@Composable
private fun EmojiFallback(emoji: String?, height: Dp) {
    Text(
        text = emoji ?: "🏳",
        fontSize = (height.value * 0.6f).sp,
        style = MaterialTheme.typography.titleLarge,
    )
}
