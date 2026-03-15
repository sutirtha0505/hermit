# 🐚 Hermit AI
Hermit AI is a fully on-device, private, and lightweight Large Language Model (LLM) assistant built for Android. It utilizes `llama.cpp` over the Android NDK to execute cutting-edge open-source models (like Llama 3, Mistral, and Gemma) exclusively on your local device CPU, completely offline and without any data leaving your phone.

## 🌟 Key Features
* **🔥 Native On-Device Inference:** Uses a highly customized C++ `ggml` backend over JNI to achieve robust token generation capabilities locally.
* **🔎 Hugging Face Integration:** A built-in "Model Store" allows you to search and download any `.gguf` quantized models directly from the Hugging Face hub inside the app.
* **💬 Dynamic Chat Templates:** The native engine automatically extracts and applies the correct instruct formatting tags (e.g., `<start_of_turn>user\n`) embedded within the specific downloaded model.
* **⏳ Real-Time Streaming UI:** Fast Jetpack Compose UI with real-time token streaming and asynchronous "Stop Generation" capabilities hooked directly into the lower-level inference decoder loop.
* **📱 Modern Android Architecture:** Built in Kotlin utilizing Jetpack Compose, Coroutines, MVVM, and Material 3 Design.

## 🛠️ Technology Stack
* **Language:** Kotlin & C++17
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Native Interop:** Android NDK (JNI)
* **LLM Backend:** [`llama.cpp`](https://github.com/ggml-org/llama.cpp)
* **Networking (Model Store):** Retrofit2, OkHttp3
* **Serialization:** Kotlinx Serialization

## 🚀 Getting Started
To get the project running locally within Android Studio, follow these steps:

### Prerequisites
* Android Studio (Jellyfish or later recommended)
* Android NDK (e.g., `28.2.13676358` or whatever is currently selected via SDK Manager)
* CMake (`3.22.1` or later)

### Cloning the Repository
Since `llama.cpp` is injected directly into the source tree, you must clone the repository recursively (if submoduled), or clone `llama.cpp` manually into the native library path:
```bash
git clone https://github.com/sutirtha0505/hermit.git
cd hermit
```
*(Note: If `llama.cpp` is missing from `nativelib/src/main/cpp/llama.cpp`, you will need to clone it there)*

### Building the Project
1. Open the project in Android Studio.
2. Wait for the initial Gradle Sync. Do not be alarmed by temporary C++ indexer warnings (red squiggles under `<jni.h>`). 
3. Run **Build > Refresh Linked C++ Projects**.
4. Click **Run 'app'** (`Shift + F10`) to compile the native `.so` libraries for your specific ABI target (`arm64-v8a` or `x86_64`) and deploy the APK to your emulator or physical device.

## 📥 Downloading a Model
1. Open the app and click the **Settings (⚙️)** gear icon in the top right.
2. Click the **(+) FAB** to open the Model Download Screen.
3. Search for a small quantized model (e.g., `tinyllama` or `gemma Q4`).
4. Select a `.gguf` file and it will begin downloading directly to your Android App's internal storage directory.
5. Once complete, return to the Models list and click **Load** on your new model! 

## 📝 Known Limitations & Next Steps
- **GPU Acceleration:** Currently restricted to standard CPU inference for broad device compatibility. Adding Vulkan/OpenCL backends is planned.
- **Chat Persistence:** The Chat Drawer currently resets on app closure. We plan to migrate to a local SQLite (Room) database to save session history.
- **Background Downloads:** Implementing foreground services for model downloads so they don't pause upon minimizing.

## 🛡️ Privacy & Security
Hermit AI is 100% offline. The only network calls made by the application are towards the Hugging Face Hub (api.huggingface.co) explicitly for browsing model metadata and downloading weights. **No telemetry, chat history, or personal data is collected or sent.** Everything processing inside the chat window stays executing locally in your phone's RAM.
