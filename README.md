# Bluetooth Things Finder

A Kotlin Android application that helps you find nearby Bluetooth devices with real-time distance estimation and direction guidance.

## Features

### Core Functionality
- **Device Scanning**: Continuously scans for both Bluetooth Classic and BLE devices
- **Distance Estimation**: Uses RSSI (signal strength) to estimate device proximity
- **Direction Guidance**: Provides real-time guidance to help locate devices
- **Favorites System**: Save and manage your favorite devices
- **Compass Integration**: Shows your current orientation for better navigation

### User Interface
- **Clean Material Design**: Modern UI with dynamic theming support
- **Real-time Updates**: Live signal strength and distance information
- **Movement Tracking**: Visual indicators showing if you're getting closer or farther
- **Device Details**: Tap any device for detailed guidance and information

## How to Use

1. **Start Scanning**: Tap the "Scan" button to begin searching for nearby devices
2. **View Devices**: All found devices are listed with signal strength and distance estimates
3. **Add Favorites**: Tap the star icon to save devices to your favorites
4. **Get Guidance**: Tap on any device to see detailed navigation instructions
5. **Use Compass**: The compass shows your current orientation to help with navigation

## Distance Indicators

- 🎯 **Very Close** (≤0.3m): You're almost on top of it!
- 📍 **Extremely Close** (≤0.5m): It's right here!
- 🔍 **Close** (≤1m): Look around carefully
- 👀 **Medium Close** (≤2m): Scan nearby area
- 🔭 **Medium** (≤4m): Scan the area
- 🔎 **Medium Far** (≤8m): Search wider area
- 🏃 **Far** (≤15m): Move around to get closer
- 🚶 **Very Far** (>15m): Start walking in any direction

## Movement Tracking

- ✅ **Getting Closer**: Signal is improving - keep going!
- ❌ **Moving Away**: Signal is weakening - try different direction
- ⏸️ **Signal Stable**: No significant change - look around
- ❓ **Movement Unclear**: Not enough data - keep moving

## Technical Details

### Permissions Required
- Bluetooth permissions (BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
- Location permissions (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
- Vibration permission for feedback

### Supported Device Types
- **BLE**: Bluetooth Low Energy devices (most common)
- **BT**: Classic Bluetooth devices

### Distance Calculation
The app uses an enhanced path loss model to estimate distance based on RSSI values, providing accuracy ranges from ±0.2m for very close devices to ±5.0m for very far devices.

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on a device with Bluetooth support

## Requirements

- Android API level 24+ (Android 7.0+)
- Device with Bluetooth and Bluetooth LE support
- Location services enabled (required for device discovery)

## Notes

- Signal strength can vary due to obstacles and interference
- For best accuracy, calibrate your compass outdoors first
- Some devices may not show names but will still be detected
- The app works best in open spaces with minimal interference
