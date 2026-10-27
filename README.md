# COMP3074 Lab Exercise 5

Android location exercise by Phillip Onofua. Displays latitude and longitude using the fused location provider with high accuracy, and requests foreground fine/coarse location permission at runtime.

- **Tap to get location:** request the current coordinates.
- **Start Listener:** request location updates every five seconds.
- **Stop Listener:** remove the active updates. Updates also stop when the screen leaves the foreground.

Open in Android Studio, sync Gradle and run on a device with Google Play services and location enabled. Minimum Android version: API 24. Grant precise location for GPS accuracy; approximate permission is handled too.

Build: `./gradlew :app:assembleDebug`

For emulator testing, supply a simulated GPS position through Extended Controls → Location. Coordinates from an emulator are test positions, not the student's physical location.
