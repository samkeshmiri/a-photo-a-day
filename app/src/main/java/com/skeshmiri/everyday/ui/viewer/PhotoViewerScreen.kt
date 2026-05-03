package com.skeshmiri.everyday.ui.viewer

import android.text.format.DateFormat
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.skeshmiri.everyday.ui.common.ScreenHeader
import com.skeshmiri.everyday.ui.common.UriImage
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Date

@Composable
fun PhotoViewerScreen(
    uri: Uri,
    title: String,
    contentDescription: String,
    capturedAt: Instant?,
) {
    val context = LocalContext.current
    val headerTitle = capturedAt
        ?.let { "${formatPhotoDateTitle(title)}, ${DateFormat.getTimeFormat(context).format(Date.from(it))}" }
        ?: formatPhotoDateTitle(title)
    Scaffold(
        topBar = {
            ScreenHeader(title = headerTitle)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            UriImage(
                uri = uri,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private fun formatPhotoDateTitle(dateKey: String): String {
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return dateKey
    val month = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH).format(date)
    return "$month ${date.dayOfMonth}${date.dayOfMonth.ordinalSuffix()} ${date.year}"
}

private fun Int.ordinalSuffix(): String =
    if (this % 100 in 11..13) {
        "th"
    } else {
        when (this % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
