# Hermit

Hermit is an Android on-device LLM app built with Jetpack Compose (Kotlin) and a native C++ inference layer powered by llama.cpp.

It lets you:
- discover and download GGUF models from Hugging Face,
- load local downloaded models,
- run fully local CPU inference,
- stream tokens in real time,
- store chat sessions and messages in Room.

## What This Project Contains

```text
hermit/
|- app/                # Android app (Compose UI, ViewModels, Room, downloads)
|- nativelib/          # JNI bridge + CMake + llama.cpp integration
|- gradle/             # Version catalog and wrapper config
|- build.gradle.kts    # Root build script
|- settings.gradle.kts # Multi-module config (:app, :nativelib)
```

## Stack And Key Versions

| Area | Value |
|---|---|
| Android Gradle Plugin | 9.1.0 |
| Gradle Wrapper | 9.3.1 |
| Kotlin | 2.2.10 |
| compileSdk / targetSdk | 36 |
| minSdk | 30 |
| Java source/target compatibility | 11 |
| Gradle daemon toolchain | Java 21 |
| Native build | CMake 3.22.1 + NDK |
| ABIs | arm64-v8a, x86_64 |

## Prerequisites

Install the following before opening the project:

1. Android Studio (latest stable recommended).
2. Android SDK Platform 36.
3. Android SDK Build-Tools compatible with AGP 9.x.
4. Android NDK (installed via SDK Manager).
5. CMake 3.22.1 (installed via SDK Manager).
6. Git.

Optional but useful:
- A physical Android device (API 30+) for better native performance.
- Plenty of free storage for model files (often 0.7 GB to 2.5+ GB each).

## Clone And Open

```bash
git clone https://github.com/sutirtha0505/hermit.git
cd hermit
```

Open the folder in Android Studio and let Gradle sync.

Note:
- This repository already includes the native llama.cpp source under nativelib/src/main/cpp/llama.cpp.

## Project Configuration

### 1) local.properties

Android Studio usually creates local.properties automatically.

If needed, create/update it manually:

```properties
sdk.dir=/Users/<your-username>/Library/Android/sdk
```

Do not commit local.properties.

### 2) SDK, NDK, CMake

From Android Studio:
1. Open Settings -> Languages and Frameworks -> Android SDK.
2. Install SDK Platform 36.
3. In SDK Tools, install:
	- Android NDK
	- CMake 3.22.1

### 3) Native ABI targets

The native module builds only for:
- arm64-v8a
- x86_64

This is configured in nativelib/build.gradle.kts and is important for emulator/device compatibility.

## How To Run

### Run From Android Studio (recommended)

1. Select the app run configuration.
2. Choose a device/emulator:
	- arm64 device preferred, or
	- x86_64 emulator.
3. Click Run.

First build compiles JNI + C++ sources and can take longer.

### Run From Command Line

From project root:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

If a device is attached, launch with adb:

```bash
adb shell am start -n com.example.hermit/.MainActivity
```

## Build, Test, And Useful Commands

```bash
# Build
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease

# Install debug APK
./gradlew installDebug

# Unit tests
./gradlew test

# Instrumented tests (device/emulator required)
./gradlew connectedAndroidTest
```

## App Usage Guide

### Download a model

1. Open the app.
2. Go to Models (settings icon in top bar).
3. Tap the add button to open the download screen.
4. Search Hugging Face and pick a GGUF model.
5. Tap Download.

The app fetches model metadata from:
- https://huggingface.co/api/models

### Load a downloaded model

1. Return to the Local Models screen.
2. Tap Load for a downloaded GGUF file.
3. Wait for model initialization.

### Start chatting

1. After a model is loaded, type a prompt.
2. Responses stream token-by-token.
3. Use Stop to cancel generation.

### Where model files are stored

Downloaded models are stored in app-specific external files directory:

```text
/Android/data/com.example.hermit/files/models/
```

## Architecture Notes

- UI: Jetpack Compose screens and Material 3 components.
- State: ViewModel-driven state (chat + downloads).
- Persistence: Room database (hermit_chat_db) for chat sessions/messages.
- Native: JNI bridge in nativelib calling llama.cpp APIs.
- Generation:
  - model context window is currently set to 2048 tokens,
  - token streaming callback updates the latest assistant message.

## Permissions And Networking

Declared permissions include INTERNET and media/external read permissions.

Behavior summary:
- Inference is local on-device CPU.
- Network is used for model discovery/download from Hugging Face.
- No telemetry pipeline is configured in this codebase.

## Important Current Limitations

- Import model button is present in UI but file-picker import is not implemented yet.
- Generation backend is CPU-focused in current configuration.
- Very large models may fail on low-memory devices.
- Download success depends on remote server Range support and stable connectivity.

## Troubleshooting

### Gradle sync fails

- Confirm SDK path in local.properties.
- Confirm JDK availability in Android Studio Gradle settings.
- Re-run:

```bash
./gradlew --stop
./gradlew clean
./gradlew assembleDebug
```

### Native/CMake errors (jni.h not found, CMake configure fail)

- Ensure NDK and CMake 3.22.1 are installed from SDK Manager.
- In Android Studio, run Build -> Refresh Linked C++ Projects.

### App installs but model loading fails

- Verify the GGUF file exists and is fully downloaded.
- Try smaller Q4 models first (for example, 1B-1.5B class).
- Check Logcat tag HermitNative for native loading errors.

### Download problems

- Confirm device internet access.
- Retry download from the model list.
- If partial file is corrupted, delete it from app storage and re-download.

## Contributing

1. Fork and create a feature branch.
2. Keep changes module-scoped (app or nativelib).
3. Run debug build and tests before opening a PR.
4. Include clear reproduction and validation steps in PR description.

## License

This project is licensed under the GNU General Public License v3.0.
See [LICENSE](LICENSE).
