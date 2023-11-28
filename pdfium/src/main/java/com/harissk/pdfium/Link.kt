package com.harissk.pdfium

import android.graphics.RectF

/**
 * Created by Harishkumar on 26/11/23.
 */

data class Link(
    var bounds: RectF? = null,
    var destPageIdx: Int? = null,
    var uri: String? = null,
)