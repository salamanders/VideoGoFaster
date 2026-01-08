# Android Emulator Setup in Sandbox

This document describes how to set up and run the Android Emulator in the Jules isolated sandbox environment.

## Investigation Findings

The sandbox environment runs on Ubuntu 24.04 (Noble Numbat) on x86_64 architecture.
Crucially, **KVM (Kernel-based Virtual Machine) is not available** (`/dev/kvm` access fails or is missing).
This means hardware acceleration for the emulator is disabled.

The emulator must run in **software rendering mode** (`swiftshader_indirect`) with acceleration explicitly disabled (`-accel off`).
**Warning:** This mode is extremely slow and unstable. While the emulator process starts, Android system services (like `PackageManager`) often fail to initialize or crash due to timeouts, making `adb install` and Instrumented Tests unreliable.

## Setup Instructions

### 1. Prerequisites
Ensure `java`, `wget`, and `unzip` are installed (available by default).

### 2. Install Android SDK Command Line Tools
Run the following commands to set up the SDK structure and download the tools:

```bash
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip
mv cmdline-tools latest
rm cmdline-tools.zip
```

### 3. Configure Environment Variables
Export these variables (add to `~/.bashrc` or run in your session):

```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
```

### 4. Install SDK Packages
Install the platforms and tools required by the project (`compileSdk 36`, `minSdk 35`):

```bash
# Accept licenses
yes | sdkmanager --licenses > /dev/null

# Install packages
# Note: Using system-images;android-35 because 36 images may be unstable/unavailable
sdkmanager "platform-tools" "platforms;android-36" "build-tools;35.0.0" "emulator" "system-images;android-35;google_apis;x86_64"
```

### 5. Create Android Virtual Device (AVD)

```bash
echo "no" | avdmanager create avd -n test_avd -k "system-images;android-35;google_apis;x86_64" --force
```

### 6. Start the Emulator
Run headless with software rendering:

```bash
emulator -avd test_avd -no-window -gpu swiftshader_indirect -no-audio -no-boot-anim -accel off > emulator.log 2>&1 &
```

Wait for the device:
```bash
adb wait-for-device
# Note: sys.boot_completed may timeout or never return '1' in this environment
adb shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done;'
```

## Baseline Test Results

### Unit Tests
**Status: PASS**
Command: `./gradlew testDebugUnitTest`
Result: Tests passed successfully.

### Instrumented Tests
**Status: FAIL**
Command: `./gradlew connectedDebugAndroidTest`
Result: Failed to run.
Details:
The emulator boots (adb connects), but `adb install` fails, preventing tests from running.
Common errors observed:
*   `cmd: Can't find service: package` (PackageManagerService failed to start)
*   `java.lang.NullPointerException` in `StorageManagerService` (Dependency on PackageManager failed)
*   `Broken pipe` during APK push/install.

**Conclusion:** Instrumented tests are currently not viable in this unaccelerated sandbox environment due to emulator instability.
Attempts to increase `vm.heapSize` to 256M and use `hw.ramSize=2G` did not resolve the boot timeout or `PackageManager` crashes.
The emulator processes start, but system services often fail to initialize within a reasonable timeframe (>10 minutes).
