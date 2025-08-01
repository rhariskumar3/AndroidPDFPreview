package com.harissk.pdfpreview.utils

import com.harissk.pdfium.util.Size
import com.harissk.pdfium.util.SizeF
import kotlin.math.floor

/**
 * Copyright [2024] [Haris Kumar R](https://github.com/rhariskumar3)
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
 * A utility class that helps to calculate the optimal page size for PDF preview based on
 * different fitting policies.
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

    private fun fitWidth(pageSize: Size, maxWidth: Float): SizeF {
        if (pageSize.width <= 0 || pageSize.height <= 0 || maxWidth <= 0f) {
            return SizeF(0F, 0F)
        }
        val ratio = pageSize.width.toFloat() / pageSize.height.toFloat()
        if (ratio.isNaN() || ratio.isInfinite() || ratio <= 0f) {
            return SizeF(0F, 0F)
        }
        val height = floor(maxWidth / ratio)
        return SizeF(maxWidth, if (height.isFinite() && height > 0f) height else 0F)
    }

    private fun fitHeight(pageSize: Size, maxHeight: Float): SizeF {
        if (pageSize.width <= 0 || pageSize.height <= 0 || maxHeight <= 0f) {
            return SizeF(0F, 0F)
        }
        val ratio = pageSize.height.toFloat() / pageSize.width.toFloat()
        if (ratio.isNaN() || ratio.isInfinite() || ratio <= 0f) {
            return SizeF(0F, 0F)
        }
        val width = floor(maxHeight / ratio)
        return SizeF(if (width.isFinite() && width > 0f) width else 0F, maxHeight)
    }

    private fun fitBoth(pageSize: Size, maxWidth: Float, maxHeight: Float): SizeF {
        if (pageSize.width <= 0 || pageSize.height <= 0 || maxWidth <= 0f || maxHeight <= 0f) {
            return SizeF(0F, 0F)
        }
        val ratio = pageSize.width.toFloat() / pageSize.height.toFloat()
        if (ratio.isNaN() || ratio.isInfinite() || ratio <= 0f) {
            return SizeF(0F, 0F)
        }
        var w: Float = maxWidth
        var h: Float = floor(maxWidth / ratio)
        if (!h.isFinite() || h <= 0f || h > maxHeight) {
            h = maxHeight
            w = floor(maxHeight * ratio)
            if (!w.isFinite() || w <= 0f) {
                return SizeF(0F, 0F)
            }
        }
        return SizeF(w, h)
    }
}