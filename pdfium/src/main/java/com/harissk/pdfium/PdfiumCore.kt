package com.harissk.pdfium

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.ParcelFileDescriptor
import android.util.ArrayMap
import android.view.Surface
import com.harissk.pdfium.exception.PageRenderingException
import com.harissk.pdfium.listener.LogWriter
import com.harissk.pdfium.search.FPDFTextSearchContext
import com.harissk.pdfium.search.TextSearchContext
import com.harissk.pdfium.util.FileUtils
import com.harissk.pdfium.util.Size
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Core class for interacting with the Pdfium library.
 *
 * This class provides a Java interface to the native Pdfium library, allowing
 * you to open, read, render, and interact with PDF documents.
 *
 * **Key functionalities:**
 * - Opening PDF documents from files or byte arrays.
 * - Retrieving document metadata, such as title, author, and page count.
 * - Accessing and navigating through the document's table of contents (bookmarks).
 * - Rendering pages to surfaces or bitmaps for display.
 * - Extracting text from pages, including character positions and bounding boxes.
 * - Searching for text within pages.
 * - Handling annotations (limited functionality).
 * - Managing native resources and closing documents.
 *
 * **Usage:**
 * 1. Create a new `PdfiumCore` instance.
 * 2. Open a PDF document using `newDocument()`.
 * 3. Perform desired operations, such as rendering pages or extracting text.
 * 4. Close the document using `closeDocument()` to release native resources.
 *
 * **Example:**
 * ```java
 * PdfiumCore pdfiumCore = new PdfiumCore();
 * try (ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
 *     pdfiumCore.newDocument(fd);
 *     int pageCount = pdfiumCore.getPageCount();
 *     // ... perform operations on the document ...
 * } finally {
 *     pdfiumCore.closeDocument();
 * }
 * ```
 *
 * **Note:** This class interacts with native code through JNI. Ensure that the
 * necessary native libraries (pdfium and pdfium_jni) are loaded before using
 * this class.
 */
class PdfiumCore : java.io.Closeable {

    private var mCurrentDpi: Int = 72 // pdfium has default dpi set to 72
    private val mNativePagesPtr: MutableMap<Int, Long> = ArrayMap()
    private val mNativeTextPagesPtr: MutableMap<Int, Long> = ArrayMap()
    private val mNativeSearchHandlePtr: MutableMap<Int, Long> = ArrayMap()
    private var mNativeDocPtr: Long = 0
    private var mFileDescriptor: ParcelFileDescriptor? = null

    // Native methods
    private external fun nativeOpenDocument(fd: Int, password: String?): Long
    private external fun nativeOpenMemDocument(data: ByteArray, password: String?): Long
    private external fun nativeCloseDocument(docPtr: Long)
    private external fun nativeGetPageCount(docPtr: Long): Int
    private external fun nativeLoadPage(docPtr: Long, pageIndex: Int): Long
    private external fun nativeLoadPages(docPtr: Long, fromIndex: Int, toIndex: Int): LongArray
    private external fun nativeClosePage(pagePtr: Long)
    private external fun nativeClosePages(pagesPtr: LongArray)
    private external fun nativeGetPageWidthPixel(pagePtr: Long, dpi: Int): Int
    private external fun nativeGetPageHeightPixel(pagePtr: Long, dpi: Int): Int
    private external fun nativeGetPageWidthPoint(pagePtr: Long): Int
    private external fun nativeGetPageHeightPoint(pagePtr: Long): Int
    private external fun nativeGetPageRotation(pagePtr: Long): Int
    private external fun nativeRenderPage(
        pagePtr: Long, surface: Surface,
        startX: Int, startY: Int,
        drawSizeHor: Int, drawSizeVer: Int,
        renderAnnot: Boolean,
    )

    private external fun nativeRenderPageBitmap(
        pagePtr: Long, bitmap: Bitmap,
        startX: Int, startY: Int,
        drawSizeHor: Int, drawSizeVer: Int,
        renderAnnot: Boolean,
    )

    private external fun nativeGetDocumentMetaText(docPtr: Long, tag: String): String?
    private external fun nativeGetFirstChildBookmark(docPtr: Long, bookmarkPtr: Long?): Long?
    private external fun nativeGetSiblingBookmark(docPtr: Long, bookmarkPtr: Long): Long?
    private external fun nativeGetBookmarkTitle(bookmarkPtr: Long): String?
    private external fun nativeGetBookmarkDestIndex(docPtr: Long, bookmarkPtr: Long): Long
    private external fun nativeGetPageSizeByIndex(docPtr: Long, pageIndex: Int, dpi: Int): Size
    private external fun nativeGetPageLinks(pagePtr: Long): LongArray
    private external fun nativeGetDestPageIndex(docPtr: Long, linkPtr: Long): Int?
    private external fun nativeGetLinkURI(docPtr: Long, linkPtr: Long): String?
    private external fun nativeGetLinkRect(linkPtr: Long): RectF?
    private external fun nativePageCoordinateToDevice(
        pagePtr: Long, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, pageX: Double, pageY: Double,
    ): Point

