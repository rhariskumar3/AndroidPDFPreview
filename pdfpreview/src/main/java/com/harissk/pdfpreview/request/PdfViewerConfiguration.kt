package com.harissk.pdfpreview.request

/**
 * Copyright [2025] [Haris Kumar R](https://github.com/rhariskumar3)
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
 * @param isDebugEnabled           Whether debug mode is enabled for logging.
 * @param thumbnailQuality       The quality of thumbnails (between 0 and 1), with a default of 0.7.
 * @param renderTileSize    The size of rendered parts in pixels, with a default of 512.
 * @param preloadMarginDp       The number of pages to preload above and below the currently displayed
 * page, in dp.
 * @param maxCachedBitmaps           The size of the main bitmap cache, which is the number of bitmaps to be
 * kept in memory.
 * @param maxCachedPages  The maximum number of pages kept in view.
 * @param maxCachedThumbnails The size of the thumbnail bitmap cache.
 * @param minZoom    The minimum zoom level allowed when pinching.
 * @param maxZoom    The maximum zoom level allowed when pinching.
 */
data class PdfViewerConfiguration(
    /**
     * Enable debug mode to see logs.
     */
    val isDebugEnabled: Boolean = false,
    /**
     * Between 0 and 1, the thumbnails quality (default 0.7).
     * Increasing this value may cause performance decrease.
     */
    val thumbnailQuality: Float = 0.7f,
    /**
     * The size of the rendered parts (default 512 for better quality).
     * Tinier : a little bit slower to have the whole page rendered but more reactive.
     * Bigger : user will have to wait longer to have the first visual results
     */
    val renderTileSize: Float = 512f,
    /**
     * Part of the document above and below the screen that should be preloaded, in dp.
     */
    val preloadMarginDp: Float = 20F,
    /**
     * The size of the cache (number of bitmaps kept).
     * Increased to 64 to support better caching at high zoom levels (3-4×)
     * and reduce cache thrashing during multi-page viewing.
     */
    val maxCachedBitmaps: Int = 64,
    /**
     * Max pages kept in View
     * Increasing this value may cause performance decrease
     */
    val maxCachedPages: Int = 3,
    /**
     * The size of the thumbnails cache (number of bitmaps kept).
     */
    val maxCachedThumbnails: Int = 4,
    /**
     * Minimum zoom level.
     */
    val minZoom: Float = 1f,
    /**
     * Maximum zoom level.
     */
    val maxZoom: Float = 5f,
) {
    companion object {
        val DEFAULT: PdfViewerConfiguration = PdfViewerConfiguration()
    }
}