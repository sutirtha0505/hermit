cmake_minimum_required(VERSION 3.22.1)

project("nativelib")

set(CMAKE_CXX_STANDARD 17)

find_library(log-lib log)

# Add llama.cpp as a subdirectory if the user clones it there
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# We just want the llama library, not tests or executables
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Build llama tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Build llama examples" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Build llama server" FORCE)

# Fix ARM32/ARM64 Android build issues in ggml
set(GGML_CPU_ARM_EXTRA OFF CACHE BOOL "Disable extra ARM" FORCE)
set(GGML_USE_CPU_HBM OFF CACHE BOOL "Disable CPU HBM" FORCE)
set(GGML_USE_LLAMAFILE OFF CACHE BOOL "Disable llamafile" FORCE)
set(GGML_USE_CPU_REPACK OFF CACHE BOOL "Disable CPU repack" FORCE)
set(GGML_VULKAN ON CACHE BOOL "Enable Vulkan" FORCE)

# Add Vulkan Headers from the local project directory
set(VULKAN_HEADERS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/Vulkan-Headers)
include_directories(SYSTEM ${VULKAN_HEADERS_DIR}/include)
set(Vulkan_INCLUDE_DIR ${VULKAN_HEADERS_DIR}/include CACHE PATH "Vulkan include dir" FORCE)

# Point to the correct glslc executable in the Android NDK
set(Vulkan_GLSLC_EXECUTABLE "/Volumes/Crucial_NVME_M.2/Android_SDK/ndk/28.2.13676358/shader-tools/darwin-x86_64/glslc" CACHE FILEPATH "glslc executable" FORCE)

# Explicitly disable extensions that NDK glslc doesn't support but might be misdetected
set(GGML_VULKAN_COOPMAT_GLSLC_SUPPORT OFF CACHE BOOL "Disable coopmat" FORCE)
set(GGML_VULKAN_COOPMAT2_GLSLC_SUPPORT OFF CACHE BOOL "Disable coopmat2" FORCE)

add_subdirectory(${LLAMA_DIR})

add_library(${CMAKE_PROJECT_NAME} SHARED
    nativelib.cpp)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/ggml/include
)

target_link_libraries(${CMAKE_PROJECT_NAME}
    llama
    android
    ${log-lib}
)
