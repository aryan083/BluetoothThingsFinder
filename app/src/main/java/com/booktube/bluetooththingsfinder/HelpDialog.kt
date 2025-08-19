package com.booktube.bluetooththingsfinder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("How to Use")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                HelpSection(
                    title = "Finding Devices",
                    content = "1. Tap the 'Scan' button to start searching for nearby Bluetooth devices\n" +
                            "2. The app will scan for 10 seconds and show all found devices\n" +
                            "3. Devices are sorted by signal strength (strongest first)"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Understanding Signal Strength",
                    content = "• Green: Very close (within 0.5m)\n" +
                            "• Yellow: Close (within 2m)\n" +
                            "• Orange: Medium distance (within 8m)\n" +
                            "• Red: Far (beyond 8m)"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Direction Indicators",
                    content = "📍 Very Close: You're almost on top of it!\n" +
                            "🔍 Close: Look around carefully\n" +
                            "👀 Medium: Scan the area\n" +
                            "🔭 Far: Move around to get closer"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Using the Compass",
                    content = "The compass shows your current orientation.\n" +
                            "North is at the top (0°), East is right (90°), etc.\n" +
                            "Use this to understand which direction you're facing."
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Favorites",
                    content = "• Tap the star icon to add devices to favorites\n" +
                            "• View your favorite devices in the 'Favorites' tab\n" +
                            "• Favorites are saved between app sessions"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Tips",
                    content = "• Move around slowly to get better signal readings\n" +
                            "• Keep the app open while searching\n" +
                            "• Signal strength can vary due to obstacles\n" +
                            "• Some devices may not show names"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun HelpSection(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
