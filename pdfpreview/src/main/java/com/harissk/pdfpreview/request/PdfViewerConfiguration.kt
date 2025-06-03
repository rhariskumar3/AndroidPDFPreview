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
 * @param isDebugEnabled           Whether debug mode is enabled for logging.
 * @param thumbnailQuality       The quality of thumbnails (between 0 and 1), with a default of 0.7.
 * @param renderTileSize    The size of rendered parts in pixels, with a default of 256.
 * @param preloadMarginDp       The number of pages to preload above and below the currently displayed
 * page, in dp.
 * @param maxCacheSizeBytes           The maximum memory size of the main bitmap cache in bytes.
 * @param maxCachedPages  The maximum number of pages kept in view.
 * @param maxThumbnailCacheSizeBytes The maximum memory size of the thumbnails cache in bytes.
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
     * The size of the rendered parts (default 256).
     * Tinier : a little bit slower to have the whole page rendered but more reactive.
     * Bigger : user will have to wait longer to have the first visual results
     */
    val renderTileSize: Float = 256f,
    /**
     * Part of the document above and below the screen that should be preloaded, in dp.
     */
    val preloadMarginDp: Float = 20F,
    /**
     * The maximum memory size of the main bitmap cache in bytes.
     */
    val maxCacheSizeBytes: Long = 16 * 1024 * 1024L, // 16MB
    /**
     * Max pages kept in View
     * Increasing this value may cause performance decrease
     */
    val maxCachedPages: Int = 3,
    /**
     * The maximum memory size of the thumbnails cache in bytes.
     */
    val maxThumbnailCacheSizeBytes: Long = 4 * 1024 * 1024L, // 4MB
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