    private external fun nativeDeviceCoordinateToPage(
        pagePtr: Long, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, deviceX: Int, deviceY: Int,
    ): PointF

    ///////////////////////////////////////
    // PDF TextPage api
    ///////////
    private external fun nativeLoadTextPage(docPtr: Long, pagePtr: Long): Long
    private external fun nativeLoadTextPages(docPtr: Long, pagePtrs: LongArray): LongArray
    private external fun nativeCloseTextPage(pagePtr: Long)
    private external fun nativeCloseTextPages(pagesPtr: LongArray)
    private external fun nativeTextCountChars(textPagePtr: Long): Int
    private external fun nativeTextGetText(
        textPagePtr: Long,
        start_index: Int,
        count: Int,
        result: ShortArray,
    ): Int

    private external fun nativeTextGetUnicode(textPagePtr: Long, index: Int): Int
    private external fun nativeTextGetCharBox(textPagePtr: Long, index: Int): DoubleArray
    private external fun nativeTextGetCharIndexAtPos(
        textPagePtr: Long,
        x: Double,
        y: Double,
        xTolerance: Double,
        yTolerance: Double,
    ): Int

    private external fun nativeTextCountRects(textPagePtr: Long, start_index: Int, count: Int): Int
    private external fun nativeTextGetRect(textPagePtr: Long, rect_index: Int): DoubleArray
    private external fun nativeTextGetBoundedTextLength(
        textPagePtr: Long,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
    ): Int

    private external fun nativeTextGetBoundedText(
        textPagePtr: Long,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        arr: ShortArray,
    ): Int

    ///////////////////////////////////////
    // PDF Search API
    ///////////
    private external fun nativeSearchStart(
        textPagePtr: Long,
        query: String,
        matchCase: Boolean,
        matchWholeWord: Boolean,
    ): Long

    private external fun nativeSearchStop(searchHandlePtr: Long)
    private external fun nativeSearchNext(searchHandlePtr: Long): Boolean
    private external fun nativeSearchPrev(searchHandlePtr: Long): Boolean
    private external fun nativeGetCharIndexOfSearchResult(searchHandlePtr: Long): Int
    private external fun nativeCountSearchResult(searchHandlePtr: Long): Int

    ///////////////////////////////////////
    // PDF Annotation API
    ///////////
    private external fun nativeAddTextAnnotation(
        docPtr: Long,
        pageIndex: Int,
        text: String,
        color: IntArray,
        bound: IntArray,
    ): Long

    ///////////////////////////////////////
    // PDF Native Callbacks
    ///////////
    private fun onAnnotationAdded(pageIndex: Int, pageNewPtr: Long) {}
    private fun onAnnotationUpdated(pageIndex: Int, pageNewPtr: Long) {}
    private fun onAnnotationRemoved(pageIndex: Int, pageNewPtr: Long) {}

    private external fun nativeGetLastError(docPtr: Long): Int
    private external fun nativeGetErrorMessage(errorCode: Int): String

    ///////////////////////////////////////
    // PDF SDK functions
    ///////////
    /**
     * Create new document from file
     */
    @Throws(IOException::class)
    fun newDocument(fd: ParcelFileDescriptor) {
        newDocument(fd, null)
    }

    /**
     * Create new document from file with password
     */
    @Synchronized
    @Throws(IOException::class)
    fun newDocument(fd: ParcelFileDescriptor, password: String?): Long {
        mFileDescriptor = fd
        val numFd: Int = FileUtils.getNumFd(fd)

        val docPtr = nativeOpenDocument(numFd, password)
        if (!isValidPointer(docPtr)) {
            val errorCode: Int = nativeGetLastError(mNativeDocPtr)
            val errorMessage: String = nativeGetErrorMessage(errorCode)
            closeDocument()
            throw IOException("Error opening PDF document. Code: $errorCode, Message: $errorMessage")
        }
        mNativeDocPtr = docPtr
        return docPtr
    }

    /**
     * Create new document from file with password
     */
    @Synchronized
    @Throws(IOException::class)
    fun newDocument(bytes: ByteArray, password: String?): Long {
        val docPtr = nativeOpenMemDocument(bytes, password)
        if (!isValidPointer(docPtr)) {
            val errorCode: Int = nativeGetLastError(mNativeDocPtr)
            val errorMessage: String = nativeGetErrorMessage(errorCode)
            closeDocument()
            throw IOException("Error opening PDF document. Code: $errorCode, Message: $errorMessage")
        }
        mNativeDocPtr = docPtr
        return docPtr
    }

