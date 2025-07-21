package com.harissk.pdfpreview

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.harissk.pdfium.exception.PageRenderingException
import com.harissk.pdfpreview.model.PagePart
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

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
 * A [Handler] that will process incoming [RenderingTask] messages and alert
 * {@link PDFView.onBitmapRendered} when the portion of the PDF is ready to render.
 */
class RenderingHandler(looper: Looper, private val pdfView: PDFView) : Handler(looper) {

    private val renderBounds = RectF()
    private val roundedRenderBounds = Rect()
    private val renderMatrix: Matrix = Matrix()
    private var running = false

    companion object {
        const val MSG_RENDER_TASK = 1
        private const val TAG = "RenderingHandler"
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
        val msg: Message = obtainMessage(MSG_RENDER_TASK, task)
        sendMessage(msg)
    }

    override fun handleMessage(message: Message) {
        val task = message.obj as RenderingTask
        try {
            val part = proceed(task)
            if (part != null) {
                when {
                    running && !pdfView.isRecycled -> pdfView.post { pdfView.onBitmapRendered(part); }
                    else -> part.renderedBitmap?.recycle()
                }
            }
        } catch (ex: PageRenderingException) {
            if (!pdfView.isRecycled) {
                pdfView.post { pdfView.onPageError(ex); }
            }
        }
    }

    @Throws(PageRenderingException::class)
    private fun proceed(renderingTask: RenderingTask): PagePart? {
        if (pdfView.isRecycled || pdfView.isRecycling) return null
        if (!running) return null

        val pdfFile: PdfFile = pdfView.pdfFile

        // Synchronize access to PDF file to prevent concurrent operations
        synchronized(pdfFile) {
            // Double-check state after acquiring lock
            if (pdfView.isRecycled || pdfView.isRecycling || !running) return null

            pdfFile.openPage(renderingTask.page)
            val w = renderingTask.width.roundToInt()
            val h = renderingTask.height.roundToInt()
            if (w == 0 || h == 0 || pdfFile.pageHasError(renderingTask.page)) {
                return null
            }

            // Check again before creating bitmap
            if (pdfView.isRecycled || pdfView.isRecycling || !running) {
                return null
            }

            val render: Bitmap = try {
                createBitmap(
                    width = w,
                    height = h,
                    config = when {
                        renderingTask.bestQuality -> Bitmap.Config.ARGB_8888
                        else -> Bitmap.Config.RGB_565
                    }
                )
            } catch (_: IllegalArgumentException) {
                pdfView.logWriter?.writeLog("Cannot create bitmap", TAG)
                return null
            }

            // Final check before native rendering - this is where the crash occurs
            if (pdfView.isRecycled || pdfView.isRecycling || !running) {
                render.recycle()
                return null
            }

            try {
                calculateBounds(w, h, renderingTask.bounds ?: RectF())
                pdfFile.renderPageBitmap(
                    bitmap = render,
                    pageIndex = renderingTask.page,
                    bounds = roundedRenderBounds,
                    annotationRendering = renderingTask.annotationRendering
                )
            } catch (_: Exception) {
                render.recycle()
                return null
            }

            return PagePart(
                page = renderingTask.page,
                renderedBitmap = render,
                pageRelativeBounds = renderingTask.bounds ?: RectF(),
                isThumbnail = renderingTask.thumbnail,
                cacheOrder = renderingTask.cacheOrder
            )
        }
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
    }

    fun start() {
        running = true
    }

    private data class RenderingTask(
        var width: Float,
        var height: Float,
        var bounds: RectF?,
        var page: Int,
        var thumbnail: Boolean,
        var cacheOrder: Int,
        var bestQuality: Boolean,
        var annotationRendering: Boolean,
    )
}