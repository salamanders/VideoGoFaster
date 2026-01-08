# ENV2.md: Emulator Environment Investigation

## System Profiling
*Date: 2025-02-21*

### Hardware Stats
- **Kernel:** Linux devbox 6.8.0 x86_64
- **OS:** Ubuntu 24.04.3 LTS (Noble Numbat)
- **CPU:** Intel(R) Xeon(R) Processor @ 2.30GHz (4 Cores)
  - *Note:* Hypervisor is KVM, but `/dev/kvm` is **NOT** accessible.
- **Memory:** 7.8 GiB Total (7.0 GiB Free)
- **Disk:** ~93 GiB Free
- **Java:** OpenJDK 21.0.8

### Critical Constraint: No Hardware Acceleration
The absence of `/dev/kvm` forces the Android Emulator to use software rendering (`-accel off -gpu swiftshader_indirect`). This places 100% of the emulation load on the CPU.
**Result:** Modern Android system images (API 28+) fail to boot to a usable state within reasonable timeframes (>5-10 mins). They get stuck initializing system services (ActivityManager, PackageManager) or performing disk encryption (`vold`).

---

## Lab Notebook: Boot Attempts

### Attempt 1: The Target (API 35, Pixel 4)
- **Config:** `system-images;android-35;google_apis;x86_64`, Device: "Pixel 4"
- **Command:** `emulator -avd test_avd_35 -gpu swiftshader_indirect -no-window -no-audio -no-boot-anim -accel off`
- **Result:** **FAILURE (Timeout)**
  - Process ran for >5 minutes.
  - `adb devices` detected the emulator.
  - `init.svc.bootanim` was `running`.
  - `sys.boot_completed` never returned `1`.
  - `dumpsys activity` returned "Can't find service: activity".
  - **Analysis:** The system server could not initialize. Zygote and AudioFlinger warnings in logcat.

### Attempt 2: Low Res Target (API 35, Nexus One)
- **Config:** `system-images;android-35;google_apis;x86_64`, Device: "Nexus One" (480x800)
- **Result:** **FAILURE (Crash)**
  - The process exited silently shortly after launch.
  - No log output was captured.
  - **Analysis:** Potentially an incompatibility with the screen density/resolution and the API 35 image in software mode.

### Attempt 3: Mid-Range Fallback (API 30, Nexus One)
- **Config:** `system-images;android-30;google_apis;x86_64`, Device: "Nexus One"
- **Result:** **FAILURE (Timeout)**
  - Similar to Attempt 1.
  - `init.svc.bootanim` eventually `stopped`, but `sys.boot_completed` remained empty.
  - Logcat showed `iorapd` failing to connect to `package_native` service.
  - **Analysis:** Even Android 11 (R) is too heavy for this software-only environment.

### Attempt 4: Legacy Fallback (API 28, Nexus One)
- **Config:** `system-images;android-28;google_apis;x86_64`, Device: "Nexus One"
- **Result:** **FAILURE (Stuck at Encryption)**
  - Logcat showed endless `vold` (Volume Daemon) logs: `Copying 8 blocks...`, `Encrypting group...`.
  - **Analysis:** The lack of AES-NI passthrough or just raw CPU slowness makes the initial disk encryption phase take forever.

---

## Final Setup Script
*Note: While no emulator fully booted in this session, the following script represents the correct "Best Effort" configuration to install the latest tools and attempt a boot. It uses API 35 as requested.*

```bash
# 1. Export Environment Variables (REQUIRED)
export ANDROID_HOME=$HOME/Android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator

# 2. Install Dependencies (Command Line Tools)
# (Assumes cmdline-tools are already downloaded to $ANDROID_HOME/cmdline-tools/latest)
# If not, download from: https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip

# 3. Accept Licenses
yes | sdkmanager --licenses > /dev/null

# 4. Install SDK Packages
# We install API 35 (Target) and API 30 (Fallback)
sdkmanager "platform-tools" "emulator" "build-tools;35.0.0" "platforms;android-36" "platforms;android-35" "system-images;android-35;google_apis;x86_64"

# 5. Create AVD (Pixel 4 Profile)
echo "no" | avdmanager create avd -n "jules_avd_35" -k "system-images;android-35;google_apis;x86_64" --device "pixel_4" --force

# 6. Run Emulator (Background Mode)
# Flags optimized for headless/no-acceleration environments
nohup emulator -avd jules_avd_35 \
  -gpu swiftshader_indirect \
  -no-window \
  -no-audio \
  -no-boot-anim \
  -accel off \
  -no-snapshot \
  > emulator.log 2>&1 &

# 7. Wait for Boot (Loop)
echo "Waiting for emulator to boot..."
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
  sleep 5
  echo -n "."
done
echo "Emulator Booted!"
```
