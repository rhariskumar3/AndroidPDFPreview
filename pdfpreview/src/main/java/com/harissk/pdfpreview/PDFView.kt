package com.harissk.pdfpreview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.RelativeLayout
import com.harissk.pdfium.Bookmark
import com.harissk.pdfium.Link
import com.harissk.pdfium.Meta
import com.harissk.pdfium.PdfiumCore
import com.harissk.pdfium.util.Size
import com.harissk.pdfium.util.SizeF
import com.harissk.pdfpreview.exception.PageRenderingException
import com.harissk.pdfpreview.link.DefaultLinkHandler
import com.harissk.pdfpreview.model.LinkTapEvent
import com.harissk.pdfpreview.model.PagePart
import com.harissk.pdfpreview.request.PdfRequest
import com.harissk.pdfpreview.request.RenderOptions
import com.harissk.pdfpreview.scroll.ScrollHandle
import com.harissk.pdfpreview.source.DocumentSource
import com.harissk.pdfpreview.utils.FitPolicy
import com.harissk.pdfpreview.utils.SnapEdge
import com.harissk.pdfpreview.utils.toPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

/**
 * Copyright [2024] [Haris Kumar R](https://github.com/rhariskumar3)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * */

/**
 * A view for displaying and interacting with PDF documents. It supports features like animation, zoom,
 * caching, and swiping.
 *
 * The PDF document is rendered as if we want to draw all pages side-by-side, with only the visible
 * portions actually drawn on the screen. All parts are rendered at the same size to optimize
 * performance and allow for interruption of native rendering. Parts are loaded when the offset or
 * zoom level changes.
 */
