package com.harissk.pdfium

/**
 * Represents a bookmark (outline) in a PDF document.
 *
 * @param title The title of the bookmark.
 * @param pageIdx The page index of the bookmark.
 * @param children The list of child bookmarks.
 * @param mNativePtr The native pointer to the bookmark object (optional).
 */
data class Bookmark(
    val title: String,
    val pageIdx: Long = 0,
    val children: ArrayList<Bookmark> = arrayListOf(),
    val mNativePtr: Long = -1,
) {
    /**
     * Returns whether this bookmark has child bookmarks.
     */
    val hasChildren: Boolean
        get() = children.isNotEmpty()
}