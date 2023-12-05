package com.harissk.androidpdfpreview

import android.content.ContentResolver.SCHEME_CONTENT
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.databinding.DataBindingUtil
import com.harissk.androidpdfpreview.databinding.ActivityMainBinding
import com.harissk.pdfium.Meta
import com.harissk.pdfpreview.exception.PageRenderingException
import com.harissk.pdfpreview.link.LinkHandler
import com.harissk.pdfpreview.listener.DocumentLoadListener
import com.harissk.pdfpreview.listener.GestureEventListener
import com.harissk.pdfpreview.listener.PageNavigationEventListener
import com.harissk.pdfpreview.listener.RenderingEventListener
import com.harissk.pdfpreview.load
import com.harissk.pdfpreview.model.LinkTapEvent
import com.harissk.pdfpreview.scroll.DefaultScrollHandle
import java.io.File
import kotlin.random.Random


class MainActivity : AppCompatActivity(), DocumentLoadListener, RenderingEventListener,
    PageNavigationEventListener, GestureEventListener, LinkHandler {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) {
        Log.d("=====>", "pickMedia: $it")
        if (it != null) {
            when (it.scheme) {
                SCHEME_CONTENT -> displayFromUri(it)
                else -> displayFromFile(it.toFile())
            }
        }
    }

    private var fileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.toolbar)

        binding.pdfView.setBackgroundColor(Color.LTGRAY);
        displayFromAsset("sample.pdf")
    }

    private fun displayFromAsset(assetFileName: String) {
        fileName = assetFileName
        binding.pdfView.load(assetFileName) {
            defaultPage(0)
            swipeHorizontal(Random.nextBoolean())
            enableAnnotationRendering(true)
            scrollHandle(DefaultScrollHandle(this@MainActivity))
            spacing(10F) // in dp

            documentLoadListener(this@MainActivity)
            renderingEventListener(this@MainActivity)
            pageNavigationEventListener(this@MainActivity)
            gestureEventListener(this@MainActivity)
            linkHandler(this@MainActivity)
        }
    }

    private fun displayFromUri(uri: Uri) {
        fileName = uri.fileName()
        binding.pdfView.load(uri) {
            defaultPage(0)
            swipeHorizontal(Random.nextBoolean())
            enableAnnotationRendering(true)
            scrollHandle(DefaultScrollHandle(this@MainActivity))
            spacing(10F) // in dp

            documentLoadListener(this@MainActivity)
            renderingEventListener(this@MainActivity)
            pageNavigationEventListener(this@MainActivity)
            gestureEventListener(this@MainActivity)
            linkHandler(this@MainActivity)
        }
    }

    private fun displayFromFile(file: File) {
        fileName = file.nameWithoutExtension
        binding.pdfView.load(file) {
            defaultPage(0)
            swipeHorizontal(Random.nextBoolean())
            enableAnnotationRendering(true)
            scrollHandle(DefaultScrollHandle(this@MainActivity))
            spacing(10F) // in dp

            documentLoadListener(this@MainActivity)
            renderingEventListener(this@MainActivity)
            pageNavigationEventListener(this@MainActivity)
            gestureEventListener(this@MainActivity)
            linkHandler(this@MainActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_pick)
            pickMedia.launch("application/pdf")
        return super.onOptionsItemSelected(item)
    }

    private fun Uri.fileName(): String {
        var result = ""
        if (scheme == SCHEME_CONTENT) {
            contentResolver.query(this, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index > 0) result = cursor.getString(index)
                }
            }
        }
        if (result.isEmpty()) result = lastPathSegment.orEmpty()
        return result
    }

    override fun onDocumentLoadingStart() {
        Log.d("=====>", "onDocumentLoadingStart() called")
    }

    override fun onDocumentLoaded(totalPages: Int) {
        Log.d("=====>", "onDocumentLoaded() called with: totalPages = $totalPages")

        Log.d("=====>", "=============")
        val meta: Meta? = binding.pdfView.documentMeta
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
        binding.pdfView.tableOfContents.forEach { bookmark ->
            Log.d("=====>", "${bookmark.pageIdx}  - ${bookmark.title}")
            bookmark.children.forEach {
                Log.d("=====>", "${bookmark.pageIdx}:${it.pageIdx} - ${it.title}")
            }
        }
        Log.d("=====>", "=============")

        if (fileName.isNullOrEmpty())
            fileName = meta?.title.orEmpty().ifEmpty { "PDF Preview" }
        binding.toolbar.subtitle = fileName
    }

    override fun onDocumentLoadError(error: Throwable) {
        Log.d("=====>", "onDocumentLoadError() called with: error = $error")
        binding.toolbar.subtitle = error.message.orEmpty().ifEmpty { "PDF Preview" }
    }

    override fun onPageChanged(newPage: Int, pageCount: Int) {
        Log.d("=====>", "onPageChanged() called with: newPage = $newPage, pageCount = $pageCount")
    }

    override fun onPageScrolled(page: Int, positionOffset: Float) {
        Log.d(
            "=====>",
            "onPageScrolled() called with: page = $page, positionOffset = $positionOffset"
        )
    }

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

    override fun onTap(motionEvent: MotionEvent): Boolean {
        Log.d("=====>", "onTap() called with: motionEvent = $motionEvent")
        return true
    }

    override fun onLongPress(motionEvent: MotionEvent) {
        Log.d("=====>", "onLongPress() called with: motionEvent = $motionEvent")
    }

    override fun handleLinkEvent(event: LinkTapEvent) {
        Log.d("=====>", "handleLinkEvent() called with: event = $event")
    }
}