package com.harissk.pdfpreview

import android.graphics.RectF
import android.util.LruCache
import com.harissk.pdfpreview.model.PagePart
import com.harissk.pdfpreview.request.PdfViewerConfiguration
import java.util.PriorityQueue

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
 * Manages the caching of rendered PDF pages and thumbnails.
 *
 * This class utilizes two priority queues for caching page parts:
 * - **activeCache:** Stores recently used page parts.
 * - **passiveCache:** Stores less frequently used page parts.
 *
 * It also uses an LruCache for storing thumbnails.
 *
 * The cache size is determined by the `RenderOptions` provided during initialization.
 *
 * Cache eviction is managed by prioritizing page parts based on a `cacheOrder` property.
 * When the cache reaches its capacity, the least recently used parts (with the lowest `cacheOrder`)
 * are removed to make space for new ones.
 */
internal class CacheManager(private val pdfViewerConfiguration: PdfViewerConfiguration) {

    private var currentCacheSizeBytes: Long = 0L
    private val passiveActiveLock = Any() // Lock object for synchronizing access

    // Default initial capacity for priority queues
    private val initialQueueCapacity = 10

    private val passiveCache: PriorityQueue<PagePart> by lazy {
        PriorityQueue(initialQueueCapacity, PAGE_PART_COMPARATOR)
    }
    private val activeCache: PriorityQueue<PagePart> by lazy {
        PriorityQueue(initialQueueCapacity, PAGE_PART_COMPARATOR)
    }

    private val thumbnails = object : LruCache<Int, PagePart>(
        // Cap max thumbnail cache size to a reasonable Int value, e.g., 16MB if config is too large or not set.
        // LruCache's maxSize is an Int.
        (pdfViewerConfiguration.maxThumbnailCacheSizeBytes.takeIf { it > 0 }?.toInt()
            ?: (16 * 1024 * 1024)).coerceAtMost(Int.MAX_VALUE / 2) // Avoid overflow issues with Int.MAX_VALUE
    ) {
        override fun sizeOf(key: Int, value: PagePart): Int {
            return value.renderedBitmap?.byteCount ?: 0
        }

        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: PagePart?, newValue: PagePart?) {
            if (oldValue?.renderedBitmap?.isRecycled == false) {
                oldValue.renderedBitmap?.recycle()
            }
        }
    }

    private companion object {
        val PAGE_PART_COMPARATOR = Comparator<PagePart> { part1, part2 ->
            (part1?.cacheOrder ?: 0).compareTo(part2?.cacheOrder ?: 0)
        }
    }

    fun cachePart(part: PagePart) {
        val bitmap = part.renderedBitmap
        if (bitmap == null || bitmap.isRecycled) return

        val bitmapSize = bitmap.byteCount
        if (bitmapSize == 0) return // No point caching an empty bitmap

        synchronized(passiveActiveLock) {
            makeSpaceFor(bitmapSize.toLong())

            // After attempting to make space, check if we can add the new part
            if ((currentCacheSizeBytes + bitmapSize) <= pdfViewerConfiguration.maxCacheSizeBytes) {
                activeCache.offer(part)
                currentCacheSizeBytes += bitmapSize
            } else {
                // Not enough space could be made, recycle the incoming part's bitmap
                part.renderedBitmap?.recycle()
                // Optionally log that the part could not be cached due to size constraints
            }
        }
    }

    fun makeANewSet() = synchronized(passiveActiveLock) {
        passiveCache.addAll(activeCache)
        activeCache.clear()
    }

    private fun makeSpaceFor(requiredBitmapSizeBytes: Long) = synchronized(passiveActiveLock) {
        while ((currentCacheSizeBytes + requiredBitmapSizeBytes) > pdfViewerConfiguration.maxCacheSizeBytes && (activeCache.isNotEmpty() || passiveCache.isNotEmpty())) {
            val partToRecycle = passiveCache.poll() ?: activeCache.poll()
            partToRecycle?.renderedBitmap?.let { bmp ->
                if (!bmp.isRecycled) {
                    currentCacheSizeBytes -= bmp.byteCount
                    bmp.recycle()
                }
            }
            // If partToRecycle is null, both queues are empty, loop will terminate.
            if (partToRecycle == null) break
        }
    }

    fun cacheThumbnail(part: PagePart) {
        val bitmap = part.renderedBitmap
        if (bitmap == null || bitmap.isRecycled || bitmap.byteCount == 0) return

        thumbnails.put(part.page, part)
    }

    fun upPartIfContained(page: Int, pageRelativeBounds: RectF, toOrder: Int): Boolean {
        synchronized(passiveActiveLock) {
            val partInActiveCache = activeCache.firstOrNull { it.page == page && it.pageRelativeBounds == pageRelativeBounds }
            if (partInActiveCache != null) {
                // Part is already in active cache, update its order and re-offer to update priority
                activeCache.remove(partInActiveCache)
                partInActiveCache.cacheOrder = toOrder
                activeCache.offer(partInActiveCache)
                return true // currentCacheSizeBytes does not change
            }

            val partInPassiveCache = passiveCache.firstOrNull { it.page == page && it.pageRelativeBounds == pageRelativeBounds }
            if (partInPassiveCache != null) {
                passiveCache.remove(partInPassiveCache)
                partInPassiveCache.cacheOrder = toOrder
                activeCache.offer(partInPassiveCache) // Move to active cache
                return true // currentCacheSizeBytes does not change as bitmap is already tracked
            }
        }
        return false
    }

    fun containsThumbnail(page: Int, bounds: RectF): Boolean {
        val cachedThumbnail = thumbnails[page] ?: return false
        // Ensure the bitmap is not recycled, as LruCache might keep the entry for a short while
        // even if sizeOf returned 0 for a recycled bitmap before actual removal.
        return cachedThumbnail.pageRelativeBounds == bounds && cachedThumbnail.renderedBitmap?.isRecycled == false
    }

    fun getPageParts(): List<PagePart> = synchronized(passiveActiveLock) {
        return buildList {
            addAll(passiveCache)
            addAll(activeCache)
        }
    }

    fun getThumbnails(): List<PagePart> = thumbnails.snapshot().values.filter { it.renderedBitmap?.isRecycled == false }.toList()

    fun recycle() {
        synchronized(passiveActiveLock) {
            passiveCache.forEach { it.renderedBitmap?.recycle() }
            passiveCache.clear()
            activeCache.forEach { it.renderedBitmap?.recycle() }
            activeCache.clear()
            currentCacheSizeBytes = 0L
        }
        thumbnails.evictAll()
    }
}