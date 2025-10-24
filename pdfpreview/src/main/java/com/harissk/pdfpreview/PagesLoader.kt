package com.harissk.pdfpreview

import android.graphics.RectF
import com.harissk.pdfium.exception.PageRenderingException
import com.harissk.pdfium.util.SizeF
import com.harissk.pdfpreview.utils.toPx
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
 * Handles page loading for a PDF view, pre-loading and caching parts of the document.
 */
internal class PagesLoader(private val pdfView: PDFView) {

    private var cacheOrder = 0
    private var xOffset = 0f
    private var yOffset = 0f
    private var pageRelativePartWidth = 0f
    private var pageRelativePartHeight = 0f
    private var partRenderWidth = 0f
    private var partRenderHeight = 0f

    private val thumbnailRect = RectF(0f, 0f, 1f, 1f)
    private val preloadOffset: Int =
        pdfView.context.toPx(pdfView.pdfViewerConfiguration.preloadMarginDp)

    private data class Holder(
        var row: Int = 0,
        var col: Int = 0,
    )

    private data class RenderRange(
        var page: Int = 0,
        var gridSize: GridSize = GridSize(),
        var leftTop: Holder = Holder(),
        var rightBottom: Holder = Holder(),
    )

    private data class GridSize(
        var rows: Int = 0,
        var cols: Int = 0,
    )

    private fun getPageColsRows(grid: GridSize, pageIndex: Int) {
        val size: SizeF = pdfView.pdfFile.getPageSize(pageIndex) ?: return

        // Validate page size
        if (size.width <= 0f || size.height <= 0f) {
            grid.rows = 1
            grid.cols = 1
            return
        }

        val ratioX: Float = 1f / size.width
        val ratioY: Float = 1f / size.height

        // Maintain tile size at higher zoom levels to prevent blur
        // Use a more aggressive scaling approach for better quality
        val zoomFactor = pdfView.zoom.coerceAtLeast(1f)
        val effectiveTileSize = pdfView.pdfViewerConfiguration.renderTileSize *
                when {
                    zoomFactor >= 3f -> zoomFactor * 1.5f  // Extra quality at high zoom
                    zoomFactor >= 2f -> zoomFactor * 1.25f // Good quality at medium zoom
                    zoomFactor > 1f -> zoomFactor          // Standard scaling
                    else -> 1f                              // Base quality
                }

        val partHeight: Float = effectiveTileSize * ratioY / pdfView.zoom
        val partWidth: Float = effectiveTileSize * ratioX / pdfView.zoom

        // Ensure valid calculations and prevent NaN/infinite values
        val calculatedRows = if (partHeight > 0f && !partHeight.isNaN()) {
            ceil(1f / partHeight).roundToInt().coerceAtLeast(1)
        } else 1

        val calculatedCols = if (partWidth > 0f && !partWidth.isNaN()) {
            ceil(1f / partWidth).roundToInt().coerceAtLeast(1)
        } else 1

        grid.rows = calculatedRows
        grid.cols = calculatedCols
    }

    private fun calculatePartSize(grid: GridSize) {
        // Ensure grid size is valid
        val validCols = grid.cols.coerceAtLeast(1)
        val validRows = grid.rows.coerceAtLeast(1)

        pageRelativePartWidth = 1f / validCols.toFloat()
        pageRelativePartHeight = 1f / validRows.toFloat()

        // Use effective tile size that maintains quality at higher zoom levels
        val zoomFactor = pdfView.zoom.coerceAtLeast(1f)
        val effectiveTileSize = pdfView.pdfViewerConfiguration.renderTileSize *
                when {
                    zoomFactor >= 3f -> zoomFactor * 1.5f  // Extra quality at high zoom
                    zoomFactor >= 2f -> zoomFactor * 1.25f // Good quality at medium zoom
                    zoomFactor > 1f -> zoomFactor          // Standard scaling
                    else -> 1f                              // Base quality
                }

        // Validate that pageRelativePartWidth and pageRelativePartHeight are not zero
        val safePageRelativePartWidth = pageRelativePartWidth.takeIf { it > 0f } ?: 1f
        val safePageRelativePartHeight = pageRelativePartHeight.takeIf { it > 0f } ?: 1f

        partRenderWidth = effectiveTileSize / safePageRelativePartWidth
        partRenderHeight = effectiveTileSize / safePageRelativePartHeight

        // Validate final results
        if (partRenderWidth.isNaN() || partRenderHeight.isNaN() || partRenderWidth <= 0 || partRenderHeight <= 0) {
            partRenderWidth = pdfView.pdfViewerConfiguration.renderTileSize.toFloat()
            partRenderHeight = pdfView.pdfViewerConfiguration.renderTileSize.toFloat()
        }
    }

