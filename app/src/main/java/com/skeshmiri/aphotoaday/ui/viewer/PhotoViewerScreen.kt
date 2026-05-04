package com.skeshmiri.aphotoaday.ui.viewer

import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.skeshmiri.aphotoaday.ui.common.ScreenHeader
import com.skeshmiri.aphotoaday.ui.common.UriImage
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

const val PhotoViewerImageTag = "photo_viewer_image"

@Composable
fun PhotoViewerScreen(
    uri: Uri,
    title: String,
    contentDescription: String,
    capturedAt: Instant?,
    onClose: () -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val headerTitle = capturedAt
        ?.let { "${formatPhotoDateTitle(title)}, ${DateFormat.getTimeFormat(context).format(Date.from(it))}" }
        ?: formatPhotoDateTitle(title)
    Scaffold(
        topBar = {
            ScreenHeader(title = headerTitle)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShare,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share photo",
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(PhotoViewerImageTag)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClose,
                ),
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
