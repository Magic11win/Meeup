package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ConversationEntity
import com.example.ui.components.ContactAvatar
import com.example.ui.components.formatDurationAgo
import com.example.ui.components.parseHexColor
import com.example.ui.theme.MeetupEmerald
import com.example.ui.theme.MeetupTealDark
import com.example.ui.theme.MeetupTealMedium
import com.example.util.DeviceGpsCoordinate
import com.example.viewmodel.LiveTrackedContactMarker

@Composable
fun LiveLocationRadarScreen(
    conversations: List<ConversationEntity>,
    liveMarkers: List<LiveTrackedContactMarker>,
    selectedContactId: String?,
    myDeviceGps: DeviceGpsCoordinate,
    onSelectContact: (String) -> Unit,
    onToggleMyGrantToContact: (conversationId: String, grant: Boolean) -> Unit,
    onToggleTheirGrantToMe: (conversationId: String, grant: Boolean) -> Unit,
    onOpenChat: (String) -> Unit,
    onLocationPermissionGranted: () -> Unit
) {
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var showAccessControlPanel by remember { mutableStateOf(false) }

    val selectedMarker = liveMarkers.find { it.conversationId == selectedContactId }
        ?: liveMarkers.firstOrNull()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            onLocationPermissionGranted()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_pulse")
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val pulseRadiusFactor by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("live_radar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Explanation Banner for Persistent Multi-Year Location Access
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MeetupTealDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Persistent Radar",
                                tint = MeetupEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Persistent Live Location Radar",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = MeetupEmerald,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${liveMarkers.size} Permitted",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Only contacts who explicitly granted you permission appear on this live map. Once granted, permissions remain valid long-term (even 1+ year later) so you can track their live movement anytime until they revoke access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MeetupEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_device_gps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync My GPS", color = Color.Black, style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = { showAccessControlPanel = !showAccessControlPanel },
                            colors = ButtonDefaults.buttonColors(containerColor = MeetupTealMedium),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manage_permissions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAccessControlPanel) "Hide Permissions" else "Manage Permissions",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // Contact Selector Strip for Permitted Contacts
        item {
            if (liveMarkers.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Contacts Have Granted Location Permission Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use 'Manage Permissions' above to test granting or revoking persistent location access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(liveMarkers, key = { it.conversationId }) { marker ->
                        val isSelected = selectedMarker?.conversationId == marker.conversationId
                        Surface(
                            color = if (isSelected) MeetupTealDark else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 3.dp,
                            modifier = Modifier
                                .clickable {
                                    onSelectContact(marker.conversationId)
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MeetupEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("radar_marker_chip_${marker.conversationId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(
                                    name = marker.name,
                                    colorHex = marker.avatarColorHex,
                                    isGroup = marker.isGroup,
                                    hasLiveRadarActive = true,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = marker.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Granted ${formatDurationAgo(marker.grantedTimestamp)} • ${"%.1f".format(marker.speedKmh)} km/h",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MeetupEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Live Radar Map Canvas with Real-Time Movement Trajectories
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF09191F))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    panOffsetX = (panOffsetX + dragAmount.x).coerceIn(-300f, 300f)
                                    panOffsetY = (panOffsetY + dragAmount.y).coerceIn(-300f, 300f)
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val centerX = (w / 2f) + panOffsetX
                        val centerY = (h / 2f) + panOffsetY

                        // Tactical Cartographic Grid & Road Network
                        val gridStep = 44.dp.toPx() * zoomLevel
                        var gx = centerX % gridStep
                        while (gx < w) {
                            drawLine(
                                color = Color(0xFF14333D),
                                start = Offset(gx, 0f),
                                end = Offset(gx, h),
                                strokeWidth = 1f
                            )
                            gx += gridStep
                        }
                        var gy = centerY % gridStep
                        while (gy < h) {
                            drawLine(
                                color = Color(0xFF14333D),
                                start = Offset(0f, gy),
                                end = Offset(w, gy),
                                strokeWidth = 1f
                            )
                            gy += gridStep
                        }

                        // Stylized Highway Arteries
                        val highwayPath = Path().apply {
                            moveTo(0f, centerY - 75f * zoomLevel)
                            cubicTo(
                                w * 0.35f,
                                centerY - 15f * zoomLevel,
                                w * 0.65f,
                                centerY + 65f * zoomLevel,
                                w,
                                centerY + 25f * zoomLevel
                            )
                        }
                        drawPath(
                            path = highwayPath,
                            color = Color(0xFF1C4754),
                            style = Stroke(width = 6f * zoomLevel)
                        )

                        // Radar Concentric Range Rings
                        listOf(60f, 125f, 195f).forEach { r ->
                            drawCircle(
                                color = Color(0xFF00A884).copy(alpha = 0.24f),
                                radius = r * zoomLevel,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 1.5f)
                            )
                        }

                        // Rotating Radar Sweep Beam
                        val sweepRad = Math.toRadians(radarSweepAngle.toDouble())
                        val sweepEndX = centerX + (210f * zoomLevel * kotlin.math.cos(sweepRad)).toFloat()
                        val sweepEndY = centerY + (210f * zoomLevel * kotlin.math.sin(sweepRad)).toFloat()
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(MeetupEmerald.copy(alpha = 0.55f), Color.Transparent),
                                start = Offset(centerX, centerY),
                                end = Offset(sweepEndX, sweepEndY)
                            ),
                            start = Offset(centerX, centerY),
                            end = Offset(sweepEndX, sweepEndY),
                            strokeWidth = 3f
                        )

                        // Draw My Device GPS Beacon at Center
                        drawCircle(
                            color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                            radius = 22f * pulseRadiusFactor * zoomLevel,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = Color(0xFF38BDF8),
                            radius = 7f,
                            center = Offset(centerX, centerY)
                        )

                        // Plot Permitted Contact Markers & Live Breadcrumb Trails
                        liveMarkers.forEachIndexed { idx, marker ->
                            val isSelected = selectedMarker?.conversationId == marker.conversationId
                            val markerColor = parseHexColor(marker.avatarColorHex, MeetupEmerald)

                            // Compute relative screen offset around center so all permitted contacts are visible & moving live
                            val baseAngleOffset = idx * 1.45
                            val relX = ((marker.longitude - marker.trailPoints.first().second) * 18000f * zoomLevel).toFloat() +
                                (kotlin.math.cos(baseAngleOffset) * 85f * zoomLevel).toFloat()
                            val relY = ((marker.latitude - marker.trailPoints.first().first) * 18000f * zoomLevel).toFloat() +
                                (kotlin.math.sin(baseAngleOffset) * 65f * zoomLevel).toFloat()

                            val pinX = centerX + relX
                            val pinY = centerY + relY

                            // Draw live movement trajectory trail
                            val trailPath = Path()
                            marker.trailPoints.forEachIndexed { tIdx, pt ->
                                val tRelX = ((pt.second - marker.trailPoints.first().second) * 18000f * zoomLevel).toFloat() +
                                    (kotlin.math.cos(baseAngleOffset) * 85f * zoomLevel).toFloat()
                                val tRelY = ((pt.first - marker.trailPoints.first().first) * 18000f * zoomLevel).toFloat() +
                                    (kotlin.math.sin(baseAngleOffset) * 65f * zoomLevel).toFloat()
                                if (tIdx == 0) {
                                    trailPath.moveTo(centerX + tRelX, centerY + tRelY)
                                } else {
                                    trailPath.lineTo(centerX + tRelX, centerY + tRelY)
                                }
                            }
                            drawPath(
                                path = trailPath,
                                color = if (isSelected) MeetupEmerald else markerColor.copy(alpha = 0.65f),
                                style = Stroke(
                                    width = if (isSelected) 4f else 2.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                )
                            )

                            // Pulse Halo for Selected Contact
                            if (isSelected) {
                                drawCircle(
                                    color = MeetupEmerald.copy(alpha = (1f - pulseRadiusFactor) * 0.6f),
                                    radius = 38f * pulseRadiusFactor,
                                    center = Offset(pinX, pinY)
                                )
                            }

                            // Outer Pin Ring
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 13f else 10f,
                                center = Offset(pinX, pinY)
                            )
                            // Inner Pin Fill
                            drawCircle(
                                color = if (isSelected) MeetupEmerald else markerColor,
                                radius = if (isSelected) 10f else 7.5f,
                                center = Offset(pinX, pinY)
                            )
                        }
                    }

                    // Map Top Overlay: My Live GPS Telemetry Pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.68f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You: ${"%.4f".format(myDeviceGps.latitude)}, ${"%.4f".format(myDeviceGps.longitude)} • E2EE Radar",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Zoom & Re-center Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = CircleShape
                        ) {
                            IconButton(
                                onClick = { zoomLevel = (zoomLevel + 0.25f).coerceAtMost(2.5f) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                            }
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = CircleShape
                        ) {
                            IconButton(
                                onClick = { zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.6f) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                            }
                        }
                        Surface(
                            color = MeetupEmerald,
                            shape = CircleShape
                        ) {
                            IconButton(
                                onClick = {
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                    zoomLevel = 1.0f
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = "Reset Map Center", tint = Color.Black)
                            }
                        }
                    }
                }
            }
        }

        // Selected Contact Live Movement Telemetry Card
        selectedMarker?.let { marker ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                ContactAvatar(
                                    name = marker.name,
                                    colorHex = marker.avatarColorHex,
                                    isGroup = marker.isGroup,
                                    hasLiveRadarActive = true,
                                    size = 48.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = marker.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = marker.cityLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = { onOpenChat(marker.conversationId) },
                                colors = ButtonDefaults.buttonColors(containerColor = MeetupTealDark),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Telemetry Metrics Row (Live Coordinates, Speed, Battery, Persistent Grant Age)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TelemetryStatPill(
                                icon = Icons.Default.Navigation,
                                label = "Live GPS",
                                value = "${"%.4f".format(marker.latitude)}, ${"%.4f".format(marker.longitude)}"
                            )
                            TelemetryStatPill(
                                icon = Icons.Default.Speed,
                                label = "Live Speed",
                                value = "${"%.1f".format(marker.speedKmh)} km/h"
                            )
                            TelemetryStatPill(
                                icon = Icons.Default.BatteryChargingFull,
                                label = "Battery",
                                value = "${marker.batteryPercent}%"
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MeetupTealDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Permission granted ${formatDurationAgo(marker.grantedTimestamp)} • ${marker.expiryLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Granular Persistent Location Permission Manager Panel
        item {
            AnimatedVisibility(visible = showAccessControlPanel || liveMarkers.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Granular Persistent Location Access Book",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Control exactly who can see your live location (even 1+ year later), and toggle which contacts have granted you access to track them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        conversations.forEach { conv ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ContactAvatar(
                                            name = conv.title,
                                            colorHex = conv.avatarColorHex,
                                            isGroup = conv.isGroup,
                                            size = 36.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = conv.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = conv.currentCityLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Toggle 1: I granted my persistent location to them
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Allow ${conv.title} to track MY live location (1+ Yr)",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = conv.iGrantedLocationToThem,
                                        onCheckedChange = { onToggleMyGrantToContact(conv.id, it) }
                                    )
                                }

                                // Toggle 2: They granted their persistent location to me
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${conv.title} granted ME permission to view their Radar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = conv.theyGrantedLocationToMe,
                                        onCheckedChange = { onToggleTheirGrantToMe(conv.id, it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MeetupTealMedium,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
