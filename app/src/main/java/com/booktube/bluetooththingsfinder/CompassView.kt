package com.booktube.bluetooththingsfinder

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CompassView(
    currentDirection: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Compass",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Simple direction indicator
        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getDirectionArrow(currentDirection),
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = "${currentDirection.toInt()}°",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = getDirectionName(currentDirection),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

private fun getDirectionArrow(degrees: Float): String {
    return when {
        degrees in 315f..45f -> "↑" // North
        degrees in 45f..135f -> "→" // East
        degrees in 135f..225f -> "↓" // South
        degrees in 225f..315f -> "←" // West
        else -> "↑" // Default to North
    }
}

private fun getDirectionName(degrees: Float): String {
    return when {
        degrees in 315f..45f -> "North"
        degrees in 45f..135f -> "East"
        degrees in 135f..225f -> "South"
        degrees in 225f..315f -> "West"
        else -> "North"
    }
}