    /**
     * Calculates the render range of each page.
     */
    private fun getRenderRangeList(
        firstXOffset: Float,
        firstYOffset: Float,
        lastXOffset: Float,
        lastYOffset: Float,
    ): List<RenderRange> = try {
        // Validate offsets to prevent NaN values
        if (firstXOffset.isNaN() || firstYOffset.isNaN() || lastXOffset.isNaN() || lastYOffset.isNaN()) {
            throw IllegalArgumentException("Offsets cannot be NaN")
        }

        val fixedFirstXOffset: Float = -firstXOffset.coerceAtMost(0F)
        val fixedFirstYOffset: Float = -firstYOffset.coerceAtMost(0F)
        val fixedLastXOffset: Float = -lastXOffset.coerceAtMost(0F)
        val fixedLastYOffset: Float = -lastYOffset.coerceAtMost(0F)
        val offsetFirst = if (pdfView.isSwipeVertical) fixedFirstYOffset else fixedFirstXOffset
        val offsetLast = if (pdfView.isSwipeVertical) fixedLastYOffset else fixedLastXOffset

        // Validate page indices
        val firstPage = pdfView.pdfFile.getPageAtOffset(offsetFirst, pdfView.zoom).coerceAtLeast(0)
        val lastPage = pdfView.pdfFile.getPageAtOffset(offsetLast, pdfView.zoom)
            .coerceAtMost(pdfView.pdfFile.pagesCount - 1)

        val pageCount = lastPage - firstPage + 1
        val renderRanges: MutableList<RenderRange> = LinkedList()
        for (page in firstPage..lastPage) {
            val range = RenderRange()
            range.page = page
            var pageFirstXOffset: Float
            var pageFirstYOffset: Float
            var pageLastXOffset: Float
            var pageLastYOffset: Float
            when (page) {
                firstPage -> {
                    pageFirstXOffset = fixedFirstXOffset
                    pageFirstYOffset = fixedFirstYOffset
                    when (pageCount) {
                        1 -> {
                            pageLastXOffset = fixedLastXOffset
                            pageLastYOffset = fixedLastYOffset
                        }

                        else -> {
                            val pageOffset = pdfView.pdfFile.getPageOffset(page, pdfView.zoom)
                            val pageSize: SizeF =
                                pdfView.pdfFile.getScaledPageSize(page, pdfView.zoom)
                            when {
                                pdfView.isSwipeVertical -> {
                                    pageLastXOffset = fixedLastXOffset
                                    pageLastYOffset = pageOffset + pageSize.height
                                }

                                else -> {
                                    pageLastYOffset = fixedLastYOffset
                                    pageLastXOffset = pageOffset + pageSize.width
                                }
                            }
                        }
                    }
                }

                lastPage -> {
                    val pageOffset = pdfView.pdfFile.getPageOffset(page, pdfView.zoom)
                    when {
                        pdfView.isSwipeVertical -> {
                            pageFirstXOffset = fixedFirstXOffset
                            pageFirstYOffset = pageOffset
                        }

                        else -> {
                            pageFirstYOffset = fixedFirstYOffset
                            pageFirstXOffset = pageOffset
                        }
                    }
                    pageLastXOffset = fixedLastXOffset
                    pageLastYOffset = fixedLastYOffset
                }

                else -> {
                    val pageOffset = pdfView.pdfFile.getPageOffset(page, pdfView.zoom)
                    val pageSize: SizeF = pdfView.pdfFile.getScaledPageSize(page, pdfView.zoom)
                    when {
                        pdfView.isSwipeVertical -> {
                            pageFirstXOffset = fixedFirstXOffset
                            pageFirstYOffset = pageOffset
                            pageLastXOffset = fixedLastXOffset
                            pageLastYOffset = pageOffset + pageSize.height
                        }

                        else -> {
                            pageFirstXOffset = pageOffset
                            pageFirstYOffset = fixedFirstYOffset
                            pageLastXOffset = pageOffset + pageSize.width
                            pageLastYOffset = fixedLastYOffset
                        }
                    }
                }
            }

            getPageColsRows(
                range.gridSize,
                range.page
            ) // get the page's grid size that rows and cols
            val scaledPageSize = pdfView.pdfFile.getScaledPageSize(range.page, pdfView.zoom)

            val rowHeight
                    : Float = scaledPageSize.height / range.gridSize.rows
            val colWidth
                    : Float = scaledPageSize.width / range.gridSize.cols

            // get the page offset int the whole file
            // ---------------------------------------
            // |            |           |            |
            // |<--offset-->|   (page)  |<--offset-->|
            // |            |           |            |
            // |            |           |            |
            // ---------------------------------------
            val secondaryOffset = pdfView.pdfFile.getSecondaryPageOffset(page, pdfView.zoom)

            // calculate the row,col of the point in the leftTop and rightBottom
            when {
                pdfView.isSwipeVertical -> {
                    range.leftTop.row = floor(
                        abs(
                            pageFirstYOffset - pdfView.pdfFile.getPageOffset(
                                range.page,
                                pdfView.zoom
                            )
                        ) / rowHeight
                    ).toInt()
                    range.leftTop.col = floor(
                        abs(pageFirstXOffset - secondaryOffset) / colWidth
                    ).toInt()
                    range.rightBottom.row = floor(
                        abs(
                            pageLastYOffset - pdfView.pdfFile.getPageOffset(
                                range.page,
                                pdfView.zoom
                            )
                        ) / rowHeight
                    ).toInt()
                    range.rightBottom.col = floor(
                        abs(pageLastXOffset - secondaryOffset) / colWidth
                    ).toInt()
                }

                else -> {
                    range.leftTop.row = floor(
                        abs(pageFirstYOffset - secondaryOffset) / rowHeight
                    ).toInt()
                    range.leftTop.col = floor(
                        abs(
                            pageFirstXOffset - pdfView.pdfFile.getPageOffset(
                                range.page,
                                pdfView.zoom
                            )
                        ) / colWidth
                    ).toInt()
                    range.rightBottom.row = floor(
                        abs(pageLastYOffset - secondaryOffset) / rowHeight
                    ).toInt()
                    range.rightBottom.col = floor(
                        abs(
                            pageLastXOffset - pdfView.pdfFile.getPageOffset(
                                range.page,
                                pdfView.zoom
                            )
                        ) / colWidth
                    ).toInt()
                }
            }
            renderRanges.add(range)
        }
        renderRanges
    } catch (e: Exception) {
        pdfView.onPageError(PageRenderingException(-1, e.cause ?: Throwable(e.message)))
        emptyList()
    }

