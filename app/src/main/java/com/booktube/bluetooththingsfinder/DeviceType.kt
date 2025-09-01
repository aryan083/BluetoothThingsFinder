package com.booktube.bluetooththingsfinder

enum class DeviceType(val displayName: String, val shortName: String, val icon: String) {
    LAPTOP("Laptop", "Laptop", "💻"),
    PHONE("Phone", "Phone", "📱"),
    TABLET("Tablet", "Tablet", "📱"),
    HEADPHONES("Headphones", "Headphones", "🎧"),
    SPEAKER("Speaker", "Speaker", "🔊"),
    SMARTWATCH("Smart Watch", "Watch", "⌚"),
    SMART_TV("Smart TV", "TV", "📺"),
    GAMING_CONSOLE("Gaming Console", "Gaming", "🎮"),
    PRINTER("Printer", "Printer", "🖨️"),
    CAMERA("Camera", "Camera", "📷"),
    KEYBOARD("Keyboard", "Keyboard", "⌨️"),
    MOUSE("Mouse", "Mouse", "🖱️"),
    ROUTER("Router", "Router", "📡"),
    SMART_HOME("Smart Home", "Home", "🏠"),
    CAR("Car", "Car", "🚗"),
    BLE("Bluetooth Device", "BLE", "📶"),
    CLASSIC("Bluetooth Device", "BT", "📶");
    
    companion object {
        fun detectDeviceType(deviceName: String, deviceAddress: String, bluetoothClass: Int?): DeviceType {
            val name = deviceName.lowercase()
            val address = deviceAddress.lowercase()
            
            return when {
                // Laptops
                name.contains("laptop") || name.contains("macbook") || name.contains("thinkpad") || 
                name.contains("dell") || name.contains("hp") || name.contains("lenovo") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") ||
                address.startsWith("00:21:6a") || address.startsWith("00:24:e8") -> LAPTOP
                
                // Phones
                name.contains("iphone") || name.contains("samsung") || name.contains("galaxy") ||
                name.contains("pixel") || name.contains("oneplus") || name.contains("xiaomi") ||
                address.startsWith("00:23:76") || address.startsWith("00:26:08") ||
                address.startsWith("00:1a:11") || address.startsWith("00:1c:b3") -> PHONE
                
                // Tablets
                name.contains("ipad") || name.contains("tablet") || name.contains("surface") ||
                address.startsWith("00:1e:c9") || address.startsWith("00:1f:5b") -> TABLET
                
                // Headphones
                name.contains("airpods") || name.contains("headphone") || name.contains("earbud") ||
                name.contains("sony") || name.contains("bose") || name.contains("jbl") ||
                name.contains("beats") || name.contains("sennheiser") ||
                address.startsWith("00:18:91") || address.startsWith("00:1b:63") ||
                address.startsWith("00:1f:3a") || address.startsWith("00:24:e8") -> HEADPHONES
                
                // Speakers
                name.contains("speaker") || name.contains("sound") || name.contains("audio") ||
                name.contains("bluetooth speaker") || name.contains("portable speaker") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> SPEAKER
                
                // Smart Watches
                name.contains("watch") || name.contains("fitbit") || name.contains("garmin") ||
                name.contains("galaxy watch") || name.contains("apple watch") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> SMARTWATCH
                
                // Smart TVs
                name.contains("tv") || name.contains("television") || name.contains("samsung tv") ||
                name.contains("lg tv") || name.contains("sony tv") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> SMART_TV
                
                // Gaming Consoles
                name.contains("playstation") || name.contains("xbox") || name.contains("nintendo") ||
                name.contains("switch") || name.contains("ps4") || name.contains("ps5") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> GAMING_CONSOLE
                
                // Printers
                name.contains("printer") || name.contains("hp") || name.contains("canon") ||
                name.contains("epson") || name.contains("brother") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> PRINTER
                
                // Cameras
                name.contains("camera") || name.contains("canon") || name.contains("nikon") ||
                name.contains("sony") || name.contains("gopro") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> CAMERA
                
                // Keyboards
                name.contains("keyboard") || name.contains("logitech") || name.contains("mechanical") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> KEYBOARD
                
                // Mice
                name.contains("mouse") || name.contains("trackpad") || name.contains("touchpad") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> MOUSE
                
                // Routers
                name.contains("router") || name.contains("modem") || name.contains("wifi") ||
                name.contains("netgear") || name.contains("linksys") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> ROUTER
                
                // Smart Home
                name.contains("smart") || name.contains("home") || name.contains("nest") ||
                name.contains("philips") || name.contains("hue") || name.contains("alexa") ||
                name.contains("google home") || name.contains("echo") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> SMART_HOME
                
                // Cars
                name.contains("car") || name.contains("bmw") || name.contains("mercedes") ||
                name.contains("audi") || name.contains("toyota") || name.contains("honda") ||
                address.startsWith("00:1b:63") || address.startsWith("00:1f:3a") -> CAR
                
                // Default based on Bluetooth class
                bluetoothClass != null && bluetoothClass != 0 -> CLASSIC
                else -> BLE
            }
        }
        
        fun fromBluetoothClass(bluetoothClass: Int?): DeviceType {
            return if (bluetoothClass != null && bluetoothClass != 0) {
                CLASSIC
            } else {
                BLE
            }
        }
    }
}
