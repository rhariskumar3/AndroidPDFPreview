package com.harissk.pdfpreview.request

/**
 * Created by Harishkumar on 05/12/23.
 */
data class RenderOptions(
    /**
     * Enable debug mode to see logs
     */
    val debugMode: Boolean = false,
    /** Between 0 and 1, the thumbnails quality (default 0.7).
     * Increasing this value may cause performance decrease
     */
    val thumbnailRatio: Float = 0.7f,
    /**
     * The size of the rendered parts (default 256)
     * Tinier : a little bit slower to have the whole page rendered but more reactive.
     * Bigger : user will have to wait longer to have the first visual results
     */
    val renderedPartSize: Float = 256f,
    /** Part of document above and below screen that should be preloaded, in dp  */
    val preloadOffset: Float = 20F,
    /** The size of the cache (number of bitmaps kept)  */
    val cacheSize: Int = 120,
    /** Max pages kept in View
     * Increasing this value may cause performance decrease
     */
    val maxPageCacheSize: Int = 10,
    /** The size of the thumbnails cache (number of bitmaps kept)  */
    val thumbnailsCacheSize: Int = 8,
    /** Minimum zoom level  */
    val pinchMinimumZoom: Float = 1f,
    /** Maximum zoom level  */
    val pinchMaximumZoom: Float = 10f,
) {
    companion object {
        val DEFAULT = RenderOptions()
    }
}
