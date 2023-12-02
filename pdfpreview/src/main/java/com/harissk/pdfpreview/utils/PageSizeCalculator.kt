package com.harissk.pdfpreview.utils

import com.harissk.pdfium.util.Size
import com.harissk.pdfium.util.SizeF
import kotlin.math.floor

/**
 * Created by Harishkumar on 25/11/23.
 */

internal class PageSizeCalculator(
    private val fitPolicy: FitPolicy,
    private val originalMaxWidthPageSize: Size,
    private val originalMaxHeightPageSize: Size,
    private val viewSize: Size,
    private val fitEachPage: Boolean,
) {
    var optimalMaxWidthPageSize: SizeF? = null
        private set
    var optimalMaxHeightPageSize: SizeF? = null
        private set
    private var widthRatio = 0f
    private var heightRatio = 0f

    init {
        calculateMaxPages()
    }

    /**
     * Calculates the optimal page size for the given page size based on the fit policy.
     *
     * @param pageSize The original page size
     * @return The optimal page size for the given fit policy
     */
    fun calculate(pageSize: Size): SizeF {
        if (pageSize.width <= 0 || pageSize.height <= 0) return SizeF(0F, 0F)
        val maxWidth: Float = when {
            fitEachPage -> viewSize.width.toFloat()
            else -> pageSize.width * widthRatio
        }
        val maxHeight: Float = when {
            fitEachPage -> viewSize.height.toFloat()
            else -> pageSize.height * heightRatio
        }
        return when (fitPolicy) {
            FitPolicy.HEIGHT -> fitHeight(pageSize, maxHeight)
            FitPolicy.BOTH -> fitBoth(pageSize, maxWidth, maxHeight)
            else -> fitWidth(pageSize, maxWidth)
        }
    }

    /**
     * Calculates the optimal page sizes based on the fit policy, setting the `optimalMaxWidthPageSize`
     * and `optimalMaxHeightPageSize` properties accordingly.
     */
    private fun calculateMaxPages() {
        when (fitPolicy) {
            FitPolicy.HEIGHT -> {
                optimalMaxHeightPageSize =
                    fitHeight(originalMaxHeightPageSize, viewSize.height.toFloat())
                heightRatio =
                    (optimalMaxHeightPageSize?.height ?: 0F) / originalMaxHeightPageSize.height
                optimalMaxWidthPageSize = fitHeight(
                    originalMaxWidthPageSize,
                    originalMaxWidthPageSize.height * heightRatio
                )
            }

            FitPolicy.BOTH -> {
                val localOptimalMaxWidth: SizeF = fitBoth(
                    originalMaxWidthPageSize,
                    viewSize.width.toFloat(),
                    viewSize.height.toFloat()
                )
                val localWidthRatio = localOptimalMaxWidth.width / originalMaxWidthPageSize.width
                optimalMaxHeightPageSize = fitBoth(
                    originalMaxHeightPageSize,
                    originalMaxHeightPageSize.width * localWidthRatio,
                    viewSize.height.toFloat()
                )
                heightRatio =
                    (optimalMaxHeightPageSize?.height ?: 0F) / originalMaxHeightPageSize.height
                optimalMaxWidthPageSize = fitBoth(
                    originalMaxWidthPageSize,
                    viewSize.width.toFloat(),
                    originalMaxWidthPageSize.height * heightRatio
                )
                widthRatio = (optimalMaxWidthPageSize?.width ?: 0F) / originalMaxWidthPageSize.width
            }

            else -> {
                optimalMaxWidthPageSize =
                    fitWidth(originalMaxWidthPageSize, viewSize.width.toFloat())
                widthRatio = (optimalMaxWidthPageSize?.width ?: 0F) / originalMaxWidthPageSize.width
                optimalMaxHeightPageSize = fitWidth(
                    originalMaxHeightPageSize,
                    originalMaxHeightPageSize.width * widthRatio
                )
            }
        }
    }

    private fun fitWidth(pageSize: Size, maxWidth: Float): SizeF = SizeF(
        maxWidth,
        floor(maxWidth / (pageSize.width.toFloat() / pageSize.height.toFloat()))
    )

    private fun fitHeight(pageSize: Size, maxHeight: Float): SizeF = SizeF(
        floor(maxHeight / (pageSize.height.toFloat() / pageSize.width.toFloat())),
        maxHeight
    )

    private fun fitBoth(pageSize: Size, maxWidth: Float, maxHeight: Float): SizeF {
        val ratio = pageSize.width.toFloat() / pageSize.height.toFloat()
        var w: Float = maxWidth
        var h: Float = floor(maxWidth / ratio)
        if (h > maxHeight) {
            h = maxHeight
            w = floor(maxHeight * ratio)
        }
        return SizeF(w, h)
    }
}