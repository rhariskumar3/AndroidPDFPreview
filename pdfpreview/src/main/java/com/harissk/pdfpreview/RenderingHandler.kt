package com.harissk.pdfpreview

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.harissk.pdfpreview.RenderingHandler.RenderingTask
import com.harissk.pdfpreview.exception.PageRenderingException
import com.harissk.pdfpreview.model.PagePart

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
                    running -> pdfView.post { pdfView.onBitmapRendered(part); }
                    else -> part.renderedBitmap?.recycle()
                }
            }
        } catch (ex: PageRenderingException) {
            pdfView.post { pdfView.onPageError(ex); }
        }
    }

    @Throws(PageRenderingException::class)
    private fun proceed(renderingTask: RenderingTask): PagePart? {
        val pdfFile: PdfFile = pdfView.pdfFile
        pdfFile.openPage(renderingTask.page)
        val w = Math.round(renderingTask.width)
        val h = Math.round(renderingTask.height)
        if (w == 0 || h == 0 || pdfFile.pageHasError(renderingTask.page)) {
            return null
        }
        val render: Bitmap = try {
            Bitmap.createBitmap(
                /* width = */ w,
                /* height = */ h,
                /* config = */ when {
                    renderingTask.bestQuality -> Bitmap.Config.ARGB_8888
                    else -> Bitmap.Config.RGB_565
                }
            )
        } catch (e: IllegalArgumentException) {
            pdfView.logWriter?.writeLog("Cannot create bitmap", TAG)
            return null
        }
        calculateBounds(w, h, renderingTask.bounds ?: RectF())
        pdfFile.renderPageBitmap(
            bitmap = render,
            pageIndex = renderingTask.page,
            bounds = roundedRenderBounds,
            annotationRendering = renderingTask.annotationRendering
        )
        return PagePart(
            page = renderingTask.page,
            renderedBitmap = render,
            pageRelativeBounds = renderingTask.bounds ?: RectF(),
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

    companion object {
        /**
         * [Message.what] kind of message this handler processes.
         */
        const val MSG_RENDER_TASK = 1
        private val TAG = RenderingHandler::class.java.getName()
    }
}