    private fun loadVisible() {
        pdfView.logWriter?.writeLog("Loading visible pages", TAG)

        if (pdfView.pageCount <= 0) {
            pdfView.logWriter?.writeLog("No pages available, skipping load", TAG)
            return
        }

        when {
            pdfView.singlePageMode -> {
                // In single page mode, only load the current page
                try {
                    loadThumbnail(pdfView.currentPage)
                } catch (e: Exception) {
                    pdfView.logWriter?.writeLog(
                        "Failed to load thumbnail for current page ${pdfView.currentPage}: ${e.message}",
                        TAG
                    )
                }

                try {
                    val gridSize = GridSize(0, 0)
                    getPageColsRows(gridSize, pdfView.currentPage)
                    calculatePartSize(gridSize)
                    val partsToLoad = pdfView.pdfViewerConfiguration.maxCachedBitmaps
                    loadPage(
                        page = pdfView.currentPage,
                        firstRow = 0,
                        lastRow = gridSize.rows - 1,
                        firstCol = 0,
                        lastCol = gridSize.cols - 1,
                        nbOfPartsLoadable = partsToLoad
                    )
                } catch (e: Exception) {
                    pdfView.logWriter?.writeLog(
                        "Failed to load current page ${pdfView.currentPage}: ${e.message}",
                        TAG
                    )
                }
            }

            else -> {
                // Original multi-page loading logic
                val scaledPreloadOffset = preloadOffset.toFloat()
                val rangeList = getRenderRangeList(
                    firstXOffset = -xOffset + scaledPreloadOffset,
                    firstYOffset = -yOffset + scaledPreloadOffset,
                    lastXOffset = -xOffset - pdfView.width - scaledPreloadOffset,
                    lastYOffset = -yOffset - pdfView.height - scaledPreloadOffset
                )

                val maxRangesToProcess = 3
                val limitedRangeList = rangeList.take(maxRangesToProcess)

                for (range in limitedRangeList)
                    try {
                        loadThumbnail(range.page)
                    } catch (e: Exception) {
                        pdfView.logWriter?.writeLog(
                            "Failed to load thumbnail for page ${range.page}: ${e.message}",
                            TAG
                        )
                        continue
                    }

                var loadedParts = 0
                // Reduced from 50 to 40 to prevent queue overload with prioritized loading
                val maxPartsPerCall = 40

                for (range in limitedRangeList)
                    try {
                        calculatePartSize(range.gridSize)
                        val partsToLoad = minOf(
                            pdfView.pdfViewerConfiguration.maxCachedBitmaps - loadedParts,
                            maxPartsPerCall - loadedParts
                        )
                        if (partsToLoad <= 0) break

                        loadedParts += loadPage(
                            page = range.page,
                            firstRow = range.leftTop.row,
                            lastRow = range.rightBottom.row,
                            firstCol = range.leftTop.col,
                            lastCol = range.rightBottom.col,
                            nbOfPartsLoadable = partsToLoad
                        )

                        if (loadedParts >= maxPartsPerCall) {
                            pdfView.logWriter?.writeLog(
                                "Reached max parts limit ($maxPartsPerCall), deferring remaining load",
                                TAG
                            )
                            break
                        }
                    } catch (e: Exception) {
                        pdfView.logWriter?.writeLog(
                            "Failed to load page ${range.page}: ${e.message}",
                            TAG
                        )
                        continue
                    }
            }
        }
    }


