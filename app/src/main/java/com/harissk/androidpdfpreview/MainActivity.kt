package com.harissk.androidpdfpreview

import android.content.ContentResolver.SCHEME_CONTENT
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.harissk.androidpdfpreview.databinding.ActivityMainBinding
import com.harissk.pdfium.Meta
import com.harissk.pdfpreview.PDFView
import com.harissk.pdfpreview.listener.OnLoadCompleteListener
import com.harissk.pdfpreview.listener.OnPageChangeListener
import com.harissk.pdfpreview.listener.OnPageErrorListener
import com.harissk.pdfpreview.scroll.DefaultScrollHandle
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random


class MainActivity : AppCompatActivity(), OnPageChangeListener, OnLoadCompleteListener,
    OnPageErrorListener {

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
        lifecycleScope.launch { loadPDF(binding.pdfView.fromAsset(assetFileName)) }
    }

    private fun displayFromUri(uri: Uri) {
        fileName = uri.fileName()
        lifecycleScope.launch { loadPDF(binding.pdfView.fromUri(uri)) }
    }

    private fun displayFromFile(file: File) {
        fileName = file.nameWithoutExtension
        lifecycleScope.launch { loadPDF(binding.pdfView.fromFile(file)) }
    }

    private suspend fun loadPDF(configurator: PDFView.Configurator) {
        configurator
            .defaultPage(0)
            .swipeHorizontal(Random.nextBoolean())
            .onPageChange(this)
            .enableAnnotationRendering(true)
            .onLoad(this)
            .scrollHandle(DefaultScrollHandle(this))
            .spacing(10F) // in dp
            .onPageError(this)
            .load()
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

    override fun loadComplete(nbPages: Int) {
        Log.d("=====>", "loadComplete() called with: nbPages = $nbPages")

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

    override fun onPageChanged(page: Int, pageCount: Int) {
        Log.d("=====>", "onPageChanged() called with: page = $page, pageCount = $pageCount")
    }

    override fun onPageError(page: Int, throwable: Throwable?) {
        Log.d("=====>", "onPageError() called with: page = $page, throwable = $throwable")
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
}