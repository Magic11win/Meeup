package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeetupEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun parseHexColor(hex: String, fallback: Color = Color(0xFF00A884)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

fun formatChatTimestamp(timestampMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMillis
    return when {
        diff < 60_000L -> "Just now"
        diff < 86_400_000L -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestampMillis))
        diff < 172_800_000L -> "Yesterday"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestampMillis))
    }
}

fun formatDurationAgo(timestampMillis: Long): String {
    val diffDays = ((System.currentTimeMillis() - timestampMillis) / (1000L * 3600L * 24L)).coerceAtLeast(0L)
    return when {
        diffDays >= 365L -> {
            val yrs = diffDays / 365L
            val mos = (diffDays % 365L) / 30L
            if (mos > 0) "${yrs}y ${mos}m ago" else "${yrs} year(s) ago"
        }
        diffDays >= 30L -> "${diffDays / 30L} months ago"
        diffDays >= 1L -> "$diffDays days ago"
        else -> "Today"
    }
}

@Composable
fun ContactAvatar(
    name: String,
    colorHex: String,
    isGroup: Boolean = false,
    isOnline: Boolean = false,
    hasLiveRadarActive: Boolean = false,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val bgColor = parseHexColor(colorHex)
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "M" }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor)
                .then(
                    if (hasLiveRadarActive) {
                        Modifier.border(2.dp, MeetupEmerald, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isGroup) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Group Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.54f)
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            }
        }

        if (hasLiveRadarActive) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MeetupEmerald)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Live Radar Active",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        } else if (isOnline) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MeetupEmerald)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun EncryptedSecurityPill(
    text: String = "AES-256-GCM End-to-End Encrypted",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
