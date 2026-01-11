# Android Emulator Setup in Sandbox

This document describes how to set up and run the Android Emulator in the Jules isolated sandbox environment.

## Investigation Findings

The sandbox environment runs on Ubuntu 24.04 (Noble Numbat) on x86_64 architecture.
Crucially, **KVM (Kernel-based Virtual Machine) is not available** (`/dev/kvm` access fails or is missing, and CPU flags lack `vmx`).
This means hardware acceleration for the emulator is disabled.

However, it is possible to run the emulator in **software rendering mode** (`swiftshader_indirect`) with acceleration explicitly disabled (`-accel off`).
**Warning:** This mode is extremely slow. Booting the emulator can take **5-10 minutes or more**, and `adb` operations may time out. The device may appear as `offline` in `adb devices` for a significant period during boot.

## Setup Instructions

### 1. Prerequisites
Ensure `java`, `wget`, and `unzip` are installed (available by default in this environment).

### 2. Install Android SDK Command Line Tools
Download the latest Linux command line tools from Google.

```bash
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip
mv cmdline-tools latest
rm cmdline-tools.zip
```

### 3. Configure Environment Variables
Set up `ANDROID_HOME` and update `PATH`. You may want to add this to your `.bashrc` or export it in your session.

```bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
```

### 4. Install SDK Packages
Install the necessary platform tools, platforms, build tools, emulator, and system images.
Note: Adjust versions as needed by your project.

```bash
# Accept licenses
yes | sdkmanager --licenses > /dev/null

# Install packages
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" "emulator" "system-images;android-35;google_apis;x86_64"
```

### 5. Create Android Virtual Device (AVD)
Create an AVD named `test_avd`.

```bash
echo "no" | avdmanager create avd -n test_avd -k "system-images;android-35;google_apis;x86_64" --force
```

### 6. Start the Emulator
Run the emulator in headless mode with software rendering and acceleration disabled.
Recommended flags for debugging boot issues include `-verbose`, `-show-kernel`, and `-no-snapshot-save`.

```bash
emulator -avd test_avd -no-window -gpu swiftshader_indirect -no-audio -no-boot-anim -accel off -no-snapshot-save -wipe-data -show-kernel &
```

*   `-no-window`: Run headless (no GUI).
*   `-gpu swiftshader_indirect`: Use SwiftShader for software graphics rendering.
*   `-no-audio`: Disable audio support.
*   `-no-boot-anim`: Skip boot animation for faster startup.
*   `-accel off`: Explicitly disable hardware acceleration (required since KVM is missing).
*   `-show-kernel`: Output kernel logs to stdout (crucial for verifying boot progress in this slow environment).
*   `-no-snapshot-save` / `-wipe-data`: Ensure a clean boot to avoid snapshot corruption issues.

### 7. Verification
Check if the device is attached. It will initially be `offline` and eventually become `device`.

```bash
adb devices
```

Monitor the boot progress by watching the kernel logs (if using `-show-kernel`) or by polling the boot completion property:

```bash
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done;'
```
