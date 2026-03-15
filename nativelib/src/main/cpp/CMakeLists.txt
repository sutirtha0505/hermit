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
set(GGML_VULKAN OFF CACHE BOOL "Disable Vulkan" FORCE)

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
