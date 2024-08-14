package com.geo.tracking.ui.components

import android.location.Location
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geo.tracking.R
import java.text.SimpleDateFormat
import java.util.Locale

val ledFont = FontFamily(Font(R.font.led))

@Composable
fun MapInfoWindowContent(location: Location) {

    val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
    val date = sdf.format(location.time)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ) {
            Text(
                text = "Location Information",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = ledFont),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(2.dp))
            InfoRow(
                icon = Icons.Default.LocationOn,
                label = "Latitude",
                value = "${location.latitude}"
            )
            InfoRow(
                icon = Icons.Default.LocationOn,
                label = "Longitude",
                value = "${location.longitude}"
            )
            InfoRow(
                icon = Icons.Default.Height,
                label = "Altitude",
                value = "${location.altitude} m"
            )
            InfoRow(
                icon = Icons.Default.MyLocation,
                label = "Accuracy",
                value = "${location.accuracy} m"
            )
            InfoRow(icon = Icons.Default.Explore, label = "Bearing", value = "${location.bearing}°")
            InfoRow(icon = Icons.Default.Speed, label = "Speed", value = "${location.speed} m/s")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                InfoRow(
                    icon = Icons.Default.VerticalAlignCenter,
                    label = "V-Accuracy",
                    value = "${location.verticalAccuracyMeters} m"
                )
            }
            InfoRow(
                icon = Icons.Default.Storage,
                label = "Provider",
                value = "${location.provider}"
            )
            InfoRow(icon = Icons.Default.Schedule, label = "Time", value = date)
            InfoRow(
                icon = Icons.Default.Timer,
                label = "Elapsed Time",
                value = "${location.elapsedRealtimeNanos / 1_000_000_000} s"
            )
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 1.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "$label ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ledFont),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.width(140.dp)
        )
    }
}
