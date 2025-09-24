package com.harissk.pdfpreview

import android.graphics.RectF
import android.util.LruCache
import com.harissk.pdfpreview.model.PagePart
import com.harissk.pdfpreview.request.PdfViewerConfiguration
import java.util.PriorityQueue

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

    private val passiveCache: PriorityQueue<PagePart> by lazy {
        PriorityQueue(pdfViewerConfiguration.maxCachedBitmaps, PAGE_PART_COMPARATOR)
    }
    private val activeCache: PriorityQueue<PagePart> by lazy {
        PriorityQueue(pdfViewerConfiguration.maxCachedBitmaps, PAGE_PART_COMPARATOR)
    }
    private val thumbnails = LruCache<Int, PagePart>(pdfViewerConfiguration.maxCachedThumbnails)

    // Comparator for prioritizing page parts in the cache.  Now a constant
    private companion object {
        val PAGE_PART_COMPARATOR = Comparator<PagePart> { part1, part2 ->
            (part1?.cacheOrder ?: 0).compareTo(part2?.cacheOrder ?: 0)
        }
    }

    fun cachePart(part: PagePart) {
        val bitmap = part.renderedBitmap
        if (bitmap == null || bitmap.isRecycled) return

        synchronized(passiveActiveLock) {
            makeAFreeSpace()
            activeCache.offer(part)
        }
    }

    fun makeANewSet() = synchronized(passiveActiveLock) {
        passiveCache.addAll(activeCache)
        activeCache.clear()
    }

    private fun makeAFreeSpace() = synchronized(passiveActiveLock) {
        while ((activeCache.size + passiveCache.size) >= pdfViewerConfiguration.maxCachedBitmaps) {
            // Remove from passive first, then active if needed
            passiveCache.poll()?.renderedBitmap?.recycle()
                ?: activeCache.poll()?.renderedBitmap?.recycle()
        }
    }

    fun cacheThumbnail(part: PagePart) {
        val bitmap = part.renderedBitmap
        if (bitmap == null || bitmap.isRecycled) return

        thumbnails.put(part.page, part)
    }

    // Returns true if any part of 'page' is already in the active or passive caches
    fun upPartIfContained(page: Int, pageRelativeBounds: RectF, toOrder: Int): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds, false, 0)

        synchronized(passiveActiveLock) {
            // Check if the fake part (page and bounds match) exists in the active cache.
            // If it is found in the active cache return true, otherwise continue.
            val partInActiveCache =
                activeCache.firstOrNull { it.page == fakePart.page && it.pageRelativeBounds == fakePart.pageRelativeBounds }
            if (partInActiveCache != null) return true

            // If the page part is in passiveCache
            val partInPassiveCache =
                passiveCache.firstOrNull { it.page == fakePart.page && it.pageRelativeBounds == fakePart.pageRelativeBounds }
            if (partInPassiveCache != null) {
                passiveCache.remove(partInPassiveCache) // Remove the existing part
                passiveCache.offer(fakePart.copy(cacheOrder = toOrder)) // Add it back with a new order
                return true
            }
        }
        return false
    }

    fun containsThumbnail(page: Int, bounds: RectF): Boolean {
        val cachedThumbnail = thumbnails[page] ?: return false
        return cachedThumbnail.pageRelativeBounds == bounds
    }

    fun clearPageCache(page: Int) {
        synchronized(passiveActiveLock) {
            // Remove from active cache
            activeCache.removeAll { it.page == page }
            // Remove from passive cache
            passiveCache.removeAll { it.page == page }
        }
        // Remove thumbnail
        thumbnails.remove(page)
    }

    fun getPageParts(): List<PagePart> = synchronized(passiveActiveLock) {
        return buildList {
            addAll(passiveCache)
            addAll(activeCache)
        }
    }

    fun getThumbnails(): List<PagePart> = thumbnails.snapshot().values.toList()

    fun recycle() {
        synchronized(passiveActiveLock) {
            passiveCache.forEach { it.renderedBitmap?.recycle() }
            passiveCache.clear()
            activeCache.forEach { it.renderedBitmap?.recycle() }
            activeCache.clear()
        }

        thumbnails.evictAll()  // Recycle bitmaps in LruCache
    }

    private val passiveActiveLock = Any()
}