/*
 * Copyright (C) 2025 [Haris Kumar R](https://github.com/rhariskumar3)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.harissk.androidpdfpreview.presentation.model

/**
 * PDF viewer configuration settings
 */
internal data class ViewerSettings(
    val defaultPage: Int = 0,
    val swipeHorizontal: Boolean = false,
    val enableAnnotationRendering: Boolean = true,
    val spacing: Float = 10f,
    val singlePageMode: Boolean = false,
    val highMemoryMode: Boolean = false, // Enable high-memory configuration to prevent empty pages
    val enableNightMode: Boolean = false, // Enable night mode for better readability in low light
    val enableDoubleTapZoom: Boolean = true, // Enable double-tap to zoom functionality
    val enableAntialiasing: Boolean = true, // Enable anti-aliasing for smoother rendering
    val automaticPageSpacing: Boolean = false, // Automatically adjust spacing based on screen size
)