class PDFView(context: Context?, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var pdfRequest: PdfRequest? = null

    private var _pdfFile: PdfFile? = null
    val pdfFile: PdfFile
        get() = when (_pdfFile) {
            null -> loadError(IllegalStateException("PDF not decoded"))
            else -> _pdfFile
        } ?: throw IllegalStateException("PDF not decoded")

    val renderOptions: RenderOptions
        get() = pdfRequest?.renderOptions ?: RenderOptions.DEFAULT

    val minZoom: Float = DEFAULT_MIN_SCALE
    val midZoom: Float = DEFAULT_MID_SCALE
    val maxZoom: Float = DEFAULT_MAX_SCALE

    /**
     * START - scrolling in first page direction
     * END - scrolling in last page direction
     * NONE - not scrolling
     */
    internal enum class ScrollDir {
        NONE,
        START,
        END
    }

    private var scrollDir: ScrollDir = ScrollDir.NONE

    /** Rendered parts go to the cache manager  */
    internal val cacheManager = CacheManager(renderOptions)

    /** Animation manager manage all offset and zoom animation  */
    private val animationManager: AnimationManager = AnimationManager(this)

    /** Drag manager manage all touch events  */
    private val dragPinchManager: DragPinchManager = DragPinchManager(this, animationManager)

    /** The index of the current sequence  */
    var currentPage = 0
        private set

    /**
     * If you picture all the pages side by side in their optimal width, and taking into account the zoom
     * level, the current offset is the position of the left border of the screen in this big picture
     */
    var currentXOffset = 0f
        private set

    /**
     * If you picture all the pages side by side in their optimal width, and taking into account the zoom
     * level, the current offset is the position of the left border of the screen in this big picture
     */
    var currentYOffset = 0f
        private set

    /** The zoom level, always >= 1  */
    var zoom: Float = 1f
        private set

    /** True if the PDFView has been Recycling  */
    var isRecycling = false
        private set

    /** True if the PDFView has been recycled  */
    var isRecycled = true
        private set

    /** Current state of the view  */
    private var state: State = State.DEFAULT

    /** The thread [.renderingHandler] will run on  */
    private var renderingHandlerThread: HandlerThread?

    /** Handler always waiting in the background and rendering tasks  */
    var renderingHandler: RenderingHandler? = null
    private val pagesLoader: PagesLoader = PagesLoader(this)

    /** Paint object for drawing  */
    private val paint: Paint = Paint()

    /** Paint object for drawing debug stuff  */
    private val debugPaint: Paint = Paint()

    /** Policy for fitting pages to screen  */
    var pageFitPolicy: FitPolicy = FitPolicy.WIDTH
        private set
    var isFitEachPage: Boolean = false
        private set
    private var defaultPage = 0

    /** True if should scroll through pages vertically instead of horizontally  */
    var isSwipeVertical: Boolean = true
        private set
    var isSwipeEnabled: Boolean = true
    var isDoubleTapEnabled: Boolean = true
        private set
    private var nightMode: Boolean = false
    var isPageSnap: Boolean = true

    /** Pdfium core for loading and rendering PDFs  */
    private val pdfiumCore: PdfiumCore = PdfiumCore()
    var scrollHandle: ScrollHandle? = null
        private set
    private var isScrollHandleInit: Boolean = false

    /**
     * True if bitmap should use ARGB_8888 format and take more memory
     * False if bitmap should be compressed by using RGB_565 format and take less memory
     */
    var isBestQuality: Boolean = false
        private set

    /**
     * True if annotations should be rendered
     * False otherwise
     */
    var isAnnotationRendering: Boolean = false
        private set

    /**
     * True if the view should render during scaling<br></br>
     * Can not be forced on older API versions (< Build.VERSION_CODES.KITKAT) as the GestureDetector
     * does not detect scrolling while scaling.<br></br>
     * False otherwise
     */
    private var renderDuringScale: Boolean = false

    /** Antialiasing and bitmap filtering  */
    var isAntialiasing: Boolean = true
        private set
    private val antialiasFilter =
        PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** Spacing between pages, in px  */
    var spacingPx: Int = 0
        private set

    /** Add dynamic spacing to fit each page separately on the screen.  */
    var isAutoSpacingEnabled: Boolean = false
        private set

    /** Fling a single page at a time  */
    var isPageFlingEnabled: Boolean = true
        private set

    /** Pages numbers used when calling onDrawAllListener  */
    private val onDrawPagesNumbers: ArrayList<Int> = ArrayList(10)

    /** Holds info whether view has been added to layout and has width and height  */
    private var hasSize: Boolean = false

    /** Construct the initial view  */
    init {
        renderingHandlerThread = HandlerThread("PDF renderer")
        if (!isInEditMode) {
            debugPaint.style = Paint.Style.STROKE
            setWillNotDraw(false)
        }
    }

    fun enqueue(pdfRequest: PdfRequest) {
        this.pdfRequest = pdfRequest
        isSwipeEnabled = pdfRequest.enableSwipe
        setNightMode(pdfRequest.nightMode)
        isDoubleTapEnabled = pdfRequest.enableDoubleTap
        defaultPage = pdfRequest.defaultPage
        isSwipeVertical = !pdfRequest.swipeHorizontal
        isAnnotationRendering = pdfRequest.annotationRendering
        scrollHandle = pdfRequest.scrollHandle
        isAntialiasing = pdfRequest.antialiasing
        spacingPx = context.toPx(pdfRequest.spacing)
        isAutoSpacingEnabled = pdfRequest.autoSpacing
        pageFitPolicy = pdfRequest.pageFitPolicy
        isFitEachPage = pdfRequest.fitEachPage
        isPageSnap = pdfRequest.pageSnap
        isPageFlingEnabled = pdfRequest.pageFling
        isBestQuality = false
        if (pdfRequest.disableLongPress) dragPinchManager.disableLongPress()

        if (!hasSize) return
        scope.async(Dispatchers.Main.immediate) {
            recycle(false)
            load(pdfRequest.source, pdfRequest.password, pdfRequest.pageNumbers)
        }
    }

    private suspend fun load(
        docSource: DocumentSource,
        password: String?,
        userPages: List<Int>? = null,
    ) {
        check(isRecycled) { "Don't call load on a PDF View without recycling it first." }
        isRecycling = false
        isRecycled = false

        // Start decoding document
        try {
            pdfRequest?.documentLoadListener?.onDocumentLoadingStart()
            val pdfFile = withContext(Dispatchers.IO) {
                docSource.createDocument(this@PDFView.context, pdfiumCore, password)
                PdfFile(
                    pdfiumCore = pdfiumCore,
                    pageFitPolicy = pageFitPolicy,
                    viewSize = Size(this@PDFView.width, this@PDFView.height),
                    originalUserPages = userPages,
                    isVertical = isSwipeVertical,
                    spacingPx = spacingPx,
                    autoSpacing = isAutoSpacingEnabled,
                    fitEachPage = isFitEachPage,
                    maxPageCacheSize = renderOptions.maxPageCacheSize
                )
            }

            withContext(Dispatchers.Main) {
                this@PDFView.loadComplete(pdfFile)
            }
        } catch (t: Throwable) {
            withContext(Dispatchers.Main) {
                this@PDFView.loadError(t)
            }
        }
    }

    /**
     * Go to the given page.
     *
     * @param page Page index.
     */
    fun jumpTo(page: Int, withAnimation: Boolean = false) {
        _pdfFile ?: return
        val userPage = pdfFile.determineValidPageNumberFrom(page)
        val offset: Float = if (userPage == 0) 0F else -pdfFile.getPageOffset(userPage, zoom)
        when {
            isSwipeVertical -> when {
                withAnimation -> animationManager.startYAnimation(currentYOffset, offset)
                else -> moveTo(currentXOffset, offset)
            }

            else -> when {
                withAnimation -> animationManager.startXAnimation(currentXOffset, offset)
                else -> moveTo(offset, currentYOffset)
            }
        }
        showPage(userPage)
    }

    fun showPage(pageNb: Int) {
        if (isRecycled) return

        // Check the page number and makes the difference between UserPages and DocumentPages
        val userPage = pdfFile.determineValidPageNumberFrom(pageNb)
        currentPage = userPage
        loadPages()
        if (!documentFitsView()) scrollHandle?.setPageNum(currentPage + 1)
        pdfRequest?.pageNavigationEventListener?.onPageChanged(currentPage, pageCount)
    }

    /**
     * Get current position as ratio of document length to visible area. 0 means that document start
     * is visible, 1 that document end is visible
     *
     * @return offset between 0 and 1
     */
    var positionOffset: Float
        get() = when {
            isSwipeVertical -> -currentYOffset / (pdfFile.getDocLen(zoom) - height)
            else -> -currentXOffset / (pdfFile.getDocLen(zoom) - width)
        }.coerceIn(0F, 1F)
        set(progress) = setPositionOffset(progress, true)

    /**
     * @param progress   must be between 0 and 1
     * @param moveHandle whether to move scroll handle
     * @see PDFView.positionOffset
     */
    fun setPositionOffset(progress: Float, moveHandle: Boolean = false) {
        when {
            isSwipeVertical -> moveTo(
                currentXOffset,
                (-pdfFile.getDocLen(zoom) + height) * progress,
                moveHandle
            )

            else -> moveTo(
                (-pdfFile.getDocLen(zoom) + width) * progress,
                currentYOffset,
                moveHandle
            )
        }
        loadPageByOffset()
    }

    fun stopFling() = animationManager.stopFling()

    val pageCount: Int
        get() = _pdfFile?.pagesCount ?: 0

    private fun setNightMode(nightMode: Boolean) {
        this.nightMode = nightMode
        when {
            nightMode -> {
                val colorMatrixInverted = ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.setColorFilter(ColorMatrixColorFilter(colorMatrixInverted))
            }

            else -> paint.setColorFilter(null)
        }
    }

    fun onPageError(ex: PageRenderingException) {
        pdfRequest?.renderingEventListener?.onPageFailedToRender(ex)
    }

    fun recycle() = recycle(true)

    private fun recycle(isRemoveRequest: Boolean = true) {
        isRecycling = true

        if (isRemoveRequest) pdfRequest = null

        animationManager.stopAll()
        dragPinchManager.disable()

        // Stop tasks
        renderingHandler?.stop()
        renderingHandler?.removeMessages(RenderingHandler.MSG_RENDER_TASK)

        // Clear caches
        cacheManager.recycle()
        if (isScrollHandleInit && isRemoveRequest) scrollHandle?.destroyLayout()
        _pdfFile?.dispose()
        _pdfFile = null
        renderingHandler = null
        if (isRemoveRequest) {
            scrollHandle = null
            isScrollHandleInit = false
        }
        currentYOffset = 0f
        currentXOffset = currentYOffset
        zoom = 1f
        isRecycling = false
        isRecycled = true
        state = State.DEFAULT
    }

    /** Handle fling animation  */
    override fun computeScroll() {
        super.computeScroll()
        if (isInEditMode) return
        animationManager.computeFling()
    }

    override fun onDetachedFromWindow() {
        recycle()
        renderingHandlerThread?.quitSafely()
        renderingHandlerThread = null
        scope.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        hasSize = true
        pdfRequest?.let { enqueue(it) }
        if (isInEditMode || state != State.SHOWN) return

        // calculates the position of the point which in the center of view relative to big strip
        val centerPointInStripXOffset = -currentXOffset + oldw * 0.5f
        val centerPointInStripYOffset = -currentYOffset + oldh * 0.5f
        val relativeCenterPointInStripXOffset: Float
        val relativeCenterPointInStripYOffset: Float
        when {
            isSwipeVertical -> {
                relativeCenterPointInStripXOffset = centerPointInStripXOffset / pdfFile.maxPageWidth
                relativeCenterPointInStripYOffset =
                    centerPointInStripYOffset / pdfFile.getDocLen(zoom)
            }

            else -> {
                relativeCenterPointInStripXOffset =
                    centerPointInStripXOffset / pdfFile.getDocLen(zoom)
                relativeCenterPointInStripYOffset =
                    centerPointInStripYOffset / pdfFile.maxPageHeight
            }
        }
        animationManager.stopAll()
        pdfFile.recalculatePageSizes(Size(w, h))
        if (isSwipeVertical) {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile.maxPageWidth + w * 0.5f
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile.getDocLen(zoom) + h * 0.5f
        } else {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile.getDocLen(zoom) + w * 0.5f
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile.maxPageHeight + h * 0.5f
        }
        moveTo(currentXOffset, currentYOffset)
        loadPageByOffset()
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        _pdfFile ?: return true
        return when {
            isSwipeVertical -> when {
                direction < 0 && currentXOffset < 0 -> true
                direction > 0 && currentXOffset + toCurrentScale(pdfFile.maxPageWidth) > width -> true
                else -> false
            }

            else -> when {
                direction < 0 && currentXOffset < 0 -> true
                direction > 0 && currentXOffset + pdfFile.getDocLen(zoom) > width -> true
                else -> false
            }
        }
    }

    override fun canScrollVertically(direction: Int): Boolean {
        _pdfFile ?: return true
        return when {
            isSwipeVertical -> when {
                direction < 0 && currentYOffset < 0 -> true
                direction > 0 && currentYOffset + pdfFile.getDocLen(zoom) > height -> true
                else -> false
            }

            else -> when {
                direction < 0 && currentYOffset < 0 -> true
                direction > 0 && currentYOffset + toCurrentScale(pdfFile.maxPageHeight) > height -> true
                else -> false
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (isInEditMode) return
        if (isRecycled) return
        // As I said in this class javadoc, we can think of this canvas as a huge
        // strip on which we draw all the images. We actually only draw the rendered
        // parts, of course, but we render them in the place they belong in this huge
        // strip.

        // That's where Canvas.translate(x, y) becomes very helpful.
        // This is the situation :
        //  _______________________________________________
        // |   			 |					 			   |
        // | the actual  |					The big strip  |
        // |	canvas	 | 								   |
        // |_____________|								   |
        // |_______________________________________________|
        //
        // If the rendered part is on the bottom right corner of the strip
        // we can draw it but we won't see it because the canvas is not big enough.

        // But if we call translate(-X, -Y) on the canvas just before drawing the object :
        //  _______________________________________________
        // |   			  					  _____________|
        // |   The big strip     			 |			   |
        // |		    					 |	the actual |
        // |								 |	canvas	   |
        // |_________________________________|_____________|
        //
        // The object will be on the canvas.
        // This technique is massively used in this method, and allows
        // abstraction of the screen position when rendering the parts.

        // Draws background
        if (isAntialiasing) canvas.setDrawFilter(antialiasFilter)
        when (background) {
            null -> canvas.drawColor(if (nightMode) Color.BLACK else Color.WHITE)
            else -> background.draw(canvas)
        }
        if (state != State.SHOWN) return

        // Moves the canvas before drawing any element
        val currentXOffset = currentXOffset
        val currentYOffset = currentYOffset
        canvas.translate(currentXOffset, currentYOffset)

        // Draws thumbnails
        for (part in cacheManager.getThumbnails()) drawPart(canvas, part)

        // Draws parts
        for (part in cacheManager.getPageParts()) drawPart(canvas, part)
        onDrawPagesNumbers.clear()
        if (renderOptions.debugMode && pdfRequest?.renderingEventListener != null)
            drawWithListener(canvas, currentPage)

        // Restores the canvas position
        canvas.translate(-currentXOffset, -currentYOffset)
    }

    private fun drawWithListener(canvas: Canvas, page: Int) {
        val translateX: Float
        val translateY: Float
        when {
            isSwipeVertical -> {
                translateX = 0f
                translateY = pdfFile.getPageOffset(page, zoom)
            }

            else -> {
                translateY = 0f
                translateX = pdfFile.getPageOffset(page, zoom)
            }
        }
        canvas.translate(translateX, translateY)
        val size: SizeF = pdfFile.getPageSize(page) ?: SizeF(0f, 0f)
        pdfRequest?.renderingEventListener?.onDrawPage(
            canvas = canvas,
            pageWidth = toCurrentScale(size.width),
            pageHeight = toCurrentScale(size.height),
            displayedPage = page
        )
        canvas.translate(-translateX, -translateY)
    }

    /** Draw a given PagePart on the canvas  */
    private fun drawPart(canvas: Canvas, part: PagePart) {
        // Can seem strange, but avoid lot of calls
        val pageRelativeBounds = part.pageRelativeBounds
        val renderedBitmap = part.renderedBitmap ?: return
        if (renderedBitmap.isRecycled) return

        // Move to the target page
        val localTranslationX: Float
        val localTranslationY: Float
        val size: SizeF = pdfFile.getPageSize(part.page) ?: SizeF(0f, 0f)
        if (isSwipeVertical) {
            localTranslationY = pdfFile.getPageOffset(part.page, zoom)
            localTranslationX = toCurrentScale(pdfFile.maxPageWidth - size.width) / 2
        } else {
            localTranslationX = pdfFile.getPageOffset(part.page, zoom)
            localTranslationY = toCurrentScale(pdfFile.maxPageHeight - size.height) / 2
        }
        canvas.translate(localTranslationX, localTranslationY)
        val srcRect = Rect(0, 0, renderedBitmap.width, renderedBitmap.height)
        val offsetX = toCurrentScale(pageRelativeBounds.left * size.width)
        val offsetY = toCurrentScale(pageRelativeBounds.top * size.height)
        val width = toCurrentScale(pageRelativeBounds.width() * size.width)
        val height = toCurrentScale(pageRelativeBounds.height() * size.height)

        // If we use float values for this rectangle, there will be
        // a possible gap between page parts, especially when
        // the zoom level is high.
        val dstRect = RectF(
            offsetX.toInt().toFloat(), offsetY.toInt().toFloat(),
            (offsetX + width).toInt().toFloat(),
            (offsetY + height).toInt().toFloat()
        )

        // Check if bitmap is in the screen
        val translationX = currentXOffset + localTranslationX
        val translationY = currentYOffset + localTranslationY
        if (translationX + dstRect.left >= getWidth() || translationX + dstRect.right <= 0
            || translationY + dstRect.top >= getHeight() || translationY + dstRect.bottom <= 0
        ) {
            canvas.translate(-localTranslationX, -localTranslationY)
            return
        }
        canvas.drawBitmap(renderedBitmap, srcRect, dstRect, paint)
        if (renderOptions.debugMode) {
            debugPaint.setColor(if (part.page % 2 == 0) Color.RED else Color.BLUE)
            canvas.drawRect(dstRect, debugPaint)
        }

        // Restore the canvas position
        canvas.translate(-localTranslationX, -localTranslationY)
    }

    /**
     * Load all the parts around the center of the screen,
     * taking into account X and Y offsets, zoom level, and
     * the current page displayed
     */
    fun loadPages() {
        if (_pdfFile == null || renderingHandler == null) return
        // Cancel all current tasks
        renderingHandler?.removeMessages(RenderingHandler.MSG_RENDER_TASK)
        cacheManager.makeANewSet()
        pagesLoader.loadPages()
        redraw()
    }

    /** Called when the PDF is loaded  */
    private fun loadComplete(pdfFile: PdfFile) {
        if (isRecycling) return
        state = State.LOADED
        _pdfFile = pdfFile

        // If a ScrollHandle is provided in the PdfRequest
        try {
            if (pdfRequest?.scrollHandle != null) {
                if (renderingHandlerThread?.isAlive == false) renderingHandlerThread?.start()
                renderingHandler = RenderingHandler(renderingHandlerThread!!.getLooper(), this)
                renderingHandler?.start()
            }
            if (scrollHandle != null) {
                scrollHandle?.setupLayout(this)
                isScrollHandleInit = true
            }
            dragPinchManager.enable()
            pdfRequest?.documentLoadListener?.onDocumentLoaded(pageCount)
            jumpTo(defaultPage, false)
        } catch (e: Exception) {
            loadError(e)
        }
    }

    private fun loadError(t: Throwable): Nothing? {
        if (isRecycling) return null
        state = State.ERROR
        pdfRequest?.documentLoadListener?.onDocumentLoadError(t)
        recycle()
        invalidate()
        return null
    }

    fun redraw() {
        invalidate()
    }

    /**
     * Called when a rendering task is over and
     * a PagePart has been freshly created.
     *
     * @param part The created PagePart.
     */
    fun onBitmapRendered(part: PagePart) {
        // when it is first rendered part
        if (state == State.LOADED) {
            state = State.SHOWN
            pdfRequest?.renderingEventListener?.onPageRendered(part.page)
        }
        when {
            part.isThumbnail -> cacheManager.cacheThumbnail(part)
            else -> cacheManager.cachePart(part)
        }
        redraw()
    }

    /**
     * Move to the given X and Y offsets, but check them ahead of time
     * to be sure not to go outside the the big strip.
     *
     * @param offsetX    The big strip X offset to use as the left border of the screen.
     * @param offsetY    The big strip Y offset to use as the right border of the screen.
     * @param moveHandle whether to move scroll handle or not
     */
    fun moveTo(offsetX: Float, offsetY: Float, moveHandle: Boolean = true) {
        var offsetX1 = offsetX
        var offsetY1 = offsetY
        when {
            isSwipeVertical -> {
                // Check X offset
                val scaledPageWidth = toCurrentScale(pdfFile.maxPageWidth)
                when {
                    scaledPageWidth < width -> offsetX1 = width / 2 - scaledPageWidth / 2
                    else -> when {
                        offsetX1 > 0 -> offsetX1 = 0f
                        offsetX1 + scaledPageWidth < width -> offsetX1 = width - scaledPageWidth
                    }
                }

                // Check Y offset
                val contentHeight = pdfFile.getDocLen(zoom)
                // top visible
                when {
                    // whole document height visible on screen
                    contentHeight < height -> offsetY1 = (height - contentHeight) / 2
                    else -> when {
                        offsetY1 > 0 -> offsetY1 = 0f
                        // bottom visible
                        offsetY1 + contentHeight < height -> offsetY1 = -contentHeight + height
                    }
                }
                scrollDir = when {
                    offsetY1 < currentYOffset -> ScrollDir.END
                    offsetY1 > currentYOffset -> ScrollDir.START
                    else -> ScrollDir.NONE
                }
            }

            else -> {
                // Check Y offset
                val scaledPageHeight = toCurrentScale(pdfFile.maxPageHeight)
                when {
                    scaledPageHeight < height -> offsetY1 = height / 2 - scaledPageHeight / 2
                    else -> when {
                        offsetY1 > 0 -> offsetY1 = 0f
                        offsetY1 + scaledPageHeight < height -> offsetY1 = height - scaledPageHeight
                    }
                }

                // Check X offset
                val contentWidth = pdfFile.getDocLen(zoom)
                when {
                    // whole document width visible on screen
                    contentWidth < width -> offsetX1 = (width - contentWidth) / 2
                    else -> when {
                        // left visible
                        offsetX1 > 0 -> offsetX1 = 0f
                        // right visible
                        offsetX1 + contentWidth < width -> offsetX1 = -contentWidth + width
                    }
                }
                scrollDir = when {
                    offsetX1 < currentXOffset -> ScrollDir.END
                    offsetX1 > currentXOffset -> ScrollDir.START
                    else -> ScrollDir.NONE
                }
            }
        }
        currentXOffset = offsetX1
        currentYOffset = offsetY1
        val positionOffset = positionOffset
        if (moveHandle && !documentFitsView()) scrollHandle?.setScroll(positionOffset)
        pdfRequest?.pageNavigationEventListener?.onPageScrolled(currentPage, positionOffset)
        redraw()
    }

    fun loadPageByOffset() {
        if (0 == pageCount) return
        val offset: Float
        val screenCenter: Float
        when {
            isSwipeVertical -> {
                offset = currentYOffset
                screenCenter = height.toFloat() / 2
            }

            else -> {
                offset = currentXOffset
                screenCenter = width.toFloat() / 2
            }
        }
        val page = pdfFile.getPageAtOffset(-(offset - screenCenter), zoom)
        when {
            page >= 0 && page <= pdfFile.pagesCount - 1 && page != currentPage -> showPage(page)
            else -> loadPages()
        }
    }

    /**
     * Animate to the nearest snapping position for the current SnapPolicy
     */
    fun performPageSnap() {
        if (!isPageSnap || _pdfFile == null || pageCount == 0) return
        val centerPage = findFocusPage(currentXOffset, currentYOffset)
        val edge = findSnapEdge(centerPage)
        if (edge === SnapEdge.NONE) return
        val offset = snapOffsetForPage(centerPage, edge)
        when {
            isSwipeVertical -> animationManager.startYAnimation(currentYOffset, -offset)
            else -> animationManager.startXAnimation(currentXOffset, -offset)
        }
    }

    /**
     * Find the edge to snap to when showing the specified page
     */
    internal fun findSnapEdge(page: Int): SnapEdge {
        if (!isPageSnap || page < 0) return SnapEdge.NONE
        val currentOffset = if (isSwipeVertical) currentYOffset else currentXOffset
        val offset = -pdfFile.getPageOffset(page, zoom)
        val length = if (isSwipeVertical) height else width
        val pageLength = pdfFile.getPageLength(page, zoom)
        return when {
            length >= pageLength -> SnapEdge.CENTER
            currentOffset >= offset -> SnapEdge.START
            offset - pageLength > currentOffset - length -> SnapEdge.END
            else -> SnapEdge.NONE
        }
    }

    /**
     * Get the offset to move to in order to snap to the page
     */
    internal fun snapOffsetForPage(pageIndex: Int, edge: SnapEdge): Float {
        val offset = pdfFile.getPageOffset(pageIndex, zoom)
        val length = if (isSwipeVertical) height else width
        val pageLength = pdfFile.getPageLength(pageIndex, zoom)
        return when {
            edge === SnapEdge.CENTER -> offset - length / 2f + pageLength / 2f
            edge === SnapEdge.END -> offset - length + pageLength
            else -> offset
        }
    }

    fun findFocusPage(xOffset: Float, yOffset: Float): Int {
        val currOffset = if (isSwipeVertical) yOffset else xOffset
        val length = if (isSwipeVertical) height else width
        // make sure first and last page can be found
        return when {
            currOffset > -1 -> 0
            currOffset < -pdfFile.getDocLen(zoom) + length + 1 -> pdfFile.pagesCount - 1
            // else find page in center
            else -> pdfFile.getPageAtOffset(-(currOffset - length / 2f), zoom)
        }
    }

    /**
     * @return true if single page fills the entire screen in the scrolling direction
     */
    fun pageFillsScreen(): Boolean {
        val start = -pdfFile.getPageOffset(currentPage, zoom)
        val end = start - pdfFile.getPageLength(currentPage, zoom)
        return when {
            isSwipeVertical -> start > currentYOffset && end < currentYOffset - height
            else -> start > currentXOffset && end < currentXOffset - width
        }
    }

    /**
     * Move relatively to the current position.
     *
     * @param dx The X difference you want to apply.
     * @param dy The Y difference you want to apply.
     * @see .moveTo
     */
    fun moveRelativeTo(dx: Float, dy: Float) = moveTo(currentXOffset + dx, currentYOffset + dy)

    /**
     * Change the zoom level
     */
    fun zoomTo(zoom: Float) {
        this.zoom = zoom
    }

    /**
     * Change the zoom level, relatively to a pivot point.
     * It will call moveTo() to make sure the given point stays
     * in the middle of the screen.
     *
     * @param zoom  The zoom level.
     * @param pivot The point on the screen that should stays.
     */
    fun zoomCenteredTo(zoom: Float, pivot: PointF) {
        val dzoom = zoom / this.zoom
        zoomTo(zoom)
        var baseX = currentXOffset * dzoom
        var baseY = currentYOffset * dzoom
        baseX += pivot.x - pivot.x * dzoom
        baseY += pivot.y - pivot.y * dzoom
        moveTo(baseX, baseY)
    }

    /**
     * @see .zoomCenteredTo
     */
    fun zoomCenteredRelativeTo(dzoom: Float, pivot: PointF) {
        zoomCenteredTo(zoom * dzoom, pivot)
    }

    /**
     * Checks if whole document can be displayed on screen, doesn't include zoom
     *
     * @return true if whole document can displayed at once, false otherwise
     */
    fun documentFitsView(): Boolean {
        val len = pdfFile.getDocLen(1f)
        return when {
            isSwipeVertical -> len < height
            else -> len < width
        }
    }

    fun fitToWidth(page: Int) {
        if (state != State.SHOWN) {
            Log.e(TAG, "Cannot fit, document not rendered yet")
            return
        }
        zoomTo(width / pdfFile.getPageSize(page)?.width!!)
        jumpTo(page)
    }

    fun getPageSize(pageIndex: Int): SizeF = pdfFile.getPageSize(pageIndex) ?: SizeF(0F, 0F)

    fun toRealScale(size: Float): Float = size / zoom

    fun toCurrentScale(size: Float): Float = size * zoom

    val isZooming: Boolean
        get() = zoom != minZoom

    fun resetZoom() = zoomTo(minZoom)

    fun resetZoomWithAnimation() = zoomWithAnimation(minZoom)

    fun zoomWithAnimation(centerX: Float, centerY: Float, scale: Float) =
        animationManager.startZoomAnimation(centerX, centerY, zoom, scale)

    fun zoomWithAnimation(scale: Float) {
        animationManager.startZoomAnimation(
            centerX = (width / 2).toFloat(),
            centerY = (height / 2).toFloat(),
            zoomFrom = zoom,
            zoomTo = scale
        )
    }

    /**
     * Get page number at given offset
     *
     * @param positionOffset scroll offset between 0 and 1
     * @return page number at given offset, starting from 0
     */
    fun getPageAtPositionOffset(positionOffset: Float): Int =
        pdfFile.getPageAtOffset(pdfFile.getDocLen(zoom) * positionOffset, zoom)

    val doRenderDuringScale: Boolean
        get() = renderDuringScale

    /** Returns null if document is not loaded  */
    val documentMeta: Meta?
        get() = _pdfFile?.metaData

    /** Will be empty until document is loaded  */
    val tableOfContents: List<Bookmark>
        get() = _pdfFile?.bookmarks.orEmpty()

    /** Will be empty until document is loaded  */
    fun getLinks(page: Int): List<Link> = _pdfFile?.getPageLinks(page).orEmpty()

    fun callOnTap(e: MotionEvent): Boolean = pdfRequest?.gestureEventListener?.onTap(e) ?: false

    fun callOnLongPress(e: MotionEvent) {
        pdfRequest?.gestureEventListener?.onLongPress(e)
    }

    fun callLinkHandler(linkTapEvent: LinkTapEvent) {
        pdfRequest?.linkHandler?.handleLinkEvent(linkTapEvent) ?: DefaultLinkHandler(this)
    }

    private enum class State {
        DEFAULT,
        LOADED,
        SHOWN,
        ERROR
    }

    companion object {
        private val TAG = PDFView::class.java.getSimpleName()
        const val DEFAULT_MAX_SCALE = 3.0f
        const val DEFAULT_MID_SCALE = 1.75f
        const val DEFAULT_MIN_SCALE = 1.0f
    }
}