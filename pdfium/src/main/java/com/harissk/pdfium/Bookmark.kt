package com.harissk.pdfium

/**
 * Created by Harishkumar on 26/11/23.
 */

data class Bookmark(
    val title: String,
    val pageIdx: Long = 0,
    val children: ArrayList<Bookmark> = arrayListOf(),
    val mNativePtr: Long = -1,
) {
    val hasChildren
        get() = children.isNotEmpty()
}