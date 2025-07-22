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
package com.harissk.pdfpreview.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.harissk.pdfium.PdfiumCore
import com.harissk.pdfium.exception.PageRenderingException
import com.harissk.pdfium.util.Size
import com.harissk.pdfpreview.source.DocumentSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.math.min

/**
 * Utility class for generating PDF thumbnails from various document sources.
 *
 * This class provides static methods to generate thumbnails from PDF documents
 * using different sources like File, Uri, ByteArray, InputStream, or asset paths.
 * All operations are performed on background threads for optimal performance.
 *
 * Example usage:
 * ```kotlin
 * // Generate a simple thumbnail
 * val thumbnail = PDFThumbnailGenerator.generateThumbnail(context, file)
 *
 * // Generate with custom configuration
 * val config = ThumbnailConfig(
 *     width = 300,
 *     height = 400,
 *     quality = Bitmap.Config.ARGB_8888,
 *     annotationRendering = true
 * )
 * val thumbnail = PDFThumbnailGenerator.generateThumbnail(context, file, config = config)
 *
 * // Generate multiple thumbnails
 * val thumbnails = PDFThumbnailGenerator.generateThumbnails(
 *     context, file, listOf(0, 1, 2)
 * )
 * ```
 */
object PDFThumbnailGenerator {

    private val thumbnailCache = ThumbnailCache()

    /**
     * Generates a thumbnail for a specific page of a PDF document.
     *
     * @param context The application context.
     * @param source The PDF document source (File, Uri, ByteArray, InputStream, String asset path, or DocumentSource).
     * @param pageIndex The page index to generate thumbnail for (0-based). Default is 0.
     * @param config The thumbnail configuration. Default uses standard 200x200 settings.
     * @param password Optional password for encrypted PDFs.
     * @param useCache Whether to use caching for improved performance. Default is true.
     * @return The generated thumbnail bitmap, or null if generation failed.
     */
    suspend fun generateThumbnail(
        context: Context,
        source: Any,
        pageIndex: Int = 0,
        config: ThumbnailConfig = ThumbnailConfig(),
        password: String? = null,
        useCache: Boolean = true,
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val sourceIdentifier = generateSourceIdentifier(source)
            val cacheKey = ThumbnailCache.generateKey(sourceIdentifier, pageIndex, config)

            // Check cache first if enabled
            if (useCache) {
                thumbnailCache.get(cacheKey)?.let { cachedBitmap ->
                    if (!cachedBitmap.isRecycled) return@withContext cachedBitmap
                }
            }

            val documentSource = DocumentSource.toDocumentSource(source)
            val thumbnail =
                generateThumbnailInternal(context, documentSource, pageIndex, config, password)

            // Cache the result if successful and caching is enabled
            if (thumbnail != null && useCache) thumbnailCache.put(cacheKey, thumbnail)

            thumbnail
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates thumbnails for multiple pages of a PDF document.
     *
     * @param context The application context.
     * @param source The PDF document source.
     * @param pageIndices List of page indices to generate thumbnails for (0-based).
     * @param config The thumbnail configuration for all pages.
     * @param password Optional password for encrypted PDFs.
     * @param useCache Whether to use caching for improved performance. Default is true.
     * @return List of generated thumbnail bitmaps (null entries for failed generations).
     */
    suspend fun generateThumbnails(
        context: Context,
        source: Any,
        pageIndices: List<Int>,
        config: ThumbnailConfig = ThumbnailConfig(),
        password: String? = null,
        useCache: Boolean = true,
    ): List<Bitmap?> = withContext(Dispatchers.IO) {
        try {
            val documentSource = DocumentSource.toDocumentSource(source)
            val sourceIdentifier = generateSourceIdentifier(source)
            val results = mutableListOf<Bitmap?>()

            val pdfiumCore = PdfiumCore()

            try {
                documentSource.createDocument(context, pdfiumCore, password)

                for (pageIndex in pageIndices) {
                    val cacheKey = ThumbnailCache.generateKey(sourceIdentifier, pageIndex, config)

                    // Check cache first if enabled
                    val cachedBitmap = if (useCache) thumbnailCache.get(cacheKey) else null
                    if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                        results.add(cachedBitmap)
                        continue
                    }

                    val thumbnail = generatePageThumbnail(pdfiumCore, pageIndex, config)
                    results.add(thumbnail)

                    // Cache the result if successful and caching is enabled
                    if (thumbnail != null && useCache) {
                        thumbnailCache.put(cacheKey, thumbnail)
                    }
                }
            } finally {
                try {
                    pdfiumCore.close()
                } catch (_: Exception) {
                    // Ignore close errors
                }
            }

            results
        } catch (_: Exception) {
            pageIndices.map { null }
        }
    }

