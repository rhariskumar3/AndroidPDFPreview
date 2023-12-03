package com.harissk.pdfpreview

import android.graphics.RectF
import com.harissk.pdfium.util.SizeF
import com.harissk.pdfpreview.utils.Constants
import com.harissk.pdfpreview.utils.Constants.Cache.CACHE_SIZE
import com.harissk.pdfpreview.utils.Constants.PRELOAD_OFFSET
import com.harissk.pdfpreview.utils.toPx
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


/**
 * Created by Harishkumar on 25/11/23.
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
    private val preloadOffset: Int = pdfView.context.toPx(PRELOAD_OFFSET)

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
        val ratioX: Float = 1f / size.width
        val ratioY: Float = 1f / size.height
        val partHeight: Float = Constants.PART_SIZE * ratioY / pdfView.zoom
        val partWidth: Float = Constants.PART_SIZE * ratioX / pdfView.zoom
        grid.rows = ceil(1f / partHeight).roundToInt()
        grid.cols = ceil(1f / partWidth).roundToInt()
    }

    private fun calculatePartSize(grid: GridSize) {
        pageRelativePartWidth = 1f / grid.cols.toFloat()
        pageRelativePartHeight = 1f / grid.rows.toFloat()
        partRenderWidth = Constants.PART_SIZE / pageRelativePartWidth
        partRenderHeight = Constants.PART_SIZE / pageRelativePartHeight
    }

    /**
     * calculate the render range of each page
     */
    private fun getRenderRangeList(
        firstXOffset: Float,
        firstYOffset: Float,
        lastXOffset: Float,
        lastYOffset: Float,
    ): List<RenderRange> {
        val fixedFirstXOffset: Float = -firstXOffset.coerceAtMost(0F)
        val fixedFirstYOffset: Float = -firstYOffset.coerceAtMost(0F)
        val fixedLastXOffset: Float = -lastXOffset.coerceAtMost(0F)
        val fixedLastYOffset: Float = -lastYOffset.coerceAtMost(0F)
        val offsetFirst = if (pdfView.isSwipeVertical) fixedFirstYOffset else fixedFirstXOffset
        val offsetLast = if (pdfView.isSwipeVertical) fixedLastYOffset else fixedLastXOffset
        val firstPage = pdfView.pdfFile.getPageAtOffset(offsetFirst, pdfView.zoom)
        val lastPage = pdfView.pdfFile.getPageAtOffset(offsetLast, pdfView.zoom)
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
                            val pageSize: SizeF = pdfView.pdfFile.getScaledPageSize(page, pdfView.zoom)
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
            val rowHeight: Float = scaledPageSize.height / range.gridSize.rows
            val colWidth: Float = scaledPageSize.width / range.gridSize.cols


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
                    ).roundToInt()
                    range.leftTop.col =
                        ((pageFirstXOffset - secondaryOffset).coerceAtLeast(0F) / colWidth).roundToInt()
                    range.rightBottom.row = ceil(
                        (pageLastYOffset - pdfView.pdfFile.getPageOffset(
                            range.page,
                            pdfView.zoom
                        )) / rowHeight
                    ).roundToInt()
                    range.rightBottom.col =
                        ((pageLastXOffset - secondaryOffset).coerceAtLeast(0F) / colWidth).roundToInt()
                }

                else -> {
                    range.leftTop.col = floor(
                        abs(
                            pageFirstXOffset - pdfView.pdfFile.getPageOffset(
                                range.page,
                                pdfView.zoom
                            )
                        ) / colWidth
                    ).roundToInt()
                    range.leftTop.row =
                        ((pageFirstYOffset - secondaryOffset).coerceAtLeast(0F) / rowHeight).roundToInt()
                    range.rightBottom.col = ((pageLastXOffset - pdfView.pdfFile.getPageOffset(
                        range.page,
                        pdfView.zoom
                    )) / colWidth).roundToInt()
                    range.rightBottom.row =
                        floor((pageLastYOffset - secondaryOffset).coerceAtLeast(0F) / rowHeight).roundToInt()
                }
            }
            renderRanges.add(range)
        }
        return renderRanges
    }

    private fun loadVisible() {
        val scaledPreloadOffset = preloadOffset.toFloat()
        val rangeList = getRenderRangeList(
            firstXOffset = -xOffset + scaledPreloadOffset,
            firstYOffset = -yOffset + scaledPreloadOffset,
            lastXOffset = -xOffset - pdfView.width - scaledPreloadOffset,
            lastYOffset = -yOffset - pdfView.height - scaledPreloadOffset
        )
        for (range in rangeList) loadThumbnail(range.page)

        var loadedParts = 0
        for (range in rangeList) {
            calculatePartSize(range.gridSize)
            loadedParts += loadPage(
                page = range.page,
                firstRow = range.leftTop.row,
                lastRow = range.rightBottom.row,
                firstCol = range.leftTop.col,
                lastCol = range.rightBottom.col,
                nbOfPartsLoadable = CACHE_SIZE - loadedParts
            )
            if (loadedParts >= CACHE_SIZE) break
        }
    }

    private fun loadPage(
        page: Int, firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int,
        nbOfPartsLoadable: Int,
    ): Int {
        var loaded = 0
        for (row in firstRow..lastRow) {
            for (col in firstCol..lastCol) {
                if (loadCell(page, row, col, pageRelativePartWidth, pageRelativePartHeight))
                    loaded++
                if (loaded >= nbOfPartsLoadable) return loaded
            }
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
            else -> pageRelativePartHeight
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
        val pageSize: SizeF = pdfView.pdfFile.getPageSize(page) ?: return
        if (!pdfView.cacheManager.containsThumbnail(page, thumbnailRect))
            pdfView.renderingHandler?.addRenderingTask(
                page = page,
                width = pageSize.width * Constants.THUMBNAIL_RATIO,
                height = pageSize.height * Constants.THUMBNAIL_RATIO,
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
}