# Bluetooth Things Finder

A simple Android app to locate things via Bluetooth, similar to Apple's AirTag. The app scans for nearby Bluetooth devices and helps you find them based on signal strength and direction indicators.

## Features

- **Bluetooth Device Scanning**: Scan for nearby Bluetooth Low Energy (BLE) devices
- **Signal Strength Visualization**: Color-coded signal strength indicators (Green = Very Close, Red = Far)
- **Direction Indicators**: Emoji-based direction hints to help locate devices
- **Distance Estimation**: Approximate distance calculations based on RSSI values
- **Compass Integration**: Built-in compass to show your current orientation
- **Favorites System**: Save frequently used devices for quick access
- **Device History**: Track previously found devices
- **Vibration Feedback**: Haptic feedback when new devices are discovered
- **Tabbed Interface**: Separate tabs for scan results and favorites

## How to Use

### 1. Getting Started
- Launch the app
- Grant necessary permissions (Bluetooth, Location, Vibration)
- Ensure Bluetooth is enabled on your device

### 2. Finding Devices
- Tap the "Scan" button to start searching
- The app will scan for 10 seconds
- Found devices are displayed with signal strength and distance information
- Devices are automatically sorted by signal strength (strongest first)

### 3. Understanding the Interface
- **Compass**: Shows your current orientation (North = 0°, East = 90°, etc.)
- **Signal Strength**: Color-coded bars indicating proximity
- **Distance Estimates**: Approximate distance in meters with confidence levels
- **Direction Indicators**: 
  - 📍 Very Close: You're almost on top of it!
  - 🔍 Close: Look around carefully
  - 👀 Medium: Scan the area
  - 🔭 Far: Move around to get closer

### 4. Managing Favorites
- Tap the star icon on any device to add/remove from favorites
- Access your favorite devices in the "Favorites" tab
- Favorites are saved between app sessions

### 5. Tips for Better Results
- Move around slowly while scanning
- Keep the app open during searches
- Signal strength can vary due to obstacles
- Some devices may not display names
- Use the compass to understand your orientation

## Technical Details

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 14)
- **Permissions Required**:
  - `BLUETOOTH_SCAN`
  - `BLUETOOTH_CONNECT`
  - `ACCESS_FINE_LOCATION`
  - `VIBRATE`

## How It Works

The app uses Bluetooth Low Energy (BLE) scanning to discover nearby devices. It measures the Received Signal Strength Indicator (RSSI) to estimate proximity and provides visual feedback through:

1. **Signal Strength Colors**: Green (close) to Red (far)
2. **Distance Estimation**: Based on RSSI values using a simplified path loss model
3. **Direction Indicators**: Emoji-based hints for user guidance
4. **Compass Integration**: Uses device sensors to show orientation

## Limitations

- Distance estimates are approximate and may not be accurate in all environments
- Signal strength can be affected by obstacles, interference, and device orientation
- Some Bluetooth devices may not be discoverable
- Accuracy depends on the quality of your device's Bluetooth hardware

## Privacy & Security

- The app only scans for Bluetooth devices and does not connect to them
- No personal data is transmitted or stored
- Device addresses are stored locally for favorites and history
- Location permission is required for Bluetooth scanning (Android requirement)

## Troubleshooting

- **No devices found**: Ensure Bluetooth is enabled and try moving around
- **Permission errors**: Grant all requested permissions in device settings
- **Scan failures**: Restart Bluetooth or restart the app
- **Poor accuracy**: Move slowly and avoid obstacles

## Development

This app is built with:
- Kotlin
- Jetpack Compose
- Android Bluetooth APIs
- Material Design 3

## License

This project is open source and available under the MIT License.