    /**
     * Get total number of pages in document
     */
    val pageCount: Int
        get() = nativeGetPageCount(mNativeDocPtr)

    /**
     * Open page and store native pointer
     */
    @Throws(PageRenderingException::class)
    fun openPage(pageIndex: Int): Long {
        val pagePtr: Long = nativeLoadPage(mNativeDocPtr, pageIndex)

        if (!isValidPointer(pagePtr)) {
            val errorCode: Int = nativeGetLastError(pagePtr)
            val errorMessage: String = nativeGetErrorMessage(errorCode)
            throw PageRenderingException(
                page = pageIndex,
                throwable = Throwable(message = "Error loading page. Code: $errorCode, Message: $errorMessage")
            )
        }

        mNativePagesPtr[pageIndex] = pagePtr
        prepareTextInfo(pageIndex)
        return pagePtr
    }

    /**
     * Open range of pages and store native pointers
     */
    fun openPage(fromIndex: Int, toIndex: Int): LongArray {
        val pagesPtr: LongArray = nativeLoadPages(mNativeDocPtr, fromIndex, toIndex)
        var pageIndex = fromIndex
        for (page in pagesPtr) {
            if (pageIndex > toIndex) break
            mNativePagesPtr[pageIndex] = page
            pageIndex++
            prepareTextInfo(pageIndex)
        }
        return pagesPtr
    }

    /**
     * Get page width in pixels. <br></br>
     * This method requires page to be opened.
     */
    fun getPageWidth(index: Int): Int =
        mNativePagesPtr[index]?.let { nativeGetPageWidthPixel(it, mCurrentDpi) } ?: 0

    /**
     * Get page height in pixels. <br></br>
     * This method requires page to be opened.
     */
    fun getPageHeight(index: Int): Int =
        mNativePagesPtr[index]?.let { nativeGetPageHeightPixel(it, mCurrentDpi) } ?: 0

    /**
     * Get page width in PostScript points (1/72th of an inch).<br></br>
     * This method requires page to be opened.
     */
    fun getPageWidthPoint(index: Int): Int =
        mNativePagesPtr[index]?.let { nativeGetPageWidthPoint(it) } ?: 0

    /**
     * Get page height in PostScript points (1/72th of an inch).<br></br>
     * This method requires page to be opened.
     */
    fun getPageHeightPoint(index: Int): Int =
        mNativePagesPtr[index]?.let { nativeGetPageHeightPoint(it) } ?: 0

    /**
     * Get size of page in pixels.<br></br>
     * This method does not require given page to be opened.
     */
    fun getPageSize(index: Int): Size = nativeGetPageSizeByIndex(mNativeDocPtr, index, mCurrentDpi)

    /**
     * Get the rotation of page<br></br>
     */
    fun getPageRotation(index: Int): Int =
        mNativePagesPtr[index]?.let { nativeGetPageRotation(it) } ?: 0

    /**
     * Render page fragment on [Surface]. This method allows to render annotations.<br></br>
     * Page must be opened before rendering.
     */
    /**
     * Render page fragment on [Surface].<br></br>
     * Page must be opened before rendering.
     */
    fun renderPage(
        surface: Surface, pageIndex: Int,
        startX: Int, startY: Int, drawSizeX: Int, drawSizeY: Int,
        renderAnnot: Boolean = false,
    ) {
        try {
            //nativeRenderPage(mNativePagesPtr.get(pageIndex), surface, mCurrentDpi);
            nativeRenderPage(
                mNativePagesPtr[pageIndex] ?: throw NullPointerException(), surface,
                startX, startY, drawSizeX, drawSizeY, renderAnnot
            )
        } catch (e: NullPointerException) {
            logWriter?.writeLog("mContext may be null", TAG)
        } catch (e: Exception) {
            logWriter?.writeLog("Exception throw from native", TAG)
        }
    }

    /**
     * Render page fragment on [Bitmap]. This method allows to render annotations.<br></br>
     * Page must be opened before rendering.
     *
     *
     * For more info see [PdfiumCore.renderPageBitmap]
     */
    fun renderPageBitmap(
        bitmap: Bitmap, pageIndex: Int,
        startX: Int, startY: Int, drawSizeX: Int, drawSizeY: Int,
        renderAnnot: Boolean = false,
    ) {
        try {
            nativeRenderPageBitmap(
                mNativePagesPtr[pageIndex] ?: throw NullPointerException(), bitmap,
                startX, startY, drawSizeX, drawSizeY, renderAnnot
            )
        } catch (e: NullPointerException) {
            logWriter?.writeLog("mContext may be null", TAG)
        } catch (e: Exception) {
            logWriter?.writeLog("Exception throw from native", TAG)
        }
    }

