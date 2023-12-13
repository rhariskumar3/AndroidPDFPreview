package com.harissk.pdfpreview

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.util.SparseBooleanArray
import androidx.core.util.getOrDefault
import com.harissk.pdfium.Bookmark
import com.harissk.pdfium.Link
import com.harissk.pdfium.Meta
import com.harissk.pdfium.PdfiumCore
import com.harissk.pdfium.util.Size
import com.harissk.pdfium.util.SizeF
import com.harissk.pdfpreview.exception.PageRenderingException
import com.harissk.pdfpreview.utils.FitPolicy
import com.harissk.pdfpreview.utils.PageSizeCalculator
import java.util.LinkedList
import java.util.Queue
import kotlin.math.max


/**
 * Created by Harishkumar on 25/11/23.
 */
/**
 * Manages PDF document and its pages.
 *
 * @param pdfiumCore The PdfiumCore instance used to interact with the PDF document.
 * @param pageFitPolicy The policy used to fit pages to the view.
 * @param viewSize The size of the view where the PDF document is displayed.
 * @param originalUserPages The original user-defined page order (optional).
 * @param isVertical True if scrolling is vertical, else it's horizontal.
 * @param spacingPx Fixed spacing between pages in pixels.
 * @param autoSpacing Calculate spacing automatically so each page fits on its own in the center of the view.
 * @param fitEachPage True if every page should fit separately according to the FitPolicy,
 * @param maxPageCacheSize The maximum number of pages that can be kept in the view
 * else the largest page fits and other pages scale relatively.
 */
