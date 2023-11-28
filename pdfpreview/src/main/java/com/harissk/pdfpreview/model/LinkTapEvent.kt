package com.harissk.pdfpreview.model

import android.graphics.RectF
import com.harissk.pdfium.Link

/**
 * Created by Harishkumar on 25/11/23.
 */
data class LinkTapEvent(
    val originalX: Float,
    val originalY: Float,
    val documentX: Float,
    val documentY: Float,
    val mappedLinkRect: RectF,
    val link: Link,
)