    /**
     * Release native page resources of given page
     */
    fun closePage(pageIndex: Int) {
        val pagePtr = mNativePagesPtr[pageIndex] ?: throw NullPointerException()
        nativeClosePage(pagePtr)
        mNativePagesPtr.remove(pageIndex)
    }

    /**
     * Gets the last occurred error code. Should be called after a native function has failed.
     * @param docPtr The document pointer.
     * @return The last error code, of 0 if no error occurred.
     */
    @Synchronized
    fun getLastError(docPtr: Long): Int = nativeGetLastError(docPtr)


    /**
     * Gets the error message from a specific error code.
     * @param errorCode The error code.
     * @return The corresponding error message.
     */
    @Synchronized
    fun getErrorMessage(errorCode: Int): String = nativeGetErrorMessage(errorCode)

    /**
     * Closes the PDF document and releases resources.
     */
    @Synchronized
    override fun close() {
        closeDocument()
    }


    /**
     * Release native resources and opened file
     */
    @Synchronized
    private fun closeDocument() = try {
        // Close all native pages
        mNativePagesPtr.values.forEach { pagePtr ->
            if (isValidPointer(pagePtr)) nativeClosePage(pagePtr)
        }

        // Close all native text pages
        mNativeTextPagesPtr.values.forEach { textPagePtr ->
            if (isValidPointer(textPagePtr)) nativeCloseTextPage(textPagePtr)
        }

        // Close all search handles
        mNativeSearchHandlePtr.values.forEach { searchHandle ->
            if (isValidPointer(searchHandle)) nativeSearchStop(searchHandle)
        }

        nativeCloseDocument(mNativeDocPtr)
    } finally {
        mNativePagesPtr.clear()
        mNativeTextPagesPtr.clear()
        mNativeSearchHandlePtr.clear()
        mNativeDocPtr = 0

        try {
            mFileDescriptor?.close()
        } catch (e: IOException) {
            logWriter?.writeLog(e)
        } finally {
            mFileDescriptor = null
        }
    }

    /**
     * Get metadata for given document
     */
    val documentMeta: Meta
        get() = Meta(
            title = nativeGetDocumentMetaText(mNativeDocPtr, "Title").orEmpty(),
            author = nativeGetDocumentMetaText(mNativeDocPtr, "Author").orEmpty(),
            subject = nativeGetDocumentMetaText(mNativeDocPtr, "Subject").orEmpty(),
            keywords = nativeGetDocumentMetaText(mNativeDocPtr, "Keywords").orEmpty(),
            creator = nativeGetDocumentMetaText(mNativeDocPtr, "Creator").orEmpty(),
            producer = nativeGetDocumentMetaText(mNativeDocPtr, "Producer").orEmpty(),
            creationDate = nativeGetDocumentMetaText(mNativeDocPtr, "CreationDate").orEmpty(),
            modDate = nativeGetDocumentMetaText(mNativeDocPtr, "ModDate").orEmpty()
        )

    /**
     * Get table of contents (bookmarks) for given document
     */
    fun getTableOfContents(): List<Bookmark> {
        val topLevel = arrayListOf<Bookmark>()
        nativeGetFirstChildBookmark(mNativeDocPtr, null)?.let { recursiveGetBookmark(topLevel, it) }
        return topLevel
    }

    private fun recursiveGetBookmark(tree: ArrayList<Bookmark>, bookmarkPtr: Long) {
        val bookmark = Bookmark(
            title = nativeGetBookmarkTitle(bookmarkPtr).orEmpty(),
            pageIdx = nativeGetBookmarkDestIndex(mNativeDocPtr, bookmarkPtr),
            mNativePtr = bookmarkPtr,
            children = ArrayList()
        )
        tree.add(bookmark)

        nativeGetFirstChildBookmark(mNativeDocPtr, bookmarkPtr)?.let { child ->
            recursiveGetBookmark(bookmark.children, child)
        }

        nativeGetSiblingBookmark(mNativeDocPtr, bookmarkPtr)?.let { sibling ->
            recursiveGetBookmark(tree, sibling)
        }
    }

    /**
     * Get all links from given page
     */
    fun getPageLinks(pageIndex: Int): List<Link> {
        if (pageIndex < 0) return emptyList()
        val links = mutableListOf<Link>()
        val nativePagePtr = mNativePagesPtr[pageIndex] ?: return links

        nativeGetPageLinks(nativePagePtr).forEach { linkPtr ->
            val index = nativeGetDestPageIndex(mNativeDocPtr, linkPtr)
            val uri = nativeGetLinkURI(mNativeDocPtr, linkPtr)
            val rect = nativeGetLinkRect(linkPtr) ?: return@forEach

            if (index != null || uri != null) links.add(Link(rect, index, uri))
        }
        return links
    }

