package com.harissk.pdfpreview

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import com.harissk.pdfium.exception.PageRenderingException
import com.harissk.pdfpreview.model.PagePart
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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
 * Manages rendering tasks using a prioritized executor service.
 * It processes [RenderingTask] instances and posts results or errors
 * back to the main thread to update the [PDFView].
 */
class RenderingHandler(private val pdfView: PDFView) {

    private val renderBounds = RectF()
    private val roundedRenderBounds = Rect()
    private val renderMatrix: Matrix = Matrix()
    private var running = false

    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var executorService: ThreadPoolExecutor

    init {
        val corePoolSize = 1
        val maximumPoolSize = 1
        val keepAliveTime = 0L
        val unit = TimeUnit.MILLISECONDS
        val workQueue = PriorityBlockingQueue<Runnable>(25, RunnablePriorityComparator)
        executorService = ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue)
    }

    fun addRenderingTask(
        page: Int,
        width: Float,
        height: Float,
        bounds: RectF?,
        thumbnail: Boolean,
        cacheOrder: Int,
        bestQuality: Boolean,
        annotationRendering: Boolean,
    ) {
        val task = RenderingTask(
            width = width,
            height = height,
            bounds = bounds,
            page = page,
            thumbnail = thumbnail,
            cacheOrder = cacheOrder,
            bestQuality = bestQuality,
            annotationRendering = annotationRendering
        )
        val runnable = PrioritizedTaskRunnable(task, pdfView, this)
        if (isStillRunning()) {
            executorService.execute(runnable)
        }
    }

    @Throws(PageRenderingException::class)
    internal fun proceed(renderingTask: RenderingTask): PagePart? {
        val pdfFile: PdfFile = pdfView.pdfFile
        pdfFile.openPage(renderingTask.page) // Ensure page is open in PdfiumCore for this thread
        val w = Math.round(renderingTask.width)
        val h = Math.round(renderingTask.height)
        if (w == 0 || h == 0 || pdfFile.pageHasError(renderingTask.page)) {
            return null
        }
        val render: Bitmap = try {
            Bitmap.createBitmap(
                w,
                h,
                if (renderingTask.bestQuality) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            )
        } catch (e: IllegalArgumentException) {
            pdfView.logWriter?.writeLog("Cannot create bitmap: ${e.message}", TAG)
            return null
        }
        calculateBounds(w, h, renderingTask.bounds ?: RectF()) // Use RectF() if bounds is null
        pdfFile.renderPageBitmap(
            bitmap = render,
            pageIndex = renderingTask.page,
            bounds = roundedRenderBounds,
            annotationRendering = renderingTask.annotationRendering
        )
        return PagePart(
            page = renderingTask.page,
            renderedBitmap = render,
            pageRelativeBounds = renderingTask.bounds ?: RectF(), // Use RectF() if bounds is null
            isThumbnail = renderingTask.thumbnail,
            cacheOrder = renderingTask.cacheOrder
        )
    }

    private fun calculateBounds(width: Int, height: Int, pageSliceBounds: RectF) {
        renderMatrix.reset()
        renderMatrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height)
        renderMatrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height())
        renderBounds[0f, 0f, width.toFloat()] = height.toFloat()
        renderMatrix.mapRect(renderBounds)
        renderBounds.round(roundedRenderBounds)
    }

    fun stop() {
        running = false
        executorService.shutdownNow()
        pdfView.logWriter?.writeLog("RenderingHandler stopped, executor shutdown.", TAG)
        // Optionally:
        // try {
        //     if (!executorService.awaitTermination(50, TimeUnit.MILLISECONDS)) {
        //         pdfView.logWriter?.writeLog("Executor did not terminate in time.", TAG_ERROR)
        //     }
        // } catch (ie: InterruptedException) {
        //     Thread.currentThread().interrupt()
        //     pdfView.logWriter?.writeLog("Executor termination interrupted.", TAG_ERROR)
        // }
    }

    fun start() {
        running = true
        // If executor was shutdown and needs recreation, or if it's the first start.
        // However, given PDFView lifecycle, RenderingHandler is often recreated.
        // If executorService is shutdown, create a new one
        if (executorService.isShutdown || executorService.isTerminated) {
            val corePoolSize = 1
            val maximumPoolSize = 1
            val keepAliveTime = 0L
            val unit = TimeUnit.MILLISECONDS
            val workQueue = PriorityBlockingQueue<Runnable>(25, RunnablePriorityComparator)
            executorService = ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue)
            pdfView.logWriter?.writeLog("RenderingHandler started, new executor created.", TAG)
        } else {
            pdfView.logWriter?.writeLog("RenderingHandler started.", TAG)
        }
    }

    internal fun postResultToMainThread(part: PagePart) {
        if (isStillRunning()) {
            mainThreadHandler.post {
                if (isStillRunning()) { // Double check as state might change
                    pdfView.onBitmapRendered(part)
                } else {
                    part.renderedBitmap?.recycle()
                }
            }
        } else {
             part.renderedBitmap?.recycle()
        }
    }

    internal fun postErrorToMainThread(ex: PageRenderingException) {
        if (isStillRunning()) {
            mainThreadHandler.post {
                if (isStillRunning()) { // Double check
                    pdfView.onPageError(ex)
                }
            }
        }
    }

    internal fun isStillRunning(): Boolean = running && !executorService.isTerminated && !executorService.isShutdown

    internal data class RenderingTask(
        var width: Float,
        var height: Float,
        var bounds: RectF?,
        var page: Int,
        var thumbnail: Boolean,
        var cacheOrder: Int,
        var bestQuality: Boolean,
        var annotationRendering: Boolean,
    )

    companion object {
        private val TAG = RenderingHandler::class.java.simpleName
        // private val TAG_ERROR = RenderingHandler::class.java.simpleName + "_Error" // Optional for error specific logs
    }
}

// These should be top-level or in a separate file if preferred, but internal to the library module
internal class PrioritizedTaskRunnable(
    val task: RenderingHandler.RenderingTask,
    private val pdfView: PDFView,
    private val renderingHandler: RenderingHandler
) : Runnable {
    val priority: Int get() = task.cacheOrder

    override fun run() {
        try {
            if (!renderingHandler.isStillRunning()) {
                return
            }
            val part = renderingHandler.proceed(task)
            if (part != null) {
                if (renderingHandler.isStillRunning()) {
                    renderingHandler.postResultToMainThread(part)
                } else {
                    part.renderedBitmap?.recycle()
                }
            }
        } catch (ex: PageRenderingException) {
            if (renderingHandler.isStillRunning()) {
                renderingHandler.postErrorToMainThread(ex)
            }
        } catch (ex: Exception) { // Generic catch for other unexpected errors
            if (renderingHandler.isStillRunning()) {
                val page = task.page
                val genericError = PageRenderingException(page, IllegalStateException("Unknown error during rendering task for page $page", ex))
                renderingHandler.postErrorToMainThread(genericError)
            }
        }
    }
}

internal object RunnablePriorityComparator : Comparator<Runnable> {
    override fun compare(runnable1: Runnable, runnable2: Runnable): Int {
        return when {
            runnable1 is PrioritizedTaskRunnable && runnable2 is PrioritizedTaskRunnable ->
                runnable1.priority.compareTo(runnable2.priority)
            runnable1 is PrioritizedTaskRunnable -> -1 // Prioritize our tasks over others
            runnable2 is PrioritizedTaskRunnable -> 1  // Prioritize our tasks over others
            else -> 0
        }
    }
}