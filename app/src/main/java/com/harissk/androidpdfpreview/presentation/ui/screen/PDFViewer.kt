package com.harissk.androidpdfpreview.presentation.ui.screen

import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.harissk.androidpdfpreview.domain.model.PDFDocument
import com.harissk.androidpdfpreview.presentation.model.ViewerSettings
import com.harissk.pdfium.exception.IncorrectPasswordException
import com.harissk.pdfium.exception.PageRenderingException
import com.harissk.pdfium.listener.LogWriter
import com.harissk.pdfpreview.PDFView
import com.harissk.pdfpreview.configureView
import com.harissk.pdfpreview.link.LinkHandler
import com.harissk.pdfpreview.listener.DocumentLoadListener
import com.harissk.pdfpreview.listener.GestureEventListener
import com.harissk.pdfpreview.listener.PageNavigationEventListener
import com.harissk.pdfpreview.listener.RenderingEventListener
import com.harissk.pdfpreview.loadDocument
import com.harissk.pdfpreview.model.LinkTapEvent
import com.harissk.pdfpreview.scroll.DefaultScrollHandle
import com.harissk.pdfpreview.utils.FitPolicy
import com.harissk.pdfpreview.validation.PDFDocumentValidator
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

@Composable
internal fun PDFViewer(
    modifier: Modifier = Modifier,
    pdfDocument: PDFDocument,
    viewerSettings: ViewerSettings,
    onError: (String) -> Unit = {},
    onFullScreenToggle: (() -> Unit)? = null,
    onPasswordRequired: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    var password by remember { mutableStateOf<String?>(null) }
    var isPasswordProtected by remember { mutableStateOf(false) }

    // Track if PDF has been loaded to prevent redundant loads during recomposition
    var hasLoadedPdf by remember(pdfDocument.file) { mutableStateOf(false) }

    // Track page information for FAB
    var totalPages by remember { mutableIntStateOf(0) }
    var pdfViewRef by remember { mutableStateOf<PDFView?>(null) }

    // Pre-validate if password is needed
    LaunchedEffect(pdfDocument.file) {
        isPasswordProtected = PDFDocumentValidator.isPasswordProtected(context, pdfDocument.file)
        if (isPasswordProtected && password == null) onPasswordRequired?.invoke()
    }

    // Function to jump to a random page
    val jumpToRandomPage: () -> Unit = {
        pdfViewRef?.let { pdfView ->
            if (totalPages > 1) {
                val randomPage = Random.nextInt(0, totalPages)
                pdfView.jumpTo(randomPage, withAnimation = true)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                Log.d("========>", "PDFViewer() AndroidView factory called")

                PDFView(viewContext, null).apply {
                    pdfViewRef = this

                    // Configure view-level settings once at factory time
                    configureView {
                        swipeHorizontal(viewerSettings.swipeHorizontal)
                        enableAnnotationRendering(viewerSettings.enableAnnotationRendering)
                        singlePageMode(viewerSettings.singlePageMode)
                        scrollHandle(DefaultScrollHandle(viewContext))
                        spacing(10F) // in dp
                        scrollOptimization(true) // Enable scroll optimization for better performance
                        pageFitPolicy(FitPolicy.BOTH)

                        renderingEventListener(object : RenderingEventListener {
                            override fun onPageRendered(pageNumber: Int) {
                                Log.d(
                                    "=====>",
                                    "onPageRendered() called with: pageNumber = $pageNumber"
                                )
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
                                totalPages = pageCount
                            }

                            override fun onPageScrolled(page: Int, positionOffset: Float) {
                                Log.d(
                                    "=====>",
                                    "onPageScrolled() called with: page = $page, positionOffset = $positionOffset"
                                )
                            }
                        })

                        gestureEventListener(object : GestureEventListener {
                            private var lastTapTime = 0L
                            private val doubleTapDelay = 300L // milliseconds

                            override fun onTap(motionEvent: MotionEvent): Boolean {
                                Log.d("=====>", "onTap() called with: motionEvent = $motionEvent")

                                // Check for double-tap to toggle fullscreen
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastTapTime < doubleTapDelay) {
                                    // Double-tap detected, toggle fullscreen
                                    onFullScreenToggle?.invoke()
                                    lastTapTime = 0L // Reset to prevent triple-tap detection
                                    return true // Consume the event
                                } else {
                                    lastTapTime = currentTime
                                }

                                return false
                            }

                            override fun onLongPress(motionEvent: MotionEvent) {
                                Log.d(
                                    "=====>",
                                    "onLongPress() called with: motionEvent = $motionEvent"
                                )
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
                }
            },
            update = { pdfView ->
                Log.d("========>", "PDFViewer() AndroidView update called with: pdfView = $pdfView")

                // Early return if PDF has already been loaded to prevent redundant loads during recompositions
                if (hasLoadedPdf) {
                    Log.d("========>", "Skipping PDF load - already loaded or in progress")
                    return@AndroidView
                }

                Log.d("========>", "Loading PDF document for the first time")

                // Load or reload document when data changes (runtime updates)
                pdfView.loadDocument(pdfDocument.file) {
                    defaultPage(viewerSettings.defaultPage)
                    password?.let { password(it) } // Set password if available

                    documentLoadListener(object : DocumentLoadListener {
                        override fun onDocumentLoadingStart() {
                            Log.d("=====>", "onDocumentLoadingStart() called")
                        }

                        override fun onDocumentLoaded(totalPages: Int) {
                            Log.d(
                                "=====>",
                                "onDocumentLoaded() called with: totalPages = $totalPages"
                            )

                            // Mark as loaded only after successful load
                            hasLoadedPdf = true

//                            coroutineScope.launch {
//                                pdfView.getDocumentMeta().let { meta ->
//                                    Log.d("=====>", "=============")
//                                    Log.d("=====>", "title = ${meta?.title}")
//                                    Log.d("=====>", "author = ${meta?.author}")
//                                    Log.d("=====>", "subject = ${meta?.subject}")
//                                    Log.d("=====>", "keywords = ${meta?.keywords}")
//                                    Log.d("=====>", "creator = ${meta?.creator}")
//                                    Log.d("=====>", "producer = ${meta?.producer}")
//                                    Log.d("=====>", "creationDate = ${meta?.creationDate}")
//                                    Log.d("=====>", "modDate = ${meta?.modDate}")
//                                    Log.d("=====>", "=============")
//
//                                    Log.d("=====>", "=============")
//                                    pdfView.getTableOfContents().let { bookmarks ->
//                                        bookmarks.forEach { bookmark ->
//                                            Log.d(
//                                                "=====>",
//                                                "${bookmark.pageIdx}  - ${bookmark.title}"
//                                            )
//                                            bookmark.children.forEach { child ->
//                                                Log.d(
//                                                    "=====>",
//                                                    "${bookmark.pageIdx}:${child.pageIdx} - ${child.title}"
//                                                )
//                                            }
//                                        }
//                                        Log.d("=====>", "=============")
//                                    }
//                                }
//                            }
                        }

                        override fun onDocumentLoadError(error: Throwable) {
                            Log.d("=====>", "onDocumentLoadError() called with: error = $error")
                            // Reset flag on error so user can retry
                            hasLoadedPdf = false

                            when (error) {
                                is IncorrectPasswordException -> onPasswordRequired?.invoke()
                                else -> onError(error.message.orEmpty())
                            }
                        }
                    })
                }
            }
        )

        // Random page FAB - only show when there are more than 1 page
        if (totalPages > 1) {
            FloatingActionButton(
                onClick = jumpToRandomPage,
                modifier = Modifier.align(Alignment.BottomEnd),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Jump to random page",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