    /**
     * Map page coordinates to device screen coordinates
     *
     * @param pageIndex index of page
     * @param startX    left pixel position of the display area in device coordinates
     * @param startY    top pixel position of the display area in device coordinates
     * @param sizeX     horizontal size (in pixels) for displaying the page
     * @param sizeY     vertical size (in pixels) for displaying the page
     * @param rotate    page orientation: 0 (normal), 1 (rotated 90 degrees clockwise),
     * 2 (rotated 180 degrees), 3 (rotated 90 degrees counter-clockwise)
     * @param pageX     X value in page coordinates
     * @param pageY     Y value in page coordinate
     * @return mapped coordinates
     */
    private fun mapPageCoordinatesToDevice(
        pageIndex: Int, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, pageX: Double, pageY: Double,
    ): Point {
        val pagePtr = mNativePagesPtr[pageIndex] ?: return Point()
        return nativePageCoordinateToDevice(
            pagePtr = pagePtr,
            startX = startX,
            startY = startY,
            sizeX = sizeX,
            sizeY = sizeY,
            rotate = rotate,
            pageX = pageX,
            pageY = pageY
        )
    }

    /**
     * Convert the screen coordinates of a point to page coordinates.
     *
     *
     * The page coordinate system has its origin at the left-bottom corner
     * of the page, with the X-axis on the bottom going to the right, and
     * the Y-axis on the left side going up.
     *
     *
     * NOTE: this coordinate system can be altered when you zoom, scroll,
     * or rotate a page, however, a point on the page should always have
     * the same coordinate values in the page coordinate system.
     *
     *
     * The device coordinate system is device dependent. For screen device,
     * its origin is at the left-top corner of the window. However this
     * origin can be altered by the Windows coordinate transformation
     * utilities.
     *
     *
     * You must make sure the start_x, start_y, size_x, size_y
     * and rotate parameters have exactly same values as you used in
     * the FPDF_RenderPage() function call.
     *
     * @param pageIndex index of page
     * @param startX    Left pixel position of the display area in device coordinates.
     * @param startY    Top pixel position of the display area in device coordinates.
     * @param sizeX     Horizontal size (in pixels) for displaying the page.
     * @param sizeY     Vertical size (in pixels) for displaying the page.
     * @param rotate    Page orientation:
     * 0 (normal)
     * 1 (rotated 90 degrees clockwise)
     * 2 (rotated 180 degrees)
     * 3 (rotated 90 degrees counter-clockwise)
     * @param deviceX   X value in device coordinates to be converted.
     * @param deviceY   Y value in device coordinates to be converted.
     */
    fun mapDeviceCoordinateToPage(
        pageIndex: Int, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, deviceX: Int, deviceY: Int,
    ): PointF {
        val pagePtr = mNativePagesPtr[pageIndex] ?: return PointF()
        return nativeDeviceCoordinateToPage(
            pagePtr = pagePtr,
            startX = startX,
            startY = startY,
            sizeX = sizeX,
            sizeY = sizeY,
            rotate = rotate,
            deviceX = deviceX,
            deviceY = deviceY
        )
    }

    /**
     * @return mapped coordinates
     */
    fun mapPageCoordinateToDevice(
        pageIndex: Int, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, coords: RectF,
    ): RectF {
        val leftTop = mapPageCoordinatesToDevice(
            pageIndex, startX, startY, sizeX, sizeY, rotate,
            coords.left.toDouble(), coords.top.toDouble()
        )
        val rightBottom = mapPageCoordinatesToDevice(
            pageIndex, startX, startY, sizeX, sizeY, rotate,
            coords.right.toDouble(), coords.bottom.toDouble()
        )
        return RectF(
            leftTop.x.toFloat(),
            leftTop.y.toFloat(),
            rightBottom.x.toFloat(),
            rightBottom.y.toFloat()
        )
    }
    ///////////////////////////////////////
    // FPDF_TEXTPAGE api
    ///////////
    /**
     * Prepare information about all characters in a page.
     * Application must call FPDFText_ClosePage to release the text page information.
     *
     * @param pageIndex index of page.
     * @return A handle to the text page information structure. NULL if something goes wrong.
     */
    fun prepareTextInfo(pageIndex: Int): Long {
        val pagePtr = mNativePagesPtr[pageIndex]
        if (!isValidPointer(pagePtr)) {
            throw IllegalStateException("Page at index $pageIndex not open. Ensure page is opened before preparing text info.")
        }
        val textPagePtr = nativeLoadTextPage(mNativeDocPtr, pagePtr!!) // pagePtr is checked for null by isValidPointer
        if (isValidPointer(textPagePtr)) {
            mNativeTextPagesPtr[pageIndex] = textPagePtr
        }
        return textPagePtr
    }

