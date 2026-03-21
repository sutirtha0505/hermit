#include <android/log.h> // NOLINT
#include <atomic>        // NOLINT
#include <jni.h>         // NOLINT
#include <mutex>
#include <string> // NOLINT
#include <thread>
#include <vector> // NOLINT

#include "ggml.h"
#include "llama.h"

#define TAG "HermitNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Global state
static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;
static llama_sampler *g_sampler = nullptr;
static std::mutex g_mutex;
static std::atomic<bool> g_cancel_generation(false);
static bool g_gpu_active = false;
static bool g_gpu_fallback_triggered = false;

static void common_log_callback(ggml_log_level level, const char *text,
                                void *user_data) {
  (void)user_data;
  if (level == GGML_LOG_LEVEL_ERROR) {
    LOGE("%s", text);
  } else {
    LOGI("%s", text);
  }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_nativelib_NativeLib_loadModel(JNIEnv *env, jobject thiz,
                                               jstring model_path, jboolean use_gpu) {
  std::lock_guard<std::mutex> lock(g_mutex);
  const char *path = env->GetStringUTFChars(model_path, nullptr);
  LOGI("Loading model from: %s (GPU requested: %s)", path, use_gpu ? "true" : "false");

  llama_log_set(common_log_callback, nullptr);
  llama_backend_init();

  llama_model_params model_params = llama_model_default_params();
  g_gpu_fallback_triggered = false;

  if (use_gpu) {
    #if defined(GGML_USE_VULKAN) || defined(GGML_USE_METAL) || defined(GGML_USE_CUBLAS) || defined(GGML_USE_CUDA) || defined(GGML_USE_KOMPUTE)
      model_params.n_gpu_layers = 999;
      LOGI("GPU offloading enabled (attempting 999 layers)");
    #else
      model_params.n_gpu_layers = 0;
      LOGE("GPU requested but no GPU backend compiled!");
      env->ReleaseStringUTFChars(model_path, path);
      return -2; // Error code for "GPU not supported"
    #endif
  } else {
    model_params.n_gpu_layers = 0;
  }

  g_model = llama_model_load_from_file(path, model_params);

  if (g_model == nullptr) {
    LOGE("Failed to load model from %s", path);
    env->ReleaseStringUTFChars(model_path, path);
    return -1;
  }

  env->ReleaseStringUTFChars(model_path, path);

  g_gpu_active = (model_params.n_gpu_layers > 0);

  llama_context_params ctx_params = llama_context_default_params();
  ctx_params.n_ctx = 2048; // Context size
  const unsigned int detected_cores = std::thread::hardware_concurrency();
  const int32_t thread_count = detected_cores > 0 ? static_cast<int32_t>(detected_cores) : 1;
  ctx_params.n_threads = thread_count;
  ctx_params.n_threads_batch = thread_count;

  g_ctx = llama_init_from_model(g_model, ctx_params);
  if (g_ctx == nullptr) {
    LOGE("Failed to create context with model");
    llama_model_free(g_model);
    g_model = nullptr;
    return -1;
  }

  llama_set_n_threads(g_ctx, thread_count, thread_count);
  LOGI("Inference threads configured: n_threads=%d, n_threads_batch=%d, detected_cores=%u",
       llama_n_threads(g_ctx), llama_n_threads_batch(g_ctx), detected_cores);

  const llama_vocab *vocab = llama_model_get_vocab(g_model);
  llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
  g_sampler = llama_sampler_chain_init(sparams);
  llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(0.8f));
  llama_sampler_chain_add(g_sampler,
                          llama_sampler_init_dist(42)); // Fixed arbitrary seed

  LOGI("Model loaded successfully. GPU Active: %s", g_gpu_active ? "true" : "false");
  return 0; // Success
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_nativelib_NativeLib_unloadModel(JNIEnv *env, jobject thiz) {
  std::lock_guard<std::mutex> lock(g_mutex);
  LOGI("Unloading model");

  if (g_sampler) {
    llama_sampler_free(g_sampler);
    g_sampler = nullptr;
  }
  if (g_ctx) {
    llama_free(g_ctx);
    g_ctx = nullptr;
  }
  if (g_model) {
    llama_model_free(g_model);
    g_model = nullptr;
  }
  g_gpu_active = false;
  g_gpu_fallback_triggered = false;
  llama_backend_free();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_nativelib_NativeLib_stopGeneration(JNIEnv *env, jobject thiz) {
  g_cancel_generation = true;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_nativelib_NativeLib_getContextSize(JNIEnv *env, jobject thiz) {
  std::lock_guard<std::mutex> lock(g_mutex);
  if (!g_ctx) return 0;
  return static_cast<jint>(llama_n_ctx(g_ctx));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nativelib_NativeLib_getBackendName(JNIEnv *env, jobject thiz) {
  std::lock_guard<std::mutex> lock(g_mutex);
  if (!g_model) return env->NewStringUTF("None");

  if (g_gpu_fallback_triggered) {
      return env->NewStringUTF("CPU (GPU Fallback)");
  }

  if (!g_gpu_active) {
      return env->NewStringUTF("CPU");
  }

  #if defined(GGML_USE_VULKAN)
    return env->NewStringUTF("Vulkan (GPU)");
  #elif defined(GGML_USE_METAL)
    return env->NewStringUTF("Metal (GPU)");
  #elif defined(GGML_USE_CUBLAS) || defined(GGML_USE_CUDA)
    return env->NewStringUTF("CUDA (GPU)");
  #elif defined(GGML_USE_SYCL)
    return env->NewStringUTF("SYCL (GPU)");
  #elif defined(GGML_USE_KOMPUTE)
    return env->NewStringUTF("Kompute (GPU)");
  #else
    return env->NewStringUTF("CPU");
  #endif
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nativelib_NativeLib_completion(JNIEnv *env, jobject thiz,
                                                jstring prompt) {
  std::lock_guard<std::mutex> lock(g_mutex);

  if (!g_model || !g_ctx || !g_sampler) {
    LOGE("Model not loaded");
    return env->NewStringUTF("Error: Model not loaded.");
  }

  const char *p = env->GetStringUTFChars(prompt, nullptr);
  std::string prompt_str(p);
  env->ReleaseStringUTFChars(prompt, p);

  LOGI("Generating completion for raw prompt: %s", prompt_str.c_str());

  g_cancel_generation = false;

  const char *tmpl = llama_model_chat_template(g_model, nullptr);
  std::string final_prompt = prompt_str;

  if (tmpl != nullptr) {
    std::vector<llama_chat_message> msgs;
    msgs.push_back({"system", "You are a helpful and concise AI assistant."});
    msgs.push_back({"user", prompt_str.c_str()});

    std::vector<char> formatted(8192);
    int32_t len =
        llama_chat_apply_template(tmpl, msgs.data(), msgs.size(), true,
                                  formatted.data(), formatted.size());
    if (len > 0) {
      if (len > formatted.size()) {
        formatted.resize(len);
        llama_chat_apply_template(tmpl, msgs.data(), msgs.size(), true,
                                  formatted.data(), formatted.size());
      }
      final_prompt = std::string(formatted.data(), len);
    }
  }

  LOGI("Formatted prompt: %s", final_prompt.c_str());

  std::vector<llama_token> embd_inp;

  const int n_prompt_max = final_prompt.length() + 128; // Estimate
  embd_inp.resize(n_prompt_max);

  const llama_vocab *vocab = llama_model_get_vocab(g_model);
  int n_prompt =
      llama_tokenize(vocab, final_prompt.c_str(), final_prompt.length(),
                     embd_inp.data(), embd_inp.size(), true, true);
  if (n_prompt < 0) {
    LOGE("Failed to tokenize");
    return env->NewStringUTF("Error: Tokenization failed.");
  }
  embd_inp.resize(n_prompt);

  llama_batch batch = llama_batch_get_one(embd_inp.data(), embd_inp.size());

  if (llama_decode(g_ctx, batch)) {
    LOGE("Failed to decode");
    return env->NewStringUTF("Error: Decode failed.");
  }

  std::string final_response = "";
  int n_cur = batch.n_tokens;
  int n_predict = 1024; // Increased token limit for actual replies

  jclass cls = env->GetObjectClass(thiz);
  jmethodID postTokenId =
      env->GetMethodID(cls, "postToken", "(Ljava/lang/String;)V");

  while (n_cur <= n_prompt + n_predict) {
    if (g_cancel_generation) {
      LOGI("Generation cancelled externally.");
      break;
    }

    llama_token new_token_id = llama_sampler_sample(g_sampler, g_ctx, -1);
    llama_sampler_accept(g_sampler, new_token_id);

    if (llama_vocab_is_eog(vocab, new_token_id)) {
      LOGI("End of generation reached.");
      break;
    }

    char buf[256];
    int n =
        llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
    if (n < 0) {
      LOGE("Failed to convert token to piece.");
      break;
    }

    std::string piece(buf, n);
    final_response += piece;

    // Pass token to Kotlin callback
    jstring jPiece = env->NewStringUTF(piece.c_str());
    env->CallVoidMethod(thiz, postTokenId, jPiece);
    env->DeleteLocalRef(jPiece);

    batch = llama_batch_get_one(&new_token_id, 1);
    if (llama_decode(g_ctx, batch)) {
      LOGE("Failed to decode token");
      break;
    }

    n_cur += 1;
  }

  LOGI("Completion generated successfully.");
  return env->NewStringUTF(final_response.c_str());
}
