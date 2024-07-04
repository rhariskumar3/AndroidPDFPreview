package com.harissk.pdfpreview

import android.graphics.RectF
import com.harissk.pdfpreview.model.PagePart
import com.harissk.pdfpreview.request.RenderOptions
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
 * A class for managing the cache of rendered PDF pages and thumbnails.
 */
internal class CacheManager(private val renderOptions: RenderOptions) {

    private val passiveCache: PriorityQueue<PagePart> by lazy {
        PriorityQueue(renderOptions.cacheSize, orderComparator)
    }
    private val activeCache: PriorityQueue<PagePart> by lazy {
        PriorityQueue(renderOptions.cacheSize, orderComparator)
    }
    private val thumbnails = arrayListOf<PagePart>()
    private val passiveActiveLock = Any()
    private val orderComparator: Comparator<PagePart> = Comparator { part1, part2 ->
        part1?.cacheOrder?.compareTo(part2.cacheOrder) ?: 0
    }

    fun cachePart(part: PagePart) {
        synchronized(passiveActiveLock) {
            // Remove and recycle bitmaps if the cache is full.
            makeAFreeSpace()
            // Add the provided PagePart to the active cache.
            activeCache.offer(part)
        }
    }

    fun makeANewSet() {
        synchronized(passiveActiveLock) {
            passiveCache.addAll(activeCache)
            activeCache.clear()
        }
    }

    /**
     * Makes free space in the cache by removing and recycling bitmaps.
     */
    private fun makeAFreeSpace() {
        synchronized(passiveActiveLock) {
            // Remove and recycle bitmaps from both passive and active cache until the limit is reached
            while (activeCache.size + passiveCache.size >= renderOptions.cacheSize) {
                if (!passiveCache.isEmpty()) passiveCache.poll()?.renderedBitmap?.recycle()
                if (!activeCache.isEmpty()) activeCache.poll()?.renderedBitmap?.recycle()
            }
        }
    }

    fun cacheThumbnail(part: PagePart) {
        synchronized(thumbnails) {
            try {
                // If the thumbnail cache is full, remove and recycle the oldest thumbnails.
                while (thumbnails.size >= renderOptions.thumbnailsCacheSize)
                    thumbnails.removeAt(0).renderedBitmap?.recycle()
                // Add the provided part to the cache, if not already present. Recycle its bitmap otherwise.
                when {
                    thumbnails.any { it == part } -> part.renderedBitmap?.recycle()
                    else -> thumbnails.add(part)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun upPartIfContained(page: Int, pageRelativeBounds: RectF, toOrder: Int): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds, false, 0)
        synchronized(passiveActiveLock) {
            return when (val found = passiveCache.firstOrNull { it == fakePart }) {
                // Check if the part exists in the active cache and return the result.
                null -> activeCache.firstOrNull { it == fakePart } != null
                // Otherwise If the part is found in the passive cache, remove it, update its cacheOrder, and add it
                // to the active cache.
                else -> {
                    passiveCache.remove(found)
                    found.cacheOrder = toOrder
                    activeCache.offer(found)
                    true
                }
            }
        }
    }

    /**
     * Returns true if the described {@link PagePart} already exists in the thumbnail cache.
     */
    fun containsThumbnail(page: Int, pageRelativeBounds: RectF): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds, true, 0)
        synchronized(thumbnails) { return thumbnails.any { it == fakePart }; }
    }

    // Return a new list combining elements from the passive and active caches.
    fun getPageParts(): List<PagePart> = synchronized(passiveActiveLock) {
        return buildList {
            addAll(passiveCache)
            addAll(activeCache)
        }
    }

    fun getThumbnails(): List<PagePart> = synchronized(thumbnails) { return thumbnails; }

    fun recycle() {
        synchronized(passiveActiveLock) {
            // Recycle bitmaps in both passive and active caches.
            passiveCache.forEach { (_, renderedBitmap) -> renderedBitmap?.recycle() }
            activeCache.forEach { (_, renderedBitmap) -> renderedBitmap?.recycle() }
            // Clear the caches.
            passiveCache.clear()
            activeCache.clear()
        }
        synchronized(thumbnails) {
            // Recycle bitmaps and clear the thumbnail cache.
            thumbnails.forEach { (_, renderedBitmap) -> renderedBitmap?.recycle() }
            thumbnails.clear()
        }
    }
}