    /**
     * Release all resources allocated for a text page information structure.
     *
     * @param pageIndex index of page.
     */
    fun releaseTextInfo(pageIndex: Int) {
        val textPagePtr = mNativeTextPagesPtr[pageIndex]
        if (validPtr(textPagePtr)) nativeCloseTextPage(textPagePtr ?: 0)
    }

    /**
     * Prepare information about all characters in a range of pages.
     * Application must call FPDFText_ClosePage to release the text page information.
     *
     * @param fromIndex start index of page.
     * @param toIndex   end index of page.
     * @return list of handles to the text page information structure. NULL if something goes wrong.
     */
    fun prepareTextInfo(fromIndex: Int, toIndex: Int): LongArray {
        if (fromIndex > toIndex) {
            throw IllegalArgumentException("fromIndex cannot be greater than toIndex.")
        }
        val pagePtrsList = mutableListOf<Long>()
        for (i in fromIndex..toIndex) {
            val pagePtr = mNativePagesPtr[i]
            if (!isValidPointer(pagePtr)) {
                throw IllegalStateException("Page at index $i not open. Ensure all pages in range are opened before preparing text info.")
            }
            pagePtrsList.add(pagePtr!!) // pagePtr is checked for null by isValidPointer
        }

        val pagePtrsArray = pagePtrsList.toLongArray()
        val resultTextPagesPtrArray = nativeLoadTextPages(mNativeDocPtr, pagePtrsArray)

        // The returned resultTextPagesPtrArray corresponds to the input pagePtrsArray.
        // We need to map these back to their original page indices.
        resultTextPagesPtrArray.forEachIndexed { resultIndex, textPagePtr ->
            val originalPageIndex = fromIndex + resultIndex
            // Ensure we don't go out of bounds if the returned array is somehow shorter (should not happen if native code is correct)
            if (originalPageIndex <= toIndex) {
                if (isValidPointer(textPagePtr)) {
                    mNativeTextPagesPtr[originalPageIndex] = textPagePtr
                }
            }
        }
        return resultTextPagesPtrArray
    }

    /**
     * Release all resources allocated for a text page information structure.
     *
     * @param fromIndex start index of page.
     * @param toIndex   end index of page.
     */
    fun releaseTextInfo(fromIndex: Int, toIndex: Int) {
        // Iterate through the specified range and release valid text page pointers
        for (i in fromIndex..toIndex) {
            val textPagePtr = mNativeTextPagesPtr[i]
            if (validPtr(textPagePtr)) nativeCloseTextPage(textPagePtr ?: 0)
        }
    }

    private fun ensureTextPage(pageIndex: Int): Long {
        // Check if the text page pointer is valid for the given page index
        val ptr = mNativeTextPagesPtr[pageIndex]
        if (validPtr(ptr)) return ptr ?: 0

        // Prepare the text page if the pointer is invalid
        return prepareTextInfo(pageIndex)
    }

    fun countCharactersOnPage(pageIndex: Int): Int = try {
        val ptr = ensureTextPage(pageIndex)
        when {
            validPtr(ptr) -> nativeTextCountChars(ptr)
            else -> 0
        }
    } catch (e: Exception) {
        logWriter?.writeLog("Error counting characters on page", TAG)
        0
    }

