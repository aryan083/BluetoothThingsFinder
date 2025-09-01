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
                Text("How to Use Bluetooth Things Finder")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                HelpSection(
                    title = "Getting Started",
                    content = "1. Tap the 'Scan' button to start searching for nearby Bluetooth devices\n" +
                            "2. The app continuously scans for both BLE and Classic Bluetooth devices\n" +
                            "3. Devices are automatically sorted by signal strength (strongest first)\n" +
                            "4. The app will keep scanning until you close it"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Understanding Signal Strength",
                    content = "• 🎯 Very Close (≤0.3m): You're almost on top of it!\n" +
                            "• 📍 Extremely Close (≤0.5m): It's right here!\n" +
                            "• 🔍 Close (≤1m): Look around carefully\n" +
                            "• 👀 Medium Close (≤2m): Scan nearby area\n" +
                            "• 🔭 Medium (≤4m): Scan the area\n" +
                            "• 🔎 Medium Far (≤8m): Search wider area\n" +
                            "• 🏃 Far (≤15m): Move around to get closer\n" +
                            "• 🚶 Very Far (>15m): Start walking in any direction"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Movement Tracking",
                    content = "✅ Getting Closer: Signal is improving - keep going!\n" +
                            "❌ Moving Away: Signal is weakening - try different direction\n" +
                            "⏸️ Signal Stable: No significant change - look around\n" +
                            "❓ Movement Unclear: Not enough data - keep moving"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Finding Devices",
                    content = "• Move around slowly to get better signal readings\n" +
                            "• Watch for signal strength changes as you move\n" +
                            "• Stronger signals mean you're getting closer\n" +
                            "• Weaker signals mean you're moving away\n" +
                            "• Use the movement indicators to guide your search"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Distance Estimation",
                    content = "The app uses advanced algorithms to estimate distance:\n" +
                            "• Very Close: ±0.2m accuracy\n" +
                            "• Close: ±0.5m accuracy\n" +
                            "• Medium: ±1.0m accuracy\n" +
                            "• Far: ±2.0m accuracy\n" +
                            "• Very Far: ±5.0m accuracy\n" +
                            "Confidence levels are shown to indicate reliability."
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Favorites System",
                    content = "• Tap the star icon to add devices to favorites\n" +
                            "• View your favorite devices in the 'Favorites' tab\n" +
                            "• Favorites are saved between app sessions\n" +
                            "• Only device identifiers are stored (no signal data)"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Device Types",
                    content = "• BLE: Bluetooth Low Energy devices (most common)\n" +
                            "• BT: Classic Bluetooth devices\n" +
                            "• Both types are supported and will be detected automatically"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Pro Tips",
                    content = "• Move around slowly to get better signal readings\n" +
                            "• Keep the app open while searching for best results\n" +
                            "• Signal strength can vary due to obstacles and interference\n" +
                            "• Some devices may not show names but will still be detected\n" +
                            "• The app works best in open spaces with minimal interference\n" +
                            "• Pay attention to movement indicators for better guidance"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HelpSection(
                    title = "Troubleshooting",
                    content = "• If no devices appear, ensure Bluetooth is enabled\n" +
                            "• Grant location permission for better device discovery\n" +
                            "• Some devices may be in sleep mode - try moving closer\n" +
                            "• Restart the app if scanning stops working\n" +
                            "• Make sure you're in an area with Bluetooth devices"
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
