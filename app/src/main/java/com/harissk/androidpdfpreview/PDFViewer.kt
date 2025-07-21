package com.harissk.androidpdfpreview

import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.harissk.pdfium.Meta
import com.harissk.pdfium.exception.PageRenderingException
import com.harissk.pdfium.listener.LogWriter
import com.harissk.pdfpreview.PDFView
import com.harissk.pdfpreview.link.LinkHandler
import com.harissk.pdfpreview.listener.DocumentLoadListener
import com.harissk.pdfpreview.listener.GestureEventListener
import com.harissk.pdfpreview.listener.PageNavigationEventListener
import com.harissk.pdfpreview.listener.RenderingEventListener
import com.harissk.pdfpreview.load
import com.harissk.pdfpreview.model.LinkTapEvent
import com.harissk.pdfpreview.scroll.DefaultScrollHandle

@Composable
internal fun PDFViewer(
    modifier: Modifier = Modifier,
    pdfDocument: PDFDocument,
    viewerSettings: PDFViewerSettings,
    onError: (String) -> Unit = {},
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            val pdfView = PDFView(context, null)
            Log.d("PDFPreview", "PDFViewer: factory called")
            Log.d("PDFPreview", "PDFViewer: file = ${pdfDocument.file.absolutePath}")
            pdfView.load(pdfDocument.file) {
                defaultPage(viewerSettings.defaultPage)
                swipeHorizontal(viewerSettings.swipeHorizontal)
                enableAnnotationRendering(viewerSettings.enableAnnotationRendering)
                scrollHandle(DefaultScrollHandle(pdfView.context))
                spacing(10F) // in dp
                scrollOptimization(true) // Enable scroll optimization for better performance

                documentLoadListener(object : DocumentLoadListener {
                    override fun onDocumentLoadingStart() {
                        Log.d("=====>", "onDocumentLoadingStart() called")
                    }

                    override fun onDocumentLoaded(totalPages: Int) {
                        Log.d("=====>", "onDocumentLoaded() called with: totalPages = $totalPages")

                        Log.d("=====>", "=============")
                        val meta: Meta? = pdfView.documentMeta
                        Log.d("=====>", "title = ${meta?.title}")
                        Log.d("=====>", "author = ${meta?.author}")
                        Log.d("=====>", "subject = ${meta?.subject}")
                        Log.d("=====>", "keywords = ${meta?.keywords}")
                        Log.d("=====>", "creator = ${meta?.creator}")
                        Log.d("=====>", "producer = ${meta?.producer}")
                        Log.d("=====>", "creationDate = ${meta?.creationDate}")
                        Log.d("=====>", "modDate = ${meta?.modDate}")
                        Log.d("=====>", "=============")

                        Log.d("=====>", "=============")
                        pdfView.tableOfContents.forEach { bookmark ->
                            Log.d("=====>", "${bookmark.pageIdx}  - ${bookmark.title}")
                            bookmark.children.forEach { child ->
                                Log.d("=====>", "${bookmark.pageIdx}:${child.pageIdx} - ${child.title}")
                            }
                        }
                        Log.d("=====>", "=============")
                    }

                    override fun onDocumentLoadError(error: Throwable) {
                        Log.d("=====>", "onDocumentLoadError() called with: error = $error")
                        onError(error.message.orEmpty())
                    }
                })
                renderingEventListener(object : RenderingEventListener {
                    override fun onPageRendered(pageNumber: Int) {
                        Log.d("=====>", "onPageRendered() called with: pageNumber = $pageNumber")
                    }

                    override fun onPageFailedToRender(pageRenderingException: PageRenderingException) {
                        Log.d(
                            "=====>",
                            "onPageFailedToRender() called with: pageRenderingException = $pageRenderingException"
                        )
                    }

                    override fun onDrawPage(
                        canvas: Canvas?,
                        pageWidth: Float,
                        pageHeight: Float,
                        displayedPage: Int,
                    ) {
                        Log.d(
                            "=====>",
                            "onDrawPage() called with: canvas = $canvas, pageWidth = $pageWidth, pageHeight = $pageHeight, displayedPage = $displayedPage"
                        )
                    }
                })
                pageNavigationEventListener(object : PageNavigationEventListener {
                    override fun onPageChanged(newPage: Int, pageCount: Int) {
                        Log.d(
                            "=====>",
                            "onPageChanged() called with: newPage = $newPage, pageCount = $pageCount"
                        )
                    }

                    override fun onPageScrolled(page: Int, positionOffset: Float) {
                        Log.d(
                            "=====>",
                            "onPageScrolled() called with: page = $page, positionOffset = $positionOffset"
                        )
                    }
                })
                gestureEventListener(object : GestureEventListener {
                    override fun onTap(motionEvent: MotionEvent): Boolean {
                        Log.d("=====>", "onTap() called with: motionEvent = $motionEvent")
                        return false
                    }

                    override fun onLongPress(motionEvent: MotionEvent) {
                        Log.d("=====>", "onLongPress() called with: motionEvent = $motionEvent")
                    }
                })
                linkHandler(object : LinkHandler {
                    override fun handleLinkEvent(event: LinkTapEvent) {
                        Log.d("=====>", "handleLinkEvent() called with: event = $event")
                    }
                })
                logWriter(object : LogWriter {
                    override fun writeLog(message: String, tag: String) {
                        Log.d(tag, message)
                    }

                    override fun writeLog(throwable: Throwable, tag: String) {
                        Log.e(tag, throwable.message.toString())
                    }
                })
            }
            pdfView
        }
    )
}