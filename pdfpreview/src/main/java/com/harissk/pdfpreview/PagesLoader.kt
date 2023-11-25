package com.harissk.pdfpreview

import android.graphics.RectF
import com.harissk.pdfpreview.utils.Constants
import com.harissk.pdfpreview.utils.Constants.Cache.CACHE_SIZE
import com.harissk.pdfpreview.utils.Constants.PRELOAD_OFFSET
import com.harissk.pdfpreview.utils.MathUtils
import com.harissk.pdfpreview.utils.Util.getDP
import com.shockwave.pdfium.util.SizeF
import java.util.LinkedList
import kotlin.math.abs


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
    private val preloadOffset: Int = getDP(pdfView.context, PRELOAD_OFFSET)

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
        val size: SizeF = pdfView.pdfFile!!.getPageSize(pageIndex)
        val ratioX: Float = 1f / size.getWidth()
        val ratioY: Float = 1f / size.getHeight()
        val partHeight: Float = Constants.PART_SIZE * ratioY / pdfView.zoom
        val partWidth: Float = Constants.PART_SIZE * ratioX / pdfView.zoom
        grid.rows = MathUtils.ceil(1f / partHeight)
        grid.cols = MathUtils.ceil(1f / partWidth)
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
        val fixedFirstXOffset: Float = -MathUtils.max(firstXOffset, 0F)
        val fixedFirstYOffset: Float = -MathUtils.max(firstYOffset, 0F)
        val fixedLastXOffset: Float = -MathUtils.max(lastXOffset, 0F)
        val fixedLastYOffset: Float = -MathUtils.max(lastYOffset, 0F)
        val offsetFirst = if (pdfView.isSwipeVertical) fixedFirstYOffset else fixedFirstXOffset
        val offsetLast = if (pdfView.isSwipeVertical) fixedLastYOffset else fixedLastXOffset
        val firstPage = pdfView.pdfFile!!.getPageAtOffset(offsetFirst, pdfView.zoom)
        val lastPage = pdfView.pdfFile!!.getPageAtOffset(offsetLast, pdfView.zoom)
        val pageCount = lastPage - firstPage + 1
        val renderRanges: MutableList<RenderRange> = LinkedList()
        for (page in firstPage..lastPage) {
            val range = RenderRange()
            range.page = page
            var pageFirstXOffset: Float
            var pageFirstYOffset: Float
            var pageLastXOffset: Float
            var pageLastYOffset: Float
            if (page == firstPage) {
                pageFirstXOffset = fixedFirstXOffset
                pageFirstYOffset = fixedFirstYOffset
                if (pageCount == 1) {
                    pageLastXOffset = fixedLastXOffset
                    pageLastYOffset = fixedLastYOffset
                } else {
                    val pageOffset = pdfView.pdfFile!!.getPageOffset(page, pdfView.zoom)
                    val pageSize: SizeF = pdfView.pdfFile!!.getScaledPageSize(page, pdfView.zoom)
                    if (pdfView.isSwipeVertical) {
                        pageLastXOffset = fixedLastXOffset
                        pageLastYOffset = pageOffset + pageSize.getHeight()
                    } else {
                        pageLastYOffset = fixedLastYOffset
                        pageLastXOffset = pageOffset + pageSize.getWidth()
                    }
                }
            } else if (page == lastPage) {
                val pageOffset = pdfView.pdfFile!!.getPageOffset(page, pdfView.zoom)
                if (pdfView.isSwipeVertical) {
                    pageFirstXOffset = fixedFirstXOffset
                    pageFirstYOffset = pageOffset
                } else {
                    pageFirstYOffset = fixedFirstYOffset
                    pageFirstXOffset = pageOffset
                }
                pageLastXOffset = fixedLastXOffset
                pageLastYOffset = fixedLastYOffset
            } else {
                val pageOffset = pdfView.pdfFile!!.getPageOffset(page, pdfView.zoom)
                val pageSize: SizeF = pdfView.pdfFile!!.getScaledPageSize(page, pdfView.zoom)
                if (pdfView.isSwipeVertical) {
                    pageFirstXOffset = fixedFirstXOffset
                    pageFirstYOffset = pageOffset
                    pageLastXOffset = fixedLastXOffset
                    pageLastYOffset = pageOffset + pageSize.getHeight()
                } else {
                    pageFirstXOffset = pageOffset
                    pageFirstYOffset = fixedFirstYOffset
                    pageLastXOffset = pageOffset + pageSize.getWidth()
                    pageLastYOffset = fixedLastYOffset
                }
            }
            getPageColsRows(
                range.gridSize,
                range.page
            ) // get the page's grid size that rows and cols
            val scaledPageSize: SizeF =
                pdfView.pdfFile!!.getScaledPageSize(range.page, pdfView.zoom)
            val rowHeight: Float = scaledPageSize.getHeight() / range.gridSize.rows
            val colWidth: Float = scaledPageSize.getWidth() / range.gridSize.cols


            // get the page offset int the whole file
            // ---------------------------------------
            // |            |           |            |
            // |<--offset-->|   (page)  |<--offset-->|
            // |            |           |            |
            // |            |           |            |
            // ---------------------------------------
            val secondaryOffset = pdfView.pdfFile!!.getSecondaryPageOffset(page, pdfView.zoom)

            // calculate the row,col of the point in the leftTop and rightBottom
            if (pdfView.isSwipeVertical) {
                range.leftTop.row = MathUtils.floor(
                    abs(
                        (pageFirstYOffset - pdfView.pdfFile!!.getPageOffset(
                            range.page,
                            pdfView.zoom
                        ))
                    ) / rowHeight
                )
                range.leftTop.col =
                    MathUtils.floor(MathUtils.min(pageFirstXOffset - secondaryOffset, 0F) / colWidth)
                range.rightBottom.row = MathUtils.ceil(
                    abs(
                        (pageLastYOffset - pdfView.pdfFile!!.getPageOffset(
                            range.page,
                            pdfView.zoom
                        ))
                    ) / rowHeight
                )
                range.rightBottom.col =
                    MathUtils.floor(MathUtils.min(pageLastXOffset - secondaryOffset, 0F) / colWidth)
            } else {
                range.leftTop.col = MathUtils.floor(
                    abs(
                        (pageFirstXOffset - pdfView.pdfFile!!.getPageOffset(
                            range.page,
                            pdfView.zoom
                        ))
                    ) / colWidth
                )
                range.leftTop.row = MathUtils.floor(
                    MathUtils.min(
                        pageFirstYOffset - secondaryOffset,
                        0F
                    ) / rowHeight
                )
                range.rightBottom.col = MathUtils.floor(
                    abs(
                        (pageLastXOffset - pdfView.pdfFile!!.getPageOffset(
                            range.page,
                            pdfView.zoom
                        ))
                    ) / colWidth
                )
                range.rightBottom.row =
                    MathUtils.floor(MathUtils.min(pageLastYOffset - secondaryOffset, 0F) / rowHeight)
            }
            renderRanges.add(range)
        }
        return renderRanges
    }

    private fun loadVisible() {
        var parts = 0
        val scaledPreloadOffset = preloadOffset.toFloat()
        val firstXOffset = -xOffset + scaledPreloadOffset
        val lastXOffset = -xOffset - pdfView.width - scaledPreloadOffset
        val firstYOffset = -yOffset + scaledPreloadOffset
        val lastYOffset = -yOffset - pdfView.height - scaledPreloadOffset
        val rangeList = getRenderRangeList(firstXOffset, firstYOffset, lastXOffset, lastYOffset)
        for (range in rangeList) {
            loadThumbnail(range.page)
        }
        for (range in rangeList) {
            calculatePartSize(range.gridSize)
            parts += loadPage(
                range.page,
                range.leftTop.row,
                range.rightBottom.row,
                range.leftTop.col,
                range.rightBottom.col,
                CACHE_SIZE - parts
            )
            if (parts >= CACHE_SIZE) {
                break
            }
        }
    }

    private fun loadPage(
        page: Int, firstRow: Int, lastRow: Int, firstCol: Int, lastCol: Int,
        nbOfPartsLoadable: Int,
    ): Int {
        var loaded = 0
        for (row in firstRow..lastRow) {
            for (col in firstCol..lastCol) {
                if (loadCell(page, row, col, pageRelativePartWidth, pageRelativePartHeight)) {
                    loaded++
                }
                if (loaded >= nbOfPartsLoadable) {
                    return loaded
                }
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
        var relWidth = pageRelativePartWidth
        var relHeight = pageRelativePartHeight
        var renderWidth = partRenderWidth
        var renderHeight = partRenderHeight
        if (relX + relWidth > 1) {
            relWidth = 1 - relX
        }
        if (relY + relHeight > 1) {
            relHeight = 1 - relY
        }
        renderWidth *= relWidth
        renderHeight *= relHeight
        val pageRelativeBounds = RectF(relX, relY, relX + relWidth, relY + relHeight)
        if (renderWidth > 0 && renderHeight > 0) {
            if (!pdfView.cacheManager.upPartIfContained(page, pageRelativeBounds, cacheOrder)) {
                pdfView.renderingHandler!!.addRenderingTask(
                    page, renderWidth, renderHeight,
                    pageRelativeBounds, false, cacheOrder, pdfView.isBestQuality,
                    pdfView.isAnnotationRendering
                )
            }
            cacheOrder++
            return true
        }
        return false
    }

    private fun loadThumbnail(page: Int) {
        val pageSize: SizeF = pdfView.pdfFile!!.getPageSize(page)
        val thumbnailWidth: Float = pageSize.getWidth() * Constants.THUMBNAIL_RATIO
        val thumbnailHeight: Float = pageSize.getHeight() * Constants.THUMBNAIL_RATIO
        if (!pdfView.cacheManager.containsThumbnail(page, thumbnailRect)) {
            pdfView.renderingHandler!!.addRenderingTask(
                page,
                thumbnailWidth, thumbnailHeight, thumbnailRect,
                true, 0, pdfView.isBestQuality, pdfView.isAnnotationRendering
            )
        }
    }

    fun loadPages() {
        cacheOrder = 1
        xOffset = -MathUtils.max(pdfView.currentXOffset, 0F)
        yOffset = -MathUtils.max(pdfView.currentYOffset, 0F)
        loadVisible()
    }
}