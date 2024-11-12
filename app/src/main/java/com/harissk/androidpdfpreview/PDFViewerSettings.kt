package com.harissk.androidpdfpreview

data class PDFViewerSettings(
    val defaultPage: Int = 0,
    val swipeHorizontal: Boolean = false,
    val enableAnnotationRendering: Boolean = true,
)