    private fun loadPage(
        page: Int, firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int,
        nbOfPartsLoadable: Int,
    ): Int {
        // Calculate screen center in page coordinates for prioritized loading
        val screenCenterRow = (firstRow + lastRow) / 2f
        val screenCenterCol = (firstCol + lastCol) / 2f
        
        // Build list of tiles with their distance from screen center
        val cellsWithDistance = mutableListOf<Triple<Int, Int, Float>>()
        for (row in firstRow..lastRow) {
            for (col in firstCol..lastCol) {
                val distance = sqrt(
                    (row - screenCenterRow).pow(2) + 
                    (col - screenCenterCol).pow(2)
                )
                cellsWithDistance.add(Triple(row, col, distance))
            }
        }
        
        // Sort by distance - load center tiles first for better perceived performance
        cellsWithDistance.sortBy { it.third }
        
        // Load tiles in priority order (center to edges)
        var loaded = 0
        for ((row, col, _) in cellsWithDistance) {
            if (loadCell(page, row, col, pageRelativePartWidth, pageRelativePartHeight))
                loaded++
            if (loaded >= nbOfPartsLoadable) return loaded
        }
        return loaded
    }

    private fun loadCell(
        page: Int,
        row: Int,
        col: Int,
        pageRelativePartWidth: Float,
        pageRelativePartHeight: Float,
    ): Boolean {
        val relX = pageRelativePartWidth * col
        val relY = pageRelativePartHeight * row
        val relWidth = when {
            relX + pageRelativePartWidth > 1 -> 1 - relX
            else -> pageRelativePartWidth
        }
        val relHeight = when {
            relY + pageRelativePartHeight > 1 -> 1 - relY
            else -> pageRelativePartHeight
        }
        val renderWidth = partRenderWidth * relWidth
        val renderHeight = partRenderHeight * relHeight

        // Don't proceed if the render dimensions are zero
        if (renderWidth <= 0 || renderHeight <= 0) return false

        val pageRelativeBounds = RectF(relX, relY, relX + relWidth, relY + relHeight)
        if (!pdfView.cacheManager.upPartIfContained(page, pageRelativeBounds, cacheOrder))
            pdfView.renderingHandler?.addRenderingTask(
                page = page,
                width = renderWidth,
                height = renderHeight,
                bounds = pageRelativeBounds,
                thumbnail = false,
                cacheOrder = cacheOrder,
                bestQuality = pdfView.isBestQuality,
                annotationRendering = pdfView.isAnnotationRendering
            )
        cacheOrder++
        return true
    }

    private fun loadThumbnail(page: Int) {
        // Validate page index
        if (page < 0 || page >= pdfView.pageCount) {
            pdfView.logWriter?.writeLog("Invalid page index for thumbnail: $page", TAG)
            return
        }

        val pageSize: SizeF = pdfView.pdfFile.getPageSize(page) ?: return

        // Validate page size for thumbnails
        if (pageSize.width <= 0f || pageSize.height <= 0f) {
            pdfView.logWriter?.writeLog(
                "Invalid page size for thumbnail page $page: width=${pageSize.width}, height=${pageSize.height}",
                TAG
            )
            return
        }

        val thumbWidth = pageSize.width * pdfView.pdfViewerConfiguration.thumbnailQuality
        val thumbHeight = pageSize.height * pdfView.pdfViewerConfiguration.thumbnailQuality

        // Validate calculated thumbnail dimensions
        if (thumbWidth <= 0f || thumbHeight <= 0f || thumbWidth.isNaN() || thumbHeight.isNaN()) {
            pdfView.logWriter?.writeLog(
                "Invalid thumbnail dimensions for page $page: width=$thumbWidth, height=$thumbHeight",
                TAG
            )
            return
        }

        if (!pdfView.cacheManager.containsThumbnail(page, thumbnailRect))
            pdfView.renderingHandler?.addRenderingTask(
                page = page,
                width = thumbWidth,
                height = thumbHeight,
                bounds = thumbnailRect,
                thumbnail = true,
                cacheOrder = 0,
                bestQuality = pdfView.isBestQuality,
                annotationRendering = pdfView.isAnnotationRendering
            )
    }

    fun loadPages() {
        cacheOrder = 1
        xOffset = -pdfView.currentXOffset.coerceIn(-Float.MAX_VALUE, 0f)
        yOffset = -pdfView.currentYOffset.coerceIn(-Float.MAX_VALUE, 0f)
        loadVisible()
    }

    companion object {
        private const val TAG = "PagesLoader"
    }
}