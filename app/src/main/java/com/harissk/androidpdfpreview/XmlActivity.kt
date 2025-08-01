/*
 * Copyright (C) 2025 [Haris Kumar R](https://github.com/rhariskumar3)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.harissk.androidpdfpreview

import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.harissk.androidpdfpreview.databinding.ActivityXmlBinding
import com.harissk.androidpdfpreview.presentation.model.ViewerSettings
import com.harissk.pdfium.exception.PageRenderingException
import com.harissk.pdfium.listener.LogWriter
import com.harissk.pdfpreview.link.LinkHandler
import com.harissk.pdfpreview.listener.DocumentLoadListener
import com.harissk.pdfpreview.listener.GestureEventListener
import com.harissk.pdfpreview.listener.PageNavigationEventListener
import com.harissk.pdfpreview.listener.RenderingEventListener
import com.harissk.pdfpreview.load
import com.harissk.pdfpreview.model.LinkTapEvent
import com.harissk.pdfpreview.scroll.DefaultScrollHandle
import java.io.File

class XmlActivity : AppCompatActivity() {

    private var isFullScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding =
            DataBindingUtil.setContentView<ActivityXmlBinding>(this, R.layout.activity_xml)
        val fileName = intent.getStringExtra("fileName")
        val filePath = intent.getStringExtra("filePath")
            ?: throw IllegalArgumentException("File path is required")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar(binding, fileName)
        setupFullscreenToggle(binding)
        setupPdfViewer(binding, filePath)
    }

    private fun setupToolbar(binding: ActivityXmlBinding, fileName: String?) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        binding.toolbar.title = fileName ?: "PDF Preview"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupFullscreenToggle(binding: ActivityXmlBinding) {
        updateFabIcon(binding)
        binding.fabFullscreen.setOnClickListener {
            toggleFullscreen()
            updateFabIcon(binding)
        }
    }

    private fun toggleFullscreen() {
        isFullScreen = !isFullScreen
        if (isFullScreen) {
            supportActionBar?.hide()
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        } else {
            supportActionBar?.show()
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun updateFabIcon(binding: ActivityXmlBinding) {
        val iconRes = if (isFullScreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
        binding.fabFullscreen.setImageResource(iconRes)
    }

    private fun setupPdfViewer(binding: ActivityXmlBinding, filePath: String) {
        val viewerSettings = ViewerSettings(
            defaultPage = 0,
            swipeHorizontal = false,
            enableAnnotationRendering = true
        )

        binding.pdfView.load(File(filePath)) {
            defaultPage(viewerSettings.defaultPage)
            swipeHorizontal(viewerSettings.swipeHorizontal)
            enableAnnotationRendering(viewerSettings.enableAnnotationRendering)
            scrollHandle(DefaultScrollHandle(this@XmlActivity))
            spacing(10F)
            scrollOptimization(true)
            documentLoadListener(createDocumentLoadListener(binding))
            renderingEventListener(createRenderingEventListener())
            pageNavigationEventListener(createPageNavigationEventListener())
            gestureEventListener(createGestureEventListener())
            linkHandler(createLinkHandler())
            logWriter(createLogWriter())
        }
    }

    private fun createDocumentLoadListener(binding: ActivityXmlBinding) =
        object : DocumentLoadListener {
            override fun onDocumentLoadingStart() {
                Log.d("XMLActivity", "Document loading started")
            }

            override fun onDocumentLoaded(totalPages: Int) {
                Log.d("XMLActivity", "Document loaded with $totalPages pages")
                logDocumentMeta(binding)
                logTableOfContents(binding)
            }

            override fun onDocumentLoadError(error: Throwable) {
                Log.e("XMLActivity", "Error loading document", error)
            }
        }

    private fun logDocumentMeta(binding: ActivityXmlBinding) {
        binding.pdfView.documentMeta?.let { meta ->
            Log.d("XMLActivity", "Document Meta: $meta")
        }
    }

    private fun logTableOfContents(binding: ActivityXmlBinding) {
        binding.pdfView.tableOfContents.forEach { bookmark ->
            Log.d("XMLActivity", "Bookmark: ${bookmark.pageIdx} - ${bookmark.title}")
            bookmark.children.forEach { child ->
                Log.d("XMLActivity", "Child Bookmark: ${child.pageIdx} - ${child.title}")
            }
        }
    }

    private fun createRenderingEventListener() = object : RenderingEventListener {
        override fun onPageRendered(pageNumber: Int) {
            Log.d("XMLActivity", "Page $pageNumber rendered")
        }

        override fun onPageFailedToRender(pageRenderingException: PageRenderingException) {
            Log.e("XMLActivity", "Failed to render page", pageRenderingException)
        }

        override fun onDrawPage(
            canvas: Canvas?,
            pageWidth: Float,
            pageHeight: Float,
            displayedPage: Int,
        ) {
            Log.d("XMLActivity", "Drawing page $displayedPage")
        }
    }

    private fun createPageNavigationEventListener() = object : PageNavigationEventListener {
        override fun onPageChanged(newPage: Int, pageCount: Int) {
            Log.d("XMLActivity", "Page changed to $newPage of $pageCount")
        }

        override fun onPageScrolled(page: Int, positionOffset: Float) {
            Log.d("XMLActivity", "Page $page scrolled with offset $positionOffset")
        }
    }

    private fun createGestureEventListener() = object : GestureEventListener {
        private var lastTapTime = 0L
        private val doubleTapDelay = 300L

        override fun onTap(motionEvent: MotionEvent): Boolean {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < doubleTapDelay) {
                toggleFullscreen()
                lastTapTime = 0L
                return true
            }
            lastTapTime = currentTime
            return false
        }

        override fun onLongPress(motionEvent: MotionEvent) {
            Log.d("XMLActivity", "Long press detected")
        }
    }

    private fun createLinkHandler() = object : LinkHandler {
        override fun handleLinkEvent(event: LinkTapEvent) {
            Log.d("XMLActivity", "Link tapped: $event")
        }
    }

    private fun createLogWriter() = object : LogWriter {
        override fun writeLog(message: String, tag: String) {
            Log.d(tag, message)
        }

        override fun writeLog(throwable: Throwable, tag: String) {
            Log.e(tag, "Error: ${throwable.message}", throwable)
        }
    }
}