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
import android.view.MotionEvent
import android.widget.RelativeLayout
import com.harissk.pdfium.Bookmark
import com.harissk.pdfium.Link
import com.harissk.pdfium.Meta
import com.harissk.pdfium.PdfiumCore
import com.harissk.pdfium.exception.IncorrectPasswordException
import com.harissk.pdfium.exception.PageRenderingException
import com.harissk.pdfium.listener.LogWriter
import com.harissk.pdfium.util.Size
import com.harissk.pdfium.util.SizeF
import com.harissk.pdfpreview.link.DefaultLinkHandler
import com.harissk.pdfpreview.listener.DocumentLoadListener
import com.harissk.pdfpreview.model.LinkTapEvent
import com.harissk.pdfpreview.model.PagePart
import com.harissk.pdfpreview.request.PdfLoadRequest
import com.harissk.pdfpreview.request.PdfViewConfiguration
import com.harissk.pdfpreview.request.PdfViewerConfiguration
import com.harissk.pdfpreview.scroll.ScrollHandle
import com.harissk.pdfpreview.source.DocumentSource
import com.harissk.pdfpreview.utils.FitPolicy
import com.harissk.pdfpreview.utils.SnapEdge
import com.harissk.pdfpreview.utils.toPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

/**
 * Copyright [2025] [Haris Kumar R](https://github.com/rhariskumar3)
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

    // Track the current loading job to prevent concurrent operations
    private var currentLoadingJob: Job? = null
    private var isCurrentlyLoading: Boolean = false

    // Configuration approach
    private var viewConfiguration: PdfViewConfiguration = PdfViewConfiguration.DEFAULT
    private var pendingLoadRequest: PdfLoadRequest? = null
    private var currentDocumentLoadListener: DocumentLoadListener? = null

    private var _pdfFile: PdfFile? = null
    val pdfFile: PdfFile
        get() = when (_pdfFile) {
            null -> loadError(IllegalStateException("PDF not decoded"))
            else -> _pdfFile
        } ?: throw IllegalStateException("PDF not decoded")

    val pdfViewerConfiguration: PdfViewerConfiguration
        get() = viewConfiguration.pdfViewerConfiguration

    val singlePageMode: Boolean
        get() = viewConfiguration.singlePageMode

    val minZoom: Float = DEFAULT_MIN_SCALE
    val midZoom: Float = DEFAULT_MID_SCALE
    val maxZoom: Float = DEFAULT_MAX_SCALE

    /** Rendered parts go to the cache manager  */
    internal val cacheManager = CacheManager(pdfViewerConfiguration)

    /** Animation manager manage all offset and zoom animation  */
    private val pdfAnimator: PdfAnimator = PdfAnimator(this)

    /** Drag manager manage all touch events  */
    private val dragPinchManager = DragPinchManager(this, pdfAnimator)

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
    internal var isRecycling = false

    /** True if the PDFView is currently loading a PDF  */
    internal var isLoading = false

    /** True if the PDFView has been recycled  */
    internal var isRecycled = false

    /** Current state of the view  */
    private var state: State = State.DEFAULT

    /** The thread [.renderingHandler] will run on  */
    private var renderingHandlerThread: HandlerThread?

    /** Handler always waiting in the background and rendering tasks  */
    internal var renderingHandler: RenderingHandler? = null
    private val pagesLoader: PagesLoader = PagesLoader(this)

    /** Paint object for drawing  */
    private val paint: Paint = Paint()

    /** Paint object for drawing debug stuff  */
    private val debugPaint: Paint = Paint()

    /** Policy for fitting pages to screen  */
    private var pageFitPolicy: FitPolicy = FitPolicy.WIDTH
    private var isFitEachPage: Boolean = false
    private var defaultPage = 0

    /** True if should scroll through pages vertically instead of horizontally  */
    internal var isSwipeVertical: Boolean = true
    var isSwipeEnabled: Boolean = true
    internal var isDoubleTapEnabled: Boolean = true
    private var nightMode: Boolean = false
    var isPageSnap: Boolean = true

    /** Pdfium core for loading and rendering PDFs  */
    private val pdfiumCore: PdfiumCore = PdfiumCore()
    internal var scrollHandle: ScrollHandle? = null
    private var isScrollHandleInit: Boolean = false

    internal var isScrollHandleDragging: Boolean = false
        private set

    /**
     * True if bitmap should use ARGB_8888 format and take more memory
     * False if bitmap should be compressed by using RGB_565 format and take less memory
     */
    internal var isBestQuality: Boolean = false

    /**
     * True if annotations should be rendered
     * False otherwise
     */
    internal var isAnnotationRendering: Boolean = false

    /**
     * True if bitmap generation should be skipped during scrolling for better performance.
     * When true, only cached bitmaps are drawn during scrolling, and new bitmaps are generated
     * when scrolling stops. This improves scrolling performance but may show empty areas
     * when scrolling to new content.
     */
    private var isScrollOptimizationEnabled: Boolean = true

    /** Antialiasing and bitmap filtering  */
    private var isAntialiasing: Boolean = true
    private val antialiasFilter =
        PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** Paint for drawing placeholder pages during scroll optimization */
    private val placeholderPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    /** Paint for drawing placeholder page borders during scroll optimization */
    private val placeholderBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    /** Spacing between pages, in px  */
    private var spacingPx: Int = 0

    /** Add dynamic spacing to fit each page separately on the screen.  */
    private var isAutoSpacingEnabled: Boolean = false

    /** Fling a single page at a time  */
    internal var isPageFlingEnabled: Boolean = true

    val logWriter: LogWriter?
        get() = viewConfiguration.logWriter

    /** Construct the initial view  */
    init {
        renderingHandlerThread = HandlerThread("PDF renderer")
        if (!isInEditMode) {
            debugPaint.style = Paint.Style.STROKE
            setWillNotDraw(false)
        }
        pdfiumCore.setLogWriter(logWriter = object : LogWriter {
            override fun writeLog(message: String, tag: String) {
                logWriter?.writeLog(message, tag)
            }

            override fun writeLog(throwable: Throwable, tag: String) {
                logWriter?.writeLog(throwable, tag)
            }

        })
    }

    /**
     * Configure the PDF view with factory-time settings.
     * This should be called once when the view is created, typically in the factory block
     * of AndroidView in Compose or in the constructor/initialization of the view.
     */
    fun configure(configuration: PdfViewConfiguration) {
        this.viewConfiguration = configuration
        applyViewConfiguration()
    }

    private fun applyViewConfiguration() {
        isSwipeEnabled = viewConfiguration.enableSwipe
        setNightMode(viewConfiguration.nightMode)
        isDoubleTapEnabled = viewConfiguration.enableDoubleTap
        isSwipeVertical = !viewConfiguration.swipeHorizontal
        isAnnotationRendering = viewConfiguration.annotationRendering
        scrollHandle = viewConfiguration.scrollHandle
        isAntialiasing = viewConfiguration.antialiasing
        spacingPx = context.toPx(viewConfiguration.spacing)
        isAutoSpacingEnabled = viewConfiguration.autoSpacing
        pageFitPolicy = viewConfiguration.pageFitPolicy
        isFitEachPage = viewConfiguration.fitEachPage
        isPageSnap = viewConfiguration.pageSnap
        isPageFlingEnabled = viewConfiguration.pageFling
        isScrollOptimizationEnabled = viewConfiguration.scrollOptimization
        if (viewConfiguration.disableLongPress) dragPinchManager.disableLongPress()
        isBestQuality = false
    }

    /**
     * Load a PDF document with the specified load request.
     * This method can be called multiple times to load different documents or retry with different parameters.
     */
    fun load(loadRequest: PdfLoadRequest) {
        defaultPage = loadRequest.defaultPage
        currentDocumentLoadListener = loadRequest.documentLoadListener

        pendingLoadRequest = loadRequest

        // Check if view has valid dimensions before starting PDF loading
        when {
            width > 0 && height > 0 -> startLoading(loadRequest)
            else -> logWriter?.writeLog(
                "PDF loading deferred - view size not ready (${width}x${height})",
                "PDFView"
            )
        }
    }

    private fun startLoading(loadRequest: PdfLoadRequest) {
        // Prevent multiple concurrent operations
        if (isCurrentlyLoading) return

        currentLoadingJob?.cancelSafely()

        // Set global loading flag immediately
        isCurrentlyLoading = true

        logWriter?.writeLog("PDF loading started", "PDFView")

        try {
            currentLoadingJob = scope.async(Dispatchers.Main.immediate) {
                isLoading = true
                try {
                    recycle(false)
                    load(
                        pdfView = this@PDFView,
                        docSource = loadRequest.source,
                        password = loadRequest.password,
                        userPages = loadRequest.pageNumbers,
                        documentLoadListener = loadRequest.documentLoadListener
                    )
                } catch (e: Exception) {
                    logWriter?.writeLog("Error loading PDF: ${e.message}")
                } finally {
                    isLoading = false
                    isCurrentlyLoading = false
                    currentLoadingJob = null
                    pendingLoadRequest = null
                }
            }
        } catch (e: Exception) {
            isLoading = false
            isCurrentlyLoading = false
            currentLoadingJob = null
            pendingLoadRequest = null
            logWriter?.writeLog("Error starting PDF load: ${e.message}")
        }
    }

    private suspend fun load(
        pdfView: PDFView,
        docSource: DocumentSource,
        password: String?,
        userPages: List<Int>? = null,
        documentLoadListener: DocumentLoadListener? = null,
    ) {
        if (isRecycled) return

        // Validate that view has valid dimensions before proceeding with PDF loading
        if (pdfView.width <= 0 || pdfView.height <= 0) {
            logWriter?.writeLog(
                "Skipping PDF load due to invalid view dimensions: ${pdfView.width}x${pdfView.height}",
                "PDFView"
            )
            return
        }

        isRecycling = false
        isRecycled = false

        try {
            documentLoadListener?.onDocumentLoadingStart()
            val pdfFile = withContext(Dispatchers.IO) {
                if (isRecycling || isRecycled) return@withContext null

                docSource.createDocument(pdfView.context, pdfiumCore, password)

                if (isRecycling || isRecycled) return@withContext null

                PdfFile(
                    pdfiumCore = pdfiumCore,
                    pageFitPolicy = pageFitPolicy,
                    viewSize = Size(pdfView.width, pdfView.height),
                    originalUserPages = userPages,
                    isVertical = isSwipeVertical,
                    spacingPx = spacingPx,
                    autoSpacing = isAutoSpacingEnabled,
                    fitEachPage = isFitEachPage,
                    maxPageCacheSize = pdfViewerConfiguration.maxCachedPages,
                    singlePageMode = viewConfiguration.singlePageMode
                )
            }

            if (pdfFile == null) return
            if (isRecycling || isRecycled) return

            withContext(Dispatchers.Main) {
                if (isRecycling || isRecycled) return@withContext
                pdfView.loadComplete(pdfFile)
            }
        } catch (t: Throwable) {
            logWriter?.writeLog("PDF loading cancelled", "PDFView")
            withContext(Dispatchers.Main) {
                pdfView.loadError(t)
            }
        }
    }

    /**
     * Go to the given page.
     *
     * @param page Page index.
     * @param withAnimation Whether to animate the jump transition.
     * @param resetZoom If true, resets the zoom level to minimum (1f) before jumping to the page.
     */
    fun jumpTo(page: Int, withAnimation: Boolean = false, resetZoom: Boolean = false) {
        _pdfFile ?: return
        val userPage = pdfFile.determineValidPageNumberFrom(page)
        logWriter?.writeLog(
            "Jumping to page $userPage (requested: $page) with animation: $withAnimation, resetZoom: $resetZoom",
            "PDFView"
        )

        // Reset zoom if requested
        if (resetZoom && zoom != minZoom) {
            val oldZoom = zoom
            zoomTo(minZoom)
            viewConfiguration.zoomEventListener?.onZoomChanged(newZoom = minZoom, oldZoom = oldZoom)
        }

        val offset: Float = if (userPage == 0) 0F else -pdfFile.getPageOffset(userPage, zoom)
        when {
            isSwipeVertical -> when {
                withAnimation -> pdfAnimator.animateVertical(currentYOffset, offset)
                else -> moveTo(currentXOffset, offset)
            }

            else -> when {
                withAnimation -> pdfAnimator.animateHorizontal(currentXOffset, offset)
                else -> moveTo(offset, currentYOffset)
            }
        }
        showPage(userPage)
    }

    fun showPage(pageNb: Int) {
        if (isRecycled) return
        _pdfFile ?: return

        val userPage = pdfFile.determineValidPageNumberFrom(pageNb)
        val oldPage = currentPage
        currentPage = userPage

        // In single page mode, clear cache of the old page to prevent old parts from showing
        if (viewConfiguration.singlePageMode && oldPage != currentPage) {
            cacheManager.clearPageCache(oldPage)
        }

        loadPages()
        if (!documentFitsView()) scrollHandle?.setPageNum(currentPage + 1)
        viewConfiguration.pageNavigationEventListener?.onPageChanged(currentPage, pageCount)
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
        loadPageByOffset(skipBitmapGenerationDuringScroll = isScrollOptimizationEnabled && isScrollHandleDragging)
    }

    fun stopFling() = pdfAnimator.cancelFling()

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

    internal fun onPageError(ex: PageRenderingException) {
        viewConfiguration.renderingEventListener?.onPageFailedToRender(ex)
    }

    fun recycle() = recycle(true)

    private fun recycle(isRemoveRequest: Boolean = true) {
        currentLoadingJob?.cancelSafely()
        currentLoadingJob = null

        isRecycling = true
        isLoading = false
        // Only reset global loading flag if this is a full removal, not cleanup before loading
        if (isRemoveRequest) {
            isCurrentlyLoading = false
        }

        try {
            // Reset view configuration to default if removing entirely
            if (isRemoveRequest) viewConfiguration = PdfViewConfiguration.DEFAULT

            // Clear pending request when recycling
            if (isRemoveRequest) pendingLoadRequest = null

            pdfAnimator.cancelAllAnimations()
            dragPinchManager.disable()

            renderingHandler?.let { handler ->
                handler.stop()
                handler.removeMessages(RenderingHandler.MSG_RENDER_TASK)
            }

            cacheManager.recycle()
            if (isScrollHandleInit && isRemoveRequest) {
                try {
                    scrollHandle?.destroyLayout()
                } catch (_: Exception) {
                    // Ignore scroll handle destruction errors during recycling
                }
            }

            try {
                _pdfFile?.dispose()
            } catch (_: Exception) {
                // Ignore PDF disposal errors during recycling
            }

            _pdfFile = null
            renderingHandler = null
            if (isRemoveRequest) {
                scrollHandle = null
                isScrollHandleInit = false
            }
            currentYOffset = 0f
            currentXOffset = currentYOffset
            zoom = 1f
            state = State.DEFAULT
        } catch (e: Exception) {
            // Log but don't crash during recycling
            logWriter?.writeLog("Error during recycling: ${e.message}", "PDFView")
        } finally {
            // Ensure flags are set properly regardless of errors
            isRecycling = false
            // Only mark as recycled if this is a full removal, not just cleanup before loading
            if (isRemoveRequest) isRecycled = true
        }
    }

    /** Handle fling animation  */
    override fun computeScroll() {
        super.computeScroll()
        if (isInEditMode || isRecycled || isRecycling) return
        pdfAnimator.performFling()
    }

    override fun onDetachedFromWindow() {
        currentLoadingJob?.cancelSafely()
        currentLoadingJob = null
        scope.cancel()
        isLoading = false
        isCurrentlyLoading = false

        // Clear any pending PDF request
        pendingLoadRequest = null

        pdfAnimator.cancelAllAnimations()
        dragPinchManager.disable()

        renderingHandlerThread?.quitSafely()
        renderingHandlerThread = null

        recycle()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Check if we have a pending load request and valid dimensions now
        if (pendingLoadRequest != null && w > 0 && h > 0) {
            if (isCurrentlyLoading) {
                logWriter?.writeLog(
                    "PDF loading already in progress during size change, updating pending request",
                    "PDFView"
                )
                return
            }

            logWriter?.writeLog(
                "Starting deferred PDF loading with valid dimensions (${w}x${h})",
                "PDFView"
            )
            val request = pendingLoadRequest!!
            pendingLoadRequest = null
            load(request)
            return
        }

        // Handle size changes when PDF is already loaded
        if (isInEditMode || state != State.SHOWN) {
            // If we're in DEFAULT state and have valid dimensions, but no pending request,
            // this might be a configuration issue, but don't spam logs
            if (state == State.DEFAULT && w > 0 && h > 0 && pendingLoadRequest == null) {
                if (oldw == 0 || oldh == 0)
                    logWriter?.writeLog(
                        "Size changed to valid dimensions but no PDF loaded or pending. This may indicate the PDF needs to be reloaded.",
                        "PDFView"
                    )
            }
            return
        }

        // If PDF is already loaded, just recalculate layout for new size
        if (_pdfFile == null) return

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
        pdfAnimator.cancelAllAnimations()
        pdfFile.recalculatePageSizes(Size(w, h))
        if (isSwipeVertical) {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile.maxPageWidth + w * 0.5f
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile.getDocLen(zoom) + h * 0.5f
        } else {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile.getDocLen(zoom) + w * 0.5f
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile.maxPageHeight + h * 0.5f
        }
        moveTo(currentXOffset, currentYOffset)
        loadPageByOffset(skipBitmapGenerationDuringScroll = false)
        // Ensure scroll handle shows correct page number after orientation change
        updateScrollUIElements()
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

        // If scroll optimization is enabled and we're actively scrolling,
        // draw placeholder pages for better visual feedback
        if (isScrollOptimizationEnabled && isActivelyScrolling()) {
            drawScrollPlaceholders(canvas)
        }

        // Draws thumbnails
        for (part in cacheManager.getThumbnails()) drawPart(canvas, part)

        // Draws parts
        for (part in cacheManager.getPageParts()) drawPart(canvas, part)
        if (pdfViewerConfiguration.isDebugEnabled && viewConfiguration.renderingEventListener != null)
            drawWithListener(canvas, currentPage)

        // Restores the canvas position
        canvas.translate(-currentXOffset, -currentYOffset)
    }

    /**
     * Draw placeholder rectangles for pages during scrolling to provide visual feedback
     */
    private fun drawScrollPlaceholders(canvas: Canvas) {
        if (_pdfFile == null) return

        // Update paint colors based on current night mode
        placeholderPaint.color = if (nightMode) 0xFF2E2E2E.toInt() else 0xFFF5F5F5.toInt()
        placeholderBorderPaint.color = if (nightMode) 0xFF505050.toInt() else 0xFFDDDDDD.toInt()

        // Calculate visible area
        val visibleLeft = -currentXOffset
        val visibleTop = -currentYOffset
        val visibleRight = visibleLeft + width
        val visibleBottom = visibleTop + height

        // Draw placeholders for all pages that intersect with visible area
        for (pageIndex in 0 until pageCount) {
            val pageSize = pdfFile.getPageSize(pageIndex) ?: continue
            val scaledWidth = toCurrentScale(pageSize.width)
            val scaledHeight = toCurrentScale(pageSize.height)

            val pageLeft: Float
            val pageTop: Float

            when {
                isSwipeVertical -> {
                    pageLeft = (toCurrentScale(pdfFile.maxPageWidth) - scaledWidth) / 2
                    pageTop = pdfFile.getPageOffset(pageIndex, zoom)
                }

                else -> {
                    pageLeft = pdfFile.getPageOffset(pageIndex, zoom)
                    pageTop = (toCurrentScale(pdfFile.maxPageHeight) - scaledHeight) / 2
                }
            }

            val pageRight = pageLeft + scaledWidth
            val pageBottom = pageTop + scaledHeight

            // Check if page intersects with visible area
            if (pageRight >= visibleLeft && pageLeft <= visibleRight &&
                pageBottom >= visibleTop && pageTop <= visibleBottom
            ) {

                // Draw placeholder rectangle
                canvas.drawRect(pageLeft, pageTop, pageRight, pageBottom, placeholderPaint)
                canvas.drawRect(pageLeft, pageTop, pageRight, pageBottom, placeholderBorderPaint)
            }
        }
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
        viewConfiguration.renderingEventListener?.onDrawPage(
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
        when {
            isSwipeVertical -> {
                localTranslationY = pdfFile.getPageOffset(part.page, zoom)
                // Limit horizontal centering to prevent overlap in landscape mode
                val maxCenterOffset = (width - toCurrentScale(size.width)) / 2f
                localTranslationX = minOf(
                    toCurrentScale(pdfFile.maxPageWidth - size.width) / 2,
                    maxCenterOffset
                ).coerceAtLeast(0f)
            }

            else -> {
                localTranslationX = pdfFile.getPageOffset(part.page, zoom)
                localTranslationY = toCurrentScale(pdfFile.maxPageHeight - size.height) / 2
            }
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
        if (pdfViewerConfiguration.isDebugEnabled) {
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
    internal fun loadPages() {
        if (_pdfFile == null || renderingHandler == null || isRecycled || isRecycling) return

        // Skip bitmap generation if scroll optimization is enabled and we're actively scrolling
        if (isScrollOptimizationEnabled && isActivelyScrolling()) {
            android.util.Log.d("ScrollOptimization", "loadPages() skipped during active scrolling")
            return
        }

        // Dynamically enable best quality when zoomed in to reduce blur
        // Lower threshold for earlier quality improvement
        val wasBestQuality = isBestQuality
        isBestQuality = zoom > 1.2f

        // If quality setting changed to best quality, clear cache to force re-rendering
        if (!wasBestQuality && isBestQuality) when {
            viewConfiguration.singlePageMode -> cacheManager.clearPageCache(currentPage)

            else -> {
                // In multi-page mode, we could clear all caches, but for now just let it re-render naturally
                // cacheManager.makeANewSet() will handle cache management
            }
        }

        // Cancel all current tasks
        renderingHandler?.removeMessages(RenderingHandler.MSG_RENDER_TASK)
        cacheManager.makeANewSet()
        pagesLoader.loadPages()
        redraw()
    }

    /** Called when the PDF is loaded  */
    private fun loadComplete(pdfFile: PdfFile) {
        logWriter?.writeLog(
            "PDF loading finished successfully - ${pdfFile.pagesCount} pages",
            "PDFView"
        )

        if (isRecycling) return
        state = State.LOADED
        _pdfFile = pdfFile

        // Reset loading flags
        isLoading = false
        isCurrentlyLoading = false
        currentLoadingJob = null

        // Create RenderingHandler
        try {
            if (renderingHandlerThread?.isAlive == false) renderingHandlerThread?.start()
            renderingHandler = RenderingHandler(renderingHandlerThread!!.getLooper(), this)
            renderingHandler?.start()
        } catch (e: Exception) {
            loadError(e)
            return
        }

        // If a ScrollHandle is provided in the PdfRequest
        if (scrollHandle != null)
            try {
                scrollHandle?.setupLayout(this)
                isScrollHandleInit = true
            } catch (_: Exception) {
                logWriter?.writeLog("ScrollHandle setupLayout failed", "PDFView")
            }

        // Call to the on load complete listener if any
        try {
            dragPinchManager.enable()
            currentDocumentLoadListener?.onDocumentLoaded(pageCount)

            // Add a small delay to allow PDF native library to fully initialize page dimensions
            // Use a longer delay for large documents to ensure proper initialization
            val initDelay = when {
                pageCount > 1000 -> 200L  // Large documents need more time
                pageCount > 500 -> 150L   // Medium documents
                else -> 100L              // Small documents
            }

            postDelayed({
                if (!isRecycled && !isRecycling && _pdfFile != null)
                    jumpTo(defaultPage, false)
            }, initDelay)
        } catch (e: Exception) {
            loadError(e)
        }
    }

    private fun loadError(t: Throwable): Nothing? {
        logWriter?.writeLog("loadError: ${t.message}", "PDFView")

        if (isRecycling) return null
        state = State.ERROR

        // Ensure we reset loading flags properly
        isLoading = false
        isCurrentlyLoading = false
        currentLoadingJob = null

        currentDocumentLoadListener?.onDocumentLoadError(t)

        // Don't recycle if it's a password error - user will retry with password
        if (t is IncorrectPasswordException) return null

        // Force a redraw to clear any partial content
        recycle()
        invalidate()
        return null
    }

    internal fun redraw() {
        if (isRecycled || isRecycling) return
        invalidate()
    }

    /**
     * Called when a rendering task is over and
     * a PagePart has been freshly created.
     *
     * @param part The created PagePart.
     */
    internal fun onBitmapRendered(part: PagePart) {
        if (isRecycled || isRecycling) {
            part.renderedBitmap?.recycle()
            return
        }

        // when it is first rendered part
        if (state == State.LOADED) {
            state = State.SHOWN
            viewConfiguration.renderingEventListener?.onPageRendered(part.page)
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
    internal fun moveTo(offsetX: Float, offsetY: Float, moveHandle: Boolean = true) {
        if (isRecycled || isRecycling) return

        var offsetX1 = offsetX
        var offsetY1 = offsetY

        when {
            viewConfiguration.singlePageMode -> {
                // In single page mode, allow scrolling around zoomed pages but constrain to page boundaries
                val pageSize = pdfFile.getPageSize(currentPage) ?: SizeF(0f, 0f)
                val scaledPageWidth = toCurrentScale(pageSize.width)
                val scaledPageHeight = toCurrentScale(pageSize.height)

                // Check if page is larger than viewport in each dimension
                val pageWiderThanViewport = scaledPageWidth > width
                val pageTallerThanViewport = scaledPageHeight > height

                when {
                    // If page fits in viewport, center it
                    !pageWiderThanViewport && !pageTallerThanViewport -> {
                        offsetX1 = (width - scaledPageWidth) / 2f
                        offsetY1 = (height - scaledPageHeight) / 2f
                    }
                    // If page is wider than viewport, allow horizontal scrolling within bounds
                    pageWiderThanViewport && !pageTallerThanViewport -> {
                        offsetX1 = when {
                            offsetX1 > 0 -> 0f
                            offsetX1 + scaledPageWidth < width -> width - scaledPageWidth
                            else -> offsetX1
                        }
                        offsetY1 = (height - scaledPageHeight) / 2f
                    }
                    // If page is taller than viewport, allow vertical scrolling within bounds
                    !pageWiderThanViewport && pageTallerThanViewport -> {
                        offsetX1 = (width - scaledPageWidth) / 2f
                        offsetY1 = when {
                            offsetY1 > 0 -> 0f
                            offsetY1 + scaledPageHeight < height -> height - scaledPageHeight
                            else -> offsetY1
                        }
                    }
                    // If page is larger than viewport in both dimensions, allow scrolling in both directions
                    else -> {
                        offsetX1 = when {
                            offsetX1 > 0 -> 0f
                            offsetX1 + scaledPageWidth < width -> width - scaledPageWidth
                            else -> offsetX1
                        }
                        offsetY1 = when {
                            offsetY1 > 0 -> 0f
                            offsetY1 + scaledPageHeight < height -> height - scaledPageHeight
                            else -> offsetY1
                        }
                    }
                }
            }

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
            }
        }
        currentXOffset = offsetX1
        currentYOffset = offsetY1

        if (isScrollOptimizationEnabled && isActivelyScrolling()) {
            val positionOffset = positionOffset
            if (moveHandle && !documentFitsView()) {
                scrollHandle?.setScroll(positionOffset)
                val actualCurrentPage = when {
                    viewConfiguration.singlePageMode -> currentPage
                    else -> getPageAtPositionOffset(positionOffset)
                }
                scrollHandle?.setPageNum(actualCurrentPage + 1)
            }
            redraw()
        } else {
            val positionOffset = positionOffset
            if (moveHandle && !documentFitsView()) {
                scrollHandle?.setScroll(positionOffset)
                val actualCurrentPage = when {
                    viewConfiguration.singlePageMode -> currentPage
                    else -> getPageAtPositionOffset(positionOffset)
                }
                scrollHandle?.setPageNum(actualCurrentPage + 1)
            }
            viewConfiguration.pageNavigationEventListener?.onPageScrolled(
                page = currentPage,
                positionOffset = positionOffset
            )
            redraw()
        }
    }

    /**
     * Check if user is currently scrolling or flinging
     */
    internal fun isActivelyScrolling(): Boolean {
        return dragPinchManager.isScrolling || pdfAnimator.isFlinging || isScrollHandleDragging
    }

    /**
     * Set scroll handle dragging state - called by scroll handle implementations
     */
    internal fun setScrollHandleDragging(dragging: Boolean) {
        isScrollHandleDragging = dragging
    }

    /**
     * Update scroll handle and trigger navigation callbacks
     */
    internal fun updateScrollUIElements() {
        if (isRecycled || isRecycling) return

        if (!documentFitsView()) {
            val positionOffset = positionOffset
            scrollHandle?.setScroll(positionOffset)
            val actualCurrentPage = when {
                viewConfiguration.singlePageMode -> currentPage
                else -> getPageAtPositionOffset(positionOffset)
            }
            scrollHandle?.setPageNum(actualCurrentPage + 1)
        }
        viewConfiguration.pageNavigationEventListener?.onPageScrolled(currentPage, positionOffset)
    }

    internal fun loadPageByOffset() {
        loadPageByOffset(skipBitmapGenerationDuringScroll = isScrollOptimizationEnabled)
    }

    internal fun loadPageByOffset(skipBitmapGenerationDuringScroll: Boolean) {
        if (isRecycled || isRecycling || 0 == pageCount) return

        if (skipBitmapGenerationDuringScroll && isActivelyScrolling()) {
            redraw()
            return
        }

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
    internal fun performPageSnap() {
        if (!isPageSnap || _pdfFile == null || pageCount == 0) return
        val centerPage = findFocusPage(currentXOffset, currentYOffset)
        val edge = findSnapEdge(centerPage)
        if (edge === SnapEdge.NONE) return
        val offset = snapOffsetForPage(centerPage, edge)
        when {
            isSwipeVertical -> pdfAnimator.animateVertical(currentYOffset, -offset)
            else -> pdfAnimator.animateHorizontal(currentXOffset, -offset)
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

    internal fun findFocusPage(xOffset: Float, yOffset: Float): Int {
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
    internal fun pageFillsScreen(): Boolean {
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
     * Change the zoom level, relatively to a pivot point.
     * It will call moveTo() to make sure the given point stays
     * in the middle of the screen.
     *
     * @param zoom  The zoom level.
     * @param pivot The point on the screen that should stays.
     */
    fun zoomCenteredTo(zoom: Float, pivot: PointF) {
        val oldZoom = this.zoom
        val dzoom = zoom / this.zoom
        this.zoom = zoom
        var baseX = currentXOffset * dzoom
        var baseY = currentYOffset * dzoom
        baseX += pivot.x - pivot.x * dzoom
        baseY += pivot.y - pivot.y * dzoom
        moveTo(baseX, baseY)

        // Reload pages immediately if zoom changed significantly to reduce blur time
        val zoomChange = kotlin.math.abs(zoom - oldZoom) / oldZoom
        if (zoomChange > 0.3f) { // If zoom changed by more than 30%
            loadPages()
        }
    }

    /**
     * Change the zoom level
     */
    internal fun zoomTo(zoom: Float) {
        this.zoom = zoom
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
            logWriter?.writeLog("Cannot fit, document not rendered yet", TAG)
            return
        }
        val oldZoom = zoom
        val newZoom = width / pdfFile.getPageSize(page)?.width!!
        zoomTo(newZoom)
        if (oldZoom != newZoom)
            viewConfiguration.zoomEventListener?.onZoomChanged(newZoom = newZoom, oldZoom = oldZoom)
        jumpTo(page)
    }

    fun getPageSize(pageIndex: Int): SizeF = pdfFile.getPageSize(pageIndex) ?: SizeF(0F, 0F)

    internal fun toCurrentScale(size: Float): Float = size * zoom

    internal val isZooming: Boolean
        get() = zoom != minZoom

    internal fun resetZoomWithAnimation() = zoomWithAnimation(minZoom)

    fun zoomWithAnimation(centerX: Float, centerY: Float, scale: Float) =
        pdfAnimator.zoomToPoint(centerX, centerY, zoom, scale)

    fun zoomWithAnimation(scale: Float) {
        pdfAnimator.zoomToPoint(
            zoomCenterX = (width / 2).toFloat(),
            zoomCenterY = (height / 2).toFloat(),
            startZoom = zoom,
            targetZoom = scale
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

    /** Returns null if document is not loaded  */
    suspend fun getDocumentMeta(): Meta? = _pdfFile?.getMetaData()

    /** Will be empty until document is loaded  */
    suspend fun getTableOfContents(): List<Bookmark> = _pdfFile?.getBookmarks().orEmpty()

    /** Will be empty until document is loaded  */
    fun getLinks(page: Int): List<Link> = _pdfFile?.getPageLinks(page).orEmpty()

    internal fun callOnTap(e: MotionEvent): Boolean =
        viewConfiguration.gestureEventListener?.onTap(e) ?: false

    internal fun callOnLongPress(e: MotionEvent) {
        viewConfiguration.gestureEventListener?.onLongPress(e)
    }

    internal fun callLinkHandler(linkTapEvent: LinkTapEvent) {
        viewConfiguration.linkHandler?.handleLinkEvent(linkTapEvent) ?: DefaultLinkHandler(this)
    }

    /**
     * Notify listeners about zoom level changes.
     * This is called internally when zoom operations complete.
     *
     * @param newZoom The new zoom level.
     * @param oldZoom The previous zoom level.
     */
    internal fun notifyZoomChanged(newZoom: Float, oldZoom: Float) {
        if (newZoom != oldZoom) {
            viewConfiguration.zoomEventListener?.onZoomChanged(newZoom = newZoom, oldZoom = oldZoom)
        }
    }

    private fun Job.cancelSafely() {
        if (!isActive || isCompleted || isCancelled) return
        try {
            cancel()
        } catch (e: Exception) {
            logWriter?.writeLog("Error cancelling job: ${e.message}", "PDFView")
        }
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