package com.geo.tracking.ui.components

import android.location.Location
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geo.tracking.R

@Composable
fun CustomInfoWindow(position: Location) {
    val ledFont = FontFamily(Font(R.font.led))
    Surface(
        modifier = Modifier
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Location Information",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = ledFont),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${position.latitude}, ${position.longitude}",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = ledFont)
            )
        }
    }
}
