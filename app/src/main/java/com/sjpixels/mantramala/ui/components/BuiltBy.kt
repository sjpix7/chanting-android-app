package com.sjpixels.mantramala.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sjpixels.mantramala.R

const val SJPIXELS_URL = "http://sjpixels.com/"
private const val SJPIXELS_LABEL = "sjpixels.com"

/**
 * Quiet maker's mark: "Built by sjpixels.com". The whole line is the tap target (easier to hit than
 * the small text alone) and opens the site in the phone's browser — the app itself never goes online.
 */
@Composable
fun BuiltByLink(modifier: Modifier = Modifier, emphasis: Float = 0.7f) {
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val full = stringResource(R.string.built_by, SJPIXELS_LABEL)
    val linkColor = scheme.secondary
    val text = remember(full, linkColor) {
        buildAnnotatedString {
            append(full)
            val start = full.indexOf(SJPIXELS_LABEL)
            if (start >= 0) {
                addStyle(
                    SpanStyle(color = linkColor, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
                    start, start + SJPIXELS_LABEL.length,
                )
            }
        }
    }
    Box(
        modifier.clickable(onClickLabel = stringResource(R.string.open_link, SJPIXELS_LABEL)) {
            // No browser installed (rare) — just do nothing rather than crash.
            runCatching { uriHandler.openUri(SJPIXELS_URL) }
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.6.sp),
            color = scheme.onSurfaceVariant.copy(alpha = emphasis),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
    }
}
