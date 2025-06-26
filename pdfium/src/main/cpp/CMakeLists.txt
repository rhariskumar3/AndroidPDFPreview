# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

project("pdfium")

# Specify where JPEGTurbo libraries are located
set(Pdfium_DIR ${CMAKE_SOURCE_DIR}/pdfium)

# shared lib will also be tucked into APK and sent to target
# refer to app/build.gradle, jniLibs section for that purpose.
# ${ANDROID_ABI} is handy for our purpose here. Probably this ${ANDROID_ABI} is
# the most valuable thing of this sample, the rest are pretty much normal cmake
add_library(pdfium SHARED IMPORTED)
set_target_properties(pdfium PROPERTIES IMPORTED_LOCATION
        ${Pdfium_DIR}/lib/${ANDROID_ABI}/libpdfium.so)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        pdfium_jni

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        pdfium_jni.cpp)

target_include_directories(pdfium_jni PRIVATE
        ${CMAKE_SOURCE_DIR}/utils
        ${Pdfium_DIR}/include
        ${Pdfium_DIR}/include/cpp)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        pdfium_jni
        pdfium
        jnigraphics
        android
        GLESv2
        log)

# --- IMPORTANT: Add these flags for 16KB page size support ---
# These linker flags ensure that the output shared library (pdfium_jni.so)
# is built with segments aligned to 16KB, which is required for Android 15+
# devices that adopt the larger page size. This applies to the library being
# built by this CMakeLists.txt.

# Log the detected ANDROID_PLATFORM_LEVEL for debugging purposes.
message(STATUS "ANDROID_PLATFORM_LEVEL detected: ${ANDROID_PLATFORM_LEVEL}")

# Apply 16KB page size linker options and --no-rosegment directly to pdfium_jni
# as compileSdk is 35 and we intend to support 16KB page sizes.
message(STATUS "Applying 16KB page size linker options and --no-rosegment to pdfium_jni.")
target_link_options(pdfium_jni PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
        "-Wl,--no-rosegment"  # Added for 16KB page size support
)

# Define a preprocessor macro that can be used in C/C++ code
# to conditionally compile page-size-aware logic.
target_compile_definitions(pdfium_jni PRIVATE ANDROID_16KB_PAGE_SIZE=1)

# Custom command to verify the build result:
# Custom command to verify the build result (Windows-compatible)
add_custom_command(TARGET pdfium_jni POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E echo "=== Build completed for $<TARGET_FILE_NAME:pdfium_jni> ==="
        COMMAND ${CMAKE_COMMAND} -E echo "Library built with 16KB page size flags applied"
        COMMENT "Build verification"
)