    /**
     * Extract unicode text string from the page.
     *
     * @param pageIndex  index of page.
     * @param startIndex Index for the start characters.
     * @param length     Number of characters to be extracted.
     * @return Number of characters written into the result buffer, including the trailing terminator.
     */
    fun extractCharacters(pageIndex: Int, startIndex: Int, length: Int): String? = try {
        val ptr = ensureTextPage(pageIndex)
        when {
            validPtr(ptr) -> {
                val buf = ShortArray(length + 1)
                val r = nativeTextGetText(ptr, startIndex, length, buf)
                val bytes = ByteArray((r - 1) * 2)
                val bb = ByteBuffer.wrap(bytes)
                bb.order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until r - 1) bb.putShort(buf[i])
                String(bytes, charset("UTF-16LE"))
            }

            else -> null
        }
    } catch (e: Exception) {
        logWriter?.writeLog("Error extracting characters from page", TAG)
        null
    }

    /**
     * Get Unicode of a character in a page.
     *
     * @param pageIndex index of page.
     * @param index     Zero-based index of the character.
     * @return The Unicode of the particular character. If a character is not encoded in Unicode, the return value will be zero.
     */
    fun extractCharacter(pageIndex: Int, index: Int): Char = try {
        val ptr = ensureTextPage(pageIndex)
        if (validPtr(ptr)) nativeTextGetUnicode(ptr, index) else 0
    } catch (e: Exception) {
        logWriter?.writeLog("Error extracting character from page", TAG)
        0
    }.toChar()

    /**
     * Get bounding box of a particular character.
     *
     * @param pageIndex index of page.
     * @param index     Zero-based index of the character.
     * @return the character position measured in PDF "user space".
     */
    fun measureCharacterBox(pageIndex: Int, index: Int): RectF? = try {
        val ptr = ensureTextPage(pageIndex)
        when {
            validPtr(ptr) -> {
                val o = nativeTextGetCharBox(ptr, index)
                RectF(o[0].toFloat(), o[3].toFloat(), o[1].toFloat(), o[2].toFloat())
            }

            else -> null
        }
    } catch (e: Exception) {
        logWriter?.writeLog("Error measuring character box", TAG)
        null
    }

    /**
     * Get the index of a character at or nearby a certain position on the page
     *
     * @param pageIndex  index of page.
     * @param x          X position in PDF "user space".
     * @param y          Y position in PDF "user space".
     * @param xTolerance An x-axis tolerance value for character hit detection, in point unit.
     * @param yTolerance A y-axis tolerance value for character hit detection, in point unit.
     * @return The zero-based index of the character at, or nearby the point (x,y). If there is no character at or nearby the point, return value will be -1. If an error occurs, -3 will be returned.
     */
    fun getCharacterIndex(
        pageIndex: Int,
        x: Double,
        y: Double,
        xTolerance: Double,
        yTolerance: Double,
    ): Int = try {
        val ptr = ensureTextPage(pageIndex)
        if (validPtr(ptr)) nativeTextGetCharIndexAtPos(ptr, x, y, xTolerance, yTolerance) else -1
    } catch (e: Exception) {
        logWriter?.writeLog("Error getting character index", TAG)
        -1
    }

    /**
     * Count number of rectangular areas occupied by a segment of texts.
     *
     *
     * This function, along with FPDFText_GetRect can be used by applications to detect the position
     * on the page for a text segment, so proper areas can be highlighted or something.
     * FPDFTEXT will automatically merge small character boxes into bigger one if those characters
     * are on the same line and use same font settings.
     *
     * @param pageIndex index of page.
     * @param charIndex Index for the start characters.
     * @param count     Number of characters.
     * @return texts areas count.
     */
    fun countTextRect(pageIndex: Int, charIndex: Int, count: Int): Int = try {
        val ptr = ensureTextPage(pageIndex)
        if (validPtr(ptr)) nativeTextCountRects(ptr, charIndex, count) else -1
    } catch (e: Exception) {
        logWriter?.writeLog("Error counting text rectangles", TAG)
        -1
    }

    /**
     * Get a rectangular area from the result generated by FPDFText_CountRects.
     *
     * @param pageIndex index of page.
     * @param rectIndex Zero-based index for the rectangle.
     * @return the text rectangle.
     */
    fun getTextRect(pageIndex: Int, rectIndex: Int): RectF? = try {
        val ptr = ensureTextPage(pageIndex)
        when {
            validPtr(ptr) -> {
                val o = nativeTextGetRect(ptr, rectIndex)
                RectF(o[0].toFloat(), o[1].toFloat(), o[2].toFloat(), o[3].toFloat())
            }

            else -> null
        }
    } catch (e: Exception) {
        logWriter?.writeLog("Error getting text rectangle", TAG)
        null
    }

    /**
     * Extract unicode text within a rectangular boundary on the page.
     * If the buffer is too small, as much text as will fit is copied into it.
     *
     * @param pageIndex index of page.
     * @param rect      the text rectangle to extract.
     * @return If buffer is NULL or buflen is zero, return number of characters (not bytes) of text
     * present within the rectangle, excluding a terminating NUL.
     *
     *
     * Generally you should pass a buffer at least one larger than this if you want a terminating NUL,
     * which will be provided if space is available. Otherwise, return number of characters copied
     * into the buffer, including the terminating NUL  when space for it is available.
     */
    fun extractText(pageIndex: Int, rect: RectF): String? {
        return try {
            val ptr = ensureTextPage(pageIndex)
            if (!validPtr(ptr)) {
                return null
            }
            val length = nativeTextGetBoundedTextLength(
                textPagePtr = ptr,
                left = rect.left.toDouble(),
                top = rect.top.toDouble(),
                right = rect.right.toDouble(),
                bottom = rect.bottom.toDouble()
            )
            if (length <= 0) return null
            val buf = ShortArray(length + 1)
            val r = nativeTextGetBoundedText(
                textPagePtr = ptr,
                left = rect.left.toDouble(),
                top = rect.top.toDouble(),
                right = rect.right.toDouble(),
                bottom = rect.bottom.toDouble(),
                arr = buf
            )
            val bytes = ByteArray((r - 1) * 2)
            val bb = ByteBuffer.wrap(bytes)
            bb.order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until r - 1) bb.putShort(buf[i])
            String(bytes, charset("UTF-16LE"))
        } catch (e: Exception) {
            logWriter?.writeLog("Error extracting text", TAG)
            null
        }
    }

    /**
     * A handle class for the search context. stopSearch must be called to release this handle.
     *
     * @param pageIndex      index of page.
     * @param query          A unicode match pattern.
     * @param matchCase      match case
     * @param matchWholeWord match the whole word
     * @return A handle for the search context.
     */
    fun newPageSearch(
        pageIndex: Int,
        query: String,
        matchCase: Boolean,
        matchWholeWord: Boolean,
    ): TextSearchContext =
        object : FPDFTextSearchContext(pageIndex, query, matchCase, matchWholeWord) {
            private var mSearchHandlePtr: Long? = null
            override fun prepareSearch() {
                val textPage = prepareTextInfo(pageIndex) // Ensures text page is loaded
                // Stop any existing search on this page and remove its handle from the map
                if (this@PdfiumCore.hasSearchHandle(pageIndex)) {
                    val oldSearchHandle = this@PdfiumCore.mNativeSearchHandlePtr.remove(pageIndex)
                    if (isValidPointer(oldSearchHandle)) {
                        nativeSearchStop(oldSearchHandle!!)
                    }
                }
                mSearchHandlePtr = nativeSearchStart(textPage, query, matchCase, matchWholeWord)
                if (isValidPointer(mSearchHandlePtr)) {
                    this@PdfiumCore.mNativeSearchHandlePtr[pageIndex] = mSearchHandlePtr!!
                }
            }

            override val countResult: Int
                get() = when {
                    isValidPointer(mSearchHandlePtr) -> nativeCountSearchResult(mSearchHandlePtr!!)
                    else -> -1
                }

            override val searchNext: RectF?
                get() {
                    if (isValidPointer(mSearchHandlePtr)) {
                        mHasNext = nativeSearchNext(mSearchHandlePtr!!)
                        if (mHasNext) {
                            val index = nativeGetCharIndexOfSearchResult(mSearchHandlePtr!!)
                            if (index > -1) return measureCharacterBox(this.pageIndex, index)
                        }
                    }
                    mHasNext = false
                    return null
                }

            override val searchPrev: RectF?
                get() {
                    if (isValidPointer(mSearchHandlePtr)) {
                        mHasPrev = nativeSearchPrev(mSearchHandlePtr!!)
                        if (mHasPrev) {
                            val index = nativeGetCharIndexOfSearchResult(mSearchHandlePtr!!)
                            if (index > -1) return measureCharacterBox(this.pageIndex, index)
                        }
                    }
                    mHasPrev = false
                    return null
                }

            override fun stopSearch() {
                super.stopSearch()
                if (isValidPointer(mSearchHandlePtr)) {
                    nativeSearchStop(mSearchHandlePtr!!)
                    this@PdfiumCore.mNativeSearchHandlePtr.remove(pageIndex) // Remove from outer class map
                    mSearchHandlePtr = null // Clear local handle
                }
            }
        }

    val currentDpi: Int
        get() = mCurrentDpi

    fun setCurrentDpi(d: Int) {
        mCurrentDpi = d
    }

    fun hasPage(index: Int): Boolean = mNativePagesPtr.containsKey(index)

    fun hasTextPage(index: Int): Boolean = mNativeTextPagesPtr.containsKey(index)

    fun hasSearchHandle(index: Int): Boolean = mNativeSearchHandlePtr.containsKey(index)

    private var logWriter: LogWriter? = null

    /**
     * Sets the [LogWriter] instance to be used for logging.
     *
     * This function allows you to configure the logger with a custom implementation
     * of the `LogWriter` interface.
     *
     * @param logWriter The [LogWriter] instance to use for logging.
     *                  This writer will be responsible for handling log messages.
     */
    fun setLogWriter(logWriter: LogWriter) {
        this.logWriter = logWriter
    }

    private fun validPtr(ptr: Long?): Boolean = ptr != null && ptr != -1L && ptr != 0L // Added 0L check for consistency
    private fun validPtr(ptr: Long): Boolean = ptr != 0L && ptr != -1L // Added -1L check for consistency
    private fun isValidPointer(pointer: Long?): Boolean = pointer != null && pointer != 0L && pointer != -1L // Make it nullable and consistent

    companion object {
        private const val TAG = "PdfiumCore"

        init {
            System.loadLibrary("pdfium")
            System.loadLibrary("pdfium_jni")
        }
    }
}

