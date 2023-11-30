package com.harissk.pdfpreview.model

import android.graphics.Bitmap
import android.graphics.RectF


/**
 * Created by Harishkumar on 25/11/23.
 */
data class PagePart(
    val page: Int,
    val renderedBitmap: Bitmap?,
    val pageRelativeBounds: RectF,
    val isThumbnail: Boolean,
    var cacheOrder: Int,
)