    /**
     * Gets the total number of pages in a PDF document.
     *
     * @param context The application context.
     * @param source The PDF document source.
     * @param password Optional password for encrypted PDFs.
     * @return The number of pages, or -1 if the document couldn't be opened.
     */
    suspend fun getPageCount(
        context: Context,
        source: Any,
        password: String? = null,
    ): Int = withContext(Dispatchers.IO) {
        try {
            val documentSource = DocumentSource.toDocumentSource(source)
            val pdfiumCore = PdfiumCore()

            try {
                documentSource.createDocument(context, pdfiumCore, password)
                pdfiumCore.pageCount
            } finally {
                try {
                    pdfiumCore.close()
                } catch (_: Exception) {
                    // Ignore close errors
                }
            }
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * Clears the thumbnail cache to free memory.
     */
    fun clearCache() {
        thumbnailCache.evictAll()
    }

    /**
     * Gets the current cache size in KB.
     */
    fun getCacheSize(): Int = thumbnailCache.size()

    /**
     * Internal method to generate a thumbnail for a specific document and page.
     */
    private suspend fun generateThumbnailInternal(
        context: Context,
        documentSource: DocumentSource,
        pageIndex: Int,
        config: ThumbnailConfig,
        password: String?,
    ): Bitmap? {
        val pdfiumCore = PdfiumCore()

        return try {
            documentSource.createDocument(context, pdfiumCore, password)
            generatePageThumbnail(pdfiumCore, pageIndex, config)
        } finally {
            try {
                pdfiumCore.close()
            } catch (_: Exception) {
                // Ignore close errors
            }
        }
    }

    /**
     * Generates a thumbnail for a specific page using an already opened document.
     */
    private fun generatePageThumbnail(
        pdfiumCore: PdfiumCore,
        pageIndex: Int,
        config: ThumbnailConfig,
    ): Bitmap? {
        return try {
            // Check if page index is valid
            val pageCount = pdfiumCore.pageCount
            if (pageIndex < 0 || pageIndex >= pageCount) return null

            // Open the page
            pdfiumCore.openPage(pageIndex)
            val pageSize = pdfiumCore.getPageSize(pageIndex)

            // Calculate thumbnail dimensions based on aspect ratio handling
            val (renderWidth, renderHeight, renderBounds) = calculateRenderDimensions(
                pageSize,
                config
            )

            // Create the thumbnail bitmap
            val thumbnail = createBitmap(config.width, config.height, config.quality)

            // Fill with background color if needed
            if (config.aspectRatio == AspectRatio.PRESERVE) {
                val canvas = Canvas(thumbnail)
                canvas.drawColor(config.backgroundColor)
            }

            // Create a bitmap for rendering the PDF content
            val renderBitmap = createBitmap(renderWidth, renderHeight, config.quality)

            try {
                // Render the PDF page to the render bitmap
                pdfiumCore.renderPageBitmap(
                    bitmap = renderBitmap,
                    pageIndex = pageIndex,
                    startX = 0,
                    startY = 0,
                    drawSizeX = renderWidth,
                    drawSizeY = renderHeight,
                    renderAnnot = config.annotationRendering
                )

                // Draw the rendered content to the final thumbnail if different sizes
                when {
                    renderWidth != config.width || renderHeight != config.height -> {
                        val canvas = Canvas(thumbnail)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                        canvas.drawBitmap(renderBitmap, null, renderBounds, paint)
                        renderBitmap.recycle()
                    }

                    else -> {
                        // If sizes match, return the render bitmap directly
                        renderBitmap.recycle()
                        return thumbnail
                    }
                }
            } catch (_: PageRenderingException) {
                renderBitmap.recycle()
                thumbnail.recycle()
                return null
            }

            // Close the page
            try {
                pdfiumCore.closePage(pageIndex)
            } catch (_: Exception) {
                // Ignore close errors
            }

            thumbnail
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculates the render dimensions and bounds based on the aspect ratio configuration.
     */
    private fun calculateRenderDimensions(
        pageSize: Size,
        config: ThumbnailConfig,
    ): Triple<Int, Int, RectF> {
        val pageAspectRatio = pageSize.width.toFloat() / pageSize.height.toFloat()
        val thumbnailAspectRatio = config.width.toFloat() / config.height.toFloat()

        return when (config.aspectRatio) {
            AspectRatio.STRETCH -> {
                Triple(
                    config.width,
                    config.height,
                    RectF(0f, 0f, config.width.toFloat(), config.height.toFloat())
                )
            }

            AspectRatio.FIT_WIDTH -> {
                val scaledHeight = (config.width / pageAspectRatio).toInt()
                val height = min(scaledHeight, config.height)
                val y = (config.height - height) / 2f
                Triple(config.width, height, RectF(0f, y, config.width.toFloat(), y + height))
            }

            AspectRatio.FIT_HEIGHT -> {
                val scaledWidth = (config.height * pageAspectRatio).toInt()
                val width = min(scaledWidth, config.width)
                val x = (config.width - width) / 2f
                Triple(width, config.height, RectF(x, 0f, x + width, config.height.toFloat()))
            }

            AspectRatio.PRESERVE -> {
                when {
                    pageAspectRatio > thumbnailAspectRatio -> {
                        // Page is wider, fit to width
                        val scaledHeight = (config.width / pageAspectRatio).toInt()
                        val y = (config.height - scaledHeight) / 2f
                        Triple(
                            config.width,
                            scaledHeight,
                            RectF(0f, y, config.width.toFloat(), y + scaledHeight)
                        )
                    }

                    else -> {
                        // Page is taller, fit to height
                        val scaledWidth = (config.height * pageAspectRatio).toInt()
                        val x = (config.width - scaledWidth) / 2f
                        Triple(
                            scaledWidth,
                            config.height,
                            RectF(x, 0f, x + scaledWidth, config.height.toFloat())
                        )
                    }
                }
            }

            AspectRatio.CROP_TO_FIT -> {
                when {
                    pageAspectRatio > thumbnailAspectRatio -> {
                        // Page is wider, fit to height and crop width
                        val scaledWidth = (config.height * pageAspectRatio).toInt()
                        Triple(
                            scaledWidth,
                            config.height,
                            RectF(0f, 0f, config.width.toFloat(), config.height.toFloat())
                        )
                    }

                    else -> {
                        // Page is taller, fit to width and crop height
                        val scaledHeight = (config.width / pageAspectRatio).toInt()
                        Triple(
                            config.width,
                            scaledHeight,
                            RectF(0f, 0f, config.width.toFloat(), config.height.toFloat())
                        )
                    }
                }
            }
        }
    }

    /**
     * Generates a unique identifier for a document source for caching purposes.
     */
    private fun generateSourceIdentifier(source: Any): String {
        return when (source) {
            is File -> "file_${source.absolutePath}_${source.lastModified()}_${source.length()}"
            is String -> "asset_${source.hashCode()}"
            is ByteArray -> "bytes_${source.contentHashCode()}"
            else -> {
                // For other sources, use a hash of the string representation
                val sourceString = source.toString()
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(sourceString.toByteArray())
                hashBytes.joinToString("") { "%02x".format(it) }.take(16)
            }
        }
    }
}
