package com.harissk.pdfpreview.utils

/**
 * Created by Harishkumar on 25/11/23.
 */

internal object Constants {
    var DEBUG_MODE = false

    /** Between 0 and 1, the thumbnails quality (default 0.7). Increasing this value may cause performance decrease  */
    var THUMBNAIL_RATIO = 0.7f

    /**
     * The size of the rendered parts (default 256)
     * Tinier : a little bit slower to have the whole page rendered but more reactive.
     * Bigger : user will have to wait longer to have the first visual results
     */
    var PART_SIZE = 256f

    /** Part of document above and below screen that should be preloaded, in dp  */
    var PRELOAD_OFFSET = 20F

    object Cache {
        /** The size of the cache (number of bitmaps kept)  */
        var CACHE_SIZE = 120
        var THUMBNAILS_CACHE_SIZE = 8
    }

    object Pinch {
        var MAXIMUM_ZOOM = 10f
        var MINIMUM_ZOOM = 1f
    }
}