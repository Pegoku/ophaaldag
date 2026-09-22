/*
 * Ophaaldag - a Material 3 client for the Cure Afvalbeheer waste calendar.
 * Copyright (C) 2026 Pere Gomila
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.pegoku.ophaaldag.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.pegoku.ophaaldag.R
import com.pegoku.ophaaldag.data.WasteTypes

/** Squircle badge with the waste-stream colour and icon. */
@Composable
fun WasteIcon(
    type: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(percent = 35),
    tint: Color = Color.White,
) {
    val style = WasteTypes.style(type)
    Box(
        modifier = modifier.size(size).clip(shape).background(style.color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(style.icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * A waste-stream filter chip outlined in that stream's colour.
 *
 * The outline is the legend: it is the same colour as the stream's pin on the container map and its
 * icon in the lists, so a chip tells you what you are looking at without having to tap anything.
 * Pass a null [color] for an "all types" chip, which belongs to no single stream.
 *
 * [elevation] lifts the chip for use over map tiles; on an ordinary surface leave it at zero.
 */
@Composable
fun WasteFilterChip(
    label: String,
    selected: Boolean,
    color: Color?,
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        modifier = modifier,
        elevation = FilterChipDefaults.filterChipElevation(elevation = elevation),
        // The "all types" chip has no stream colour, so it falls back to the neutral outline;
        // without a border it vanishes into a dark background.
        border = BorderStroke(
            width = if (selected) 2.dp else 1.5.dp,
            color = color ?: MaterialTheme.colorScheme.outline,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            // Blended rather than alpha-tinted: over map tiles a translucent container lets the
            // streets through and washes the label out.
            selectedContainerColor = color
                ?.let { lerp(MaterialTheme.colorScheme.surface, it, 0.30f) }
                ?: MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val headingStyle = MaterialTheme.typography.titleMedium.toSpanStyle()
    val annotated = remember(html, linkColor, headingStyle) {
        val parsed = AnnotatedString.fromHtml(
            cleanHtml(html),
            linkStyles = TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
        ).collapseBlankLines()
        AnnotatedString.Builder(parsed).apply {
            parsed.spanStyles.filter { span ->
                span.item.fontWeight == FontWeight.Bold &&
                    (span.start == 0 || parsed.text[span.start - 1] == '\n') &&
                    (span.end == parsed.length || parsed.text[span.end] == '\n')
            }.forEach { addStyle(headingStyle.copy(fontWeight = FontWeight.SemiBold), it.start, it.end) }
        }.toAnnotatedString()
    }
    Text(annotated, modifier = modifier, style = style.copy(lineHeight = style.fontSize * 1.5f, letterSpacing = 0.sp))
}

/**
 * Normalises the CMS fragments the API returns so `Html.fromHtml` renders them well:
 * headings become bold paragraphs, list items get real bullets (BulletSpan is dropped by
 * the AnnotatedString conversion), tables degrade to lines, images and empty paragraphs go.
 */
internal fun cleanHtml(html: String): String = html
    .replace("\r\n", "\n")
    .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
    .replace(Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
    .replace(Regex("<img[^>]*>", RegexOption.IGNORE_CASE), "")
    .replace(Regex("<h[1-6][^>]*>", RegexOption.IGNORE_CASE), "<p><b>")
    .replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), "</b></p>")
    .replace(Regex("<(div|section|article|blockquote|tr)[^>]*>", RegexOption.IGNORE_CASE), "<p>")
    .replace(Regex("</(div|section|article|blockquote|tr)>", RegexOption.IGNORE_CASE), "</p>")
    .replace(Regex("</?(ul|ol|table|tbody|thead)[^>]*>", RegexOption.IGNORE_CASE), "")
    .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "<p>&#8226;&nbsp;&nbsp;")
    .replace(Regex("</li>", RegexOption.IGNORE_CASE), "</p>")
    .replace(Regex("</t[dh]>\\s*<t[dh][^>]*>", RegexOption.IGNORE_CASE), " - ")
    .replace(Regex("</?t[dh][^>]*>", RegexOption.IGNORE_CASE), "")
    .replace(Regex("<p[^>]*>(\\s|&nbsp;|<br\\s*/?>)*</p>", RegexOption.IGNORE_CASE), "")
    .replace(Regex("(<br\\s*/?>\\s*){2,}", RegexOption.IGNORE_CASE), "<br>")
    .replace(Regex("<br\\s*/?>\\s*</p>", RegexOption.IGNORE_CASE), "</p>")
    .replace(Regex("<p[^>]*>\\s*<br\\s*/?>", RegexOption.IGNORE_CASE), "<p>")
    .trim()

/** Trims edge newlines and gives CMS paragraphs and headings breathing room. */
internal fun AnnotatedString.collapseBlankLines(): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    var start = 0
    // Walk runs of newlines, copying the source (with styles) in slices.
    while (i < length) {
        if (text[i] == '\n') {
            var j = i
            while (j < length && text[j] == '\n') j++
            builder.append(subSequence(start, i))
            val atEdge = builder.length == 0 || j == length
            if (!atEdge) builder.append("\n\n")
            start = j
            i = j
        } else i++
    }
    if (start < length) builder.append(subSequence(start, length))
    return builder.toAnnotatedString()
}

@Composable
fun FullScreenLoading(message: String? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LoadingIndicator()
        if (message != null) {
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier, secondary: (@Composable () -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        secondary?.let { Spacer(Modifier.height(8.dp)); it() }
    }
}

@Composable
fun DetailTopBar(title: String, onBack: () -> Unit, scrollBehavior: TopAppBarScrollBehavior? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
        },
        actions = { actions() },
        scrollBehavior = scrollBehavior,
    )
}

fun ageText(context: Context, fetchedAt: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - fetchedAt) / 60_000L).toInt()
    val res = context.resources
    return when {
        minutes < 1 -> context.getString(R.string.just_now)
        minutes < 60 -> res.getQuantityString(R.plurals.minutes_ago, minutes, minutes)
        minutes < 24 * 60 -> (minutes / 60).let { res.getQuantityString(R.plurals.hours_ago, it, it) }
        else -> (minutes / (24 * 60)).let { res.getQuantityString(R.plurals.days_ago, it, it) }
    }
}

val ScreenPadding = PaddingValues(horizontal = 16.dp)

