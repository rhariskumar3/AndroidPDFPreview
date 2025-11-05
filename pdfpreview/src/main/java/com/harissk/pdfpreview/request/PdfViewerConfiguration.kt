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
 * This configuration controls various aspects of PDF rendering performance and quality:
 * - Caching limits for memory management
 * - Rendering quality and tile sizes
 * - Preloading behavior for smooth scrolling
 * - Zoom limits for user interaction
 * - Debug features for development
 *
 * All parameters are actively used throughout the PDF rendering pipeline.
 *
 * @param enableDebugMode           Whether debug mode is enabled for logging and visual debug overlays.
 * @param thumbnailRenderingQuality       The quality of thumbnails (between 0 and 1), affects thumbnail rendering size.
 * @param tileSize    The base size of rendered parts (tiles) in pixels, scales with zoom for quality.
 * @param offscreenPreloadMarginDp       The margin in dp around visible area for preloading content.
 * @param renderedTileCacheCapacity           The maximum number of rendered bitmaps to cache in memory.
 * @param openPdfPageCapacity  The maximum number of pages to keep opened in Pdfium (affects memory usage).
 * @param thumbnailCacheCapacity The maximum number of thumbnail bitmaps to cache.
 * @param concurrentPageRenderingLimit The maximum number of page ranges to process during loading.
 * @param tileRenderingBatchSize The maximum number of page parts (tiles) to load per rendering call.
 * @param minimumAllowedZoomLevel    The minimum zoom level allowed during pinch-to-zoom gestures.
 * @param maximumAllowedZoomLevel    The maximum zoom level allowed during pinch-to-zoom gestures.
 */
data class PdfViewerConfiguration(
    /**
     * Enable debug mode to see logs and visual debug overlays on rendered pages.
     */
    val enableDebugMode: Boolean = false,
    /**
     * Between 0 and 1, the quality multiplier for thumbnail rendering.
     * Higher values produce clearer thumbnails but use more memory.
     */
    val thumbnailRenderingQuality: Float = 0.7f,
    /**
     * The base size of page tiles in pixels.
     * Larger tiles provide better quality but use more memory and render slower.
     * This value automatically scales with zoom level to maintain quality.
     */
    val tileSize: Float = 512f,
    /**
     * Distance in dp around the visible area to preload content.
     * Increases memory usage but improves scrolling performance.
     */
    val offscreenPreloadMarginDp: Float = 20F,
    /**
     * The maximum number of rendered bitmaps to keep in memory.
     * Higher values improve performance but increase memory usage.
     */
    val renderedTileCacheCapacity: Int = 64,
    /**
     * Maximum number of pages to keep opened in the native PDF library.
     * Limits memory usage for large documents with many pages.
     */
    val openPdfPageCapacity: Int = 3,
    /**
     * The maximum number of thumbnail bitmaps to cache in memory.
     */
    val thumbnailCacheCapacity: Int = 4,
    /**
     * The maximum number of page ranges to process during loading.
     * Limits how many pages are considered for rendering at once to prevent overload.
     */
    val concurrentPageRenderingLimit: Int = 3,
    /**
     * The maximum number of page parts (tiles) to load per rendering call.
     * Prevents queue overload and ensures smooth prioritized loading.
     */
    val tileRenderingBatchSize: Int = 40,
    /**
     * Minimum zoom level allowed during pinch-to-zoom gestures.
     * Coerced with the view's default minimum zoom.
     */
    val minimumAllowedZoomLevel: Float = 1f,
    /**
     * Maximum zoom level allowed during pinch-to-zoom gestures.
     * Coerced with the view's default maximum zoom.
     */
    val maximumAllowedZoomLevel: Float = 5f,
) {
    companion object {
        val DEFAULT: PdfViewerConfiguration = PdfViewerConfiguration()
    }
}