class PdfFile(
    private val pdfiumCore: PdfiumCore,
    private val pageFitPolicy: FitPolicy,
    viewSize: Size,
    originalUserPages: List<Int>?,
    private val isVertical: Boolean,
    private val spacingPx: Int,
    private val autoSpacing: Boolean,
    private val fitEachPage: Boolean,
    private val maxPageCacheSize: Int,
) {
    var pagesCount = 0
        private set

    /** Original page sizes  */
    private val originalPageSizes = arrayListOf<Size>()

    /** Scaled page sizes  */
    private val pageSizes = arrayListOf<SizeF>()

    /** Opened pages with indicator whether opening was successful  */
    private val openedPages = SparseBooleanArray()

    /** Opened pages queue **/
    private val openedPageQueue: Queue<Int> = LinkedList()

    /** Page with maximum width  */
    private var originalMaxWidthPageSize: Size = Size(0, 0)

    /** Page with maximum height  */
    private var originalMaxHeightPageSize: Size = Size(0, 0)

    /** Scaled page with maximum height  */
    private var maxHeightPageSize: SizeF? = SizeF(0F, 0F)

    /** Scaled page with maximum width  */
    private var maxWidthPageSize: SizeF? = SizeF(0F, 0F)

    /** Calculated offsets for pages  */
    private val pageOffsets = arrayListOf<Float>()

    /** Calculated auto spacing for pages  */
    private val pageSpacing = arrayListOf<Float>()

    /** Calculated document length (width or height, depending on swipe mode)  */
    private var documentLength = 0f

    /**
     * The pages the user want to display in order
     * (ex: 0, 2, 2, 8, 8, 1, 1, 1)
     */
    private var originalUserPages: List<Int>?

    init {
        this.originalUserPages = originalUserPages
        setup(viewSize)
    }

    /**
     * Sets up the PDF document and its pages.
     *
     * @param viewSize The size of the view where the PDF document is displayed.
     */
    private fun setup(viewSize: Size) {
        pagesCount = when {
            originalUserPages != null -> originalUserPages!!.size
            else -> pdfiumCore.pageCount
        }
        for (i in 0 until pagesCount) {
            val pageSize: Size = pdfiumCore.getPageSize(documentPage(i))
            if (pageSize.width > originalMaxWidthPageSize.width)
                originalMaxWidthPageSize = pageSize
            if (pageSize.height > originalMaxHeightPageSize.height)
                originalMaxHeightPageSize = pageSize
            originalPageSizes.add(pageSize)
        }
        recalculatePageSizes(viewSize)
    }

    /**
     * Recalculates the page sizes, offsets, and document length based on the current view size.
     *
     * @param viewSize The size of the view where the PDF document is displayed.
     */
    fun recalculatePageSizes(viewSize: Size) {
        pageSizes.clear()
        val calculator = PageSizeCalculator(
            fitPolicy = pageFitPolicy,
            originalMaxWidthPageSize = originalMaxWidthPageSize,
            originalMaxHeightPageSize = originalMaxHeightPageSize,
            viewSize = viewSize,
            fitEachPage = fitEachPage
        )
        maxWidthPageSize = calculator.optimalMaxWidthPageSize
        maxHeightPageSize = calculator.optimalMaxHeightPageSize
        for (size in originalPageSizes) pageSizes.add(calculator.calculate(size))
        if (autoSpacing) prepareAutoSpacing(viewSize)
        prepareDocLen()
        preparePagesOffset()
    }

    fun getPageSize(pageIndex: Int): SizeF? = when {
        documentPage(pageIndex) < 0 -> null
        else -> pageSizes.getOrNull(pageIndex)
    }

    fun getScaledPageSize(pageIndex: Int, zoom: Float): SizeF {
        val size: SizeF = getPageSize(pageIndex) ?: SizeF(0F, 0F)
        return SizeF(size.width * zoom, size.height * zoom)
    }

    /**
     * get page size with biggest dimension (width in vertical mode and height in horizontal mode)
     *
     * @return size of page
     */
    val maxPageSize: SizeF?
        get() = if (isVertical) maxWidthPageSize else maxHeightPageSize

    val maxPageWidth: Float
        get() = maxPageSize?.width ?: 0F

    val maxPageHeight: Float
        get() = maxPageSize?.height ?: 0F

    private fun prepareAutoSpacing(viewSize: Size) {
        pageSpacing.clear()
        for (i in 0 until pagesCount) {
            val pageSize: SizeF = pageSizes[i]
            var spacing: Float = max(
                0F,
                if (isVertical) viewSize.height - pageSize.height else viewSize.width - pageSize.width
            )
            if (i < pagesCount - 1) spacing += spacingPx.toFloat()
            pageSpacing.add(spacing)
        }
    }

    private fun prepareDocLen() {
        var length = 0f
        for (i in 0 until pagesCount) {
            val pageSize: SizeF = pageSizes[i]
            length += if (isVertical) pageSize.height else pageSize.width
            when {
                autoSpacing -> length += pageSpacing[i]
                i < pagesCount - 1 -> length += spacingPx.toFloat()
            }
        }
        documentLength = length
    }

    private fun preparePagesOffset() {
        pageOffsets.clear()
        var offset = 0f
        for (i in 0 until pagesCount) {
            val pageSize: SizeF = pageSizes[i]
            val size: Float = if (isVertical) pageSize.height else pageSize.width
            when {
                autoSpacing -> {
                    offset += pageSpacing[i] / 2f
                    when (i) {
                        0 -> offset -= spacingPx / 2f
                        pagesCount - 1 -> offset += spacingPx / 2f
                    }
                    pageOffsets.add(offset)
                    offset += size + pageSpacing[i] / 2f
                }

                else -> {
                    pageOffsets.add(offset)
                    offset += size + spacingPx
                }
            }
        }
    }

    fun getDocLen(zoom: Float): Float = documentLength * zoom

    /**
     * Get the page's height if swiping vertical, or width if swiping horizontal.
     */
    fun getPageLength(pageIndex: Int, zoom: Float): Float {
        val size: SizeF = getPageSize(pageIndex) ?: SizeF(0F, 0F)
        return (if (isVertical) size.height else size.width) * zoom
    }

    fun getPageSpacing(pageIndex: Int, zoom: Float): Float =
        (if (autoSpacing) pageSpacing[pageIndex] else spacingPx.toFloat()) * zoom

    /** Get primary page offset, that is Y for vertical scroll and X for horizontal scroll  */
    fun getPageOffset(pageIndex: Int, zoom: Float): Float {
        return when {
            documentPage(pageIndex) < 0 -> 0F
            else -> pageOffsets[pageIndex] * zoom
        }
    }

    /** Get secondary page offset, that is X for vertical scroll and Y for horizontal scroll  */
    fun getSecondaryPageOffset(pageIndex: Int, zoom: Float): Float {
        val pageSize: SizeF = getPageSize(pageIndex) ?: SizeF(0F, 0F)
        return when {
            isVertical -> {
                val maxWidth = maxPageWidth
                zoom * (maxWidth - pageSize.width) / 2 //x
            }

            else -> {
                val maxHeight = maxPageHeight
                zoom * (maxHeight - pageSize.height) / 2 //y
            }
        }
    }

    fun getPageAtOffset(offset: Float, zoom: Float): Int {
        var currentPage = 0
        for (i in 0 until pagesCount) {
            val off = pageOffsets[i] * zoom - getPageSpacing(i, zoom) / 2f
            if (off >= offset) break
            currentPage++
        }
        return if (--currentPage >= 0) currentPage else 0
    }

    @Throws(PageRenderingException::class)
    fun openPage(pageIndex: Int): Boolean {
        val docPage = documentPage(pageIndex)
        if (docPage < 0) return false
        synchronized(lock) {
            return when {
                openedPages.indexOfKey(docPage) < 0 -> try {
                    pdfiumCore.openPage(docPage)
                    openedPages.put(docPage, true)
                    openedPageQueue.add(pageIndex)
                    if (openedPageQueue.size > maxPageCacheSize)
                        openedPageQueue.poll()?.let { closePage(it) }
                    true
                } catch (e: Exception) {
                    openedPages.put(docPage, false)
                    throw PageRenderingException(pageIndex, e)
                }

                else -> false
            }
        }
    }

    private fun closePage(pageIndex: Int) {
        synchronized(lock) {
            if (openedPages.indexOfKey(pageIndex) >= 0) {
                pdfiumCore.closePage(pageIndex)
                openedPages.delete(pageIndex)
            }
        }
    }

    fun pageHasError(pageIndex: Int): Boolean =
        !openedPages.getOrDefault(documentPage(pageIndex), false)

    fun renderPageBitmap(
        bitmap: Bitmap,
        pageIndex: Int,
        bounds: Rect,
        annotationRendering: Boolean,
    ) = pdfiumCore.renderPageBitmap(
        bitmap = bitmap,
        pageIndex = documentPage(pageIndex),
        startX = bounds.left,
        startY = bounds.top,
        drawSizeX = bounds.width(),
        drawSizeY = bounds.height(),
        renderAnnot = annotationRendering
    )

    val metaData: Meta
        get() = pdfiumCore.documentMeta

    val bookmarks: List<Bookmark>
        get() = pdfiumCore.getTableOfContents()

    fun getPageLinks(pageIndex: Int): List<Link> = pdfiumCore.getPageLinks(documentPage(pageIndex))

    fun mapRectToDevice(
        pageIndex: Int, startX: Int, startY: Int, sizeX: Int, sizeY: Int,
        rect: RectF,
    ): RectF = pdfiumCore.mapPageCoordinateToDevice(
        pageIndex = documentPage(pageIndex),
        startX = startX,
        startY = startY,
        sizeX = sizeX,
        sizeY = sizeY,
        rotate = 0,
        coords = rect
    )

    fun dispose() {
        pdfiumCore.closeDocument()
        originalUserPages = null
    }

    /**
     * Ensures the given user-specified page number is within the valid range,
     * considering the original user-defined page order if provided.
     *
     * @param userPage The user-specified page number.
     * @return The valid page number within the document's range.
     */
    fun determineValidPageNumberFrom(userPage: Int): Int {
        if (userPage <= 0) return 0

        when {
            originalUserPages != null -> {
                // Validate userPage within the original user-defined page order
                if (userPage >= originalUserPages!!.size) return originalUserPages!!.size - 1
            }

            else -> {
                // Validate userPage within the actual document's page count
                if (userPage >= pagesCount) return pagesCount - 1
            }
        }

        return userPage  // UserPage is already valid
    }

    fun documentPage(userPage: Int): Int {
        val documentPage: Int = when {
            // Check if userPage is within the original user-defined page order
            originalUserPages != null -> when {
                userPage >= 0 && userPage < originalUserPages!!.size -> originalUserPages!![userPage]
                else -> return -1
            }
            // Use the userPage directly for the actual document's page count
            else -> userPage
        }

        // Check if the resulting documentPage is valid
        return when (documentPage) {
            in 0..<pagesCount -> documentPage
            else -> -1
        }
    }

    companion object {
        private val lock = Any()
    }
}