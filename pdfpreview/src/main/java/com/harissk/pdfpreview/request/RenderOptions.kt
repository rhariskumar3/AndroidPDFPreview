package com.harissk.pdfpreview.request

/**
 * Copyright [2024] [Haris Kumar R](https://github.com/rhariskumar3)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * */

/**
 * Class to hold rendering options for a PDF previewer.
 *
 * @param debugMode           Whether debug mode is enabled for logging.
 * @param thumbnailRatio       The quality of thumbnails (between 0 and 1), with a default of 0.7.
 * @param renderedPartSize    The size of rendered parts in pixels, with a default of 256.
 * @param preloadOffset       The number of pages to preload above and below the currently displayed
 * page, in dp.
 * @param cacheSize           The size of the main bitmap cache, which is the number of bitmaps to be
 * kept in memory.
 * @param maxPageCacheSize  The maximum number of pages kept in view.
 * @param thumbnailsCacheSize The size of the thumbnail bitmap cache.
 * @param pinchMinimumZoom    The minimum zoom level allowed when pinching.
 * @param pinchMaximumZoom    The maximum zoom level allowed when pinching.
 */
data class RenderOptions(
    /**
     * Enable debug mode to see logs.
     */
    val debugMode: Boolean = false,
    /**
     * Between 0 and 1, the thumbnails quality (default 0.7).
     * Increasing this value may cause performance decrease.
     */
    val thumbnailRatio: Float = 0.7f,
    /**
     * The size of the rendered parts (default 256).
     * Tinier : a little bit slower to have the whole page rendered but more reactive.
     * Bigger : user will have to wait longer to have the first visual results
     */
    val renderedPartSize: Float = 256f,
    /**
     * Part of the document above and below the screen that should be preloaded, in dp.
     */
    val preloadOffset: Float = 20F,
    /**
     * The size of the cache (number of bitmaps kept).
     */
    val cacheSize: Int = 120,
    /**
     * Max pages kept in View
     * Increasing this value may cause performance decrease
     */
    val maxPageCacheSize: Int = 10,
    /**
     * The size of the thumbnails cache (number of bitmaps kept).
     */
    val thumbnailsCacheSize: Int = 8,
    /**
     * Minimum zoom level.
     */
    val pinchMinimumZoom: Float = 1f,
    /**
     * Maximum zoom level.
     */
    val pinchMaximumZoom: Float = 10f,
) {
    companion object {
        val DEFAULT: RenderOptions = RenderOptions()
    }
}