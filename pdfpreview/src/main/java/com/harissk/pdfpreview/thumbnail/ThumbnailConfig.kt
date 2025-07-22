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

package com.harissk.pdfpreview.thumbnail

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Configuration class for PDF thumbnail generation.
 *
 * @param width The width of the thumbnail in pixels. Default is 200.
 * @param height The height of the thumbnail in pixels. Default is 200.
 * @param quality The bitmap configuration for quality vs memory trade-off. Default is RGB_565.
 * @param annotationRendering Whether to render annotations in the thumbnail. Default is false.
 * @param aspectRatio How to handle the aspect ratio of the original page. Default is PRESERVE.
 * @param backgroundColor Background color for transparent areas. Default is white.
 */
data class ThumbnailConfig(
    val width: Int = 200,
    val height: Int = 200,
    val quality: Bitmap.Config = Bitmap.Config.RGB_565,
    val annotationRendering: Boolean = false,
    val aspectRatio: AspectRatio = AspectRatio.PRESERVE,
    val backgroundColor: Int = Color.WHITE,
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
    }
}

/**
 * Defines how the thumbnail should handle the aspect ratio of the original PDF page.
 */
enum class AspectRatio {
    /**
     * Preserve the original aspect ratio of the PDF page, potentially leaving empty space.
     */
    PRESERVE,

    /**
     * Stretch the content to fill the entire thumbnail dimensions.
     */
    STRETCH,

    /**
     * Scale to fit width, maintaining aspect ratio.
     */
    FIT_WIDTH,

    /**
     * Scale to fit height, maintaining aspect ratio.
     */
    FIT_HEIGHT,

    /**
     * Scale to fit the larger dimension, maintaining aspect ratio (crop if necessary).
     */
    CROP_TO_FIT
}
