package com.pegoku.curem3.ui.components

import android.content.Context
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
import com.pegoku.curem3.R
import com.pegoku.curem3.data.WasteTypes

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
    val annotated = remember(html, linkColor) {
        AnnotatedString.fromHtml(
            cleanHtml(html),
            linkStyles = TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
        ).collapseBlankLines()
    }
    Text(annotated, modifier = modifier, style = style.copy(lineHeight = style.fontSize * 1.5f))
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

/** Trims leading/trailing newlines and collapses 3+ consecutive newlines to a paragraph break. */
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
            if (!atEdge) builder.append(if (j - i >= 2) "\n\n" else "\n")
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
        title = { Text(title, maxLines = 1) },
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

