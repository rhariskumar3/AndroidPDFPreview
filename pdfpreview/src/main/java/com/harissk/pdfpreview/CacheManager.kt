package com.harissk.pdfpreview

import android.graphics.RectF
import com.harissk.pdfpreview.model.PagePart
import com.harissk.pdfpreview.request.RenderOptions
import java.util.PriorityQueue


/**
 * Created by Harishkumar on 25/11/23.
 */

internal class CacheManager(private val renderOptions: RenderOptions) {

    private val passiveCache: PriorityQueue<PagePart> by lazy {
        PriorityQueue(
            renderOptions.cacheSize,
            orderComparator
        )
    }
    private val activeCache: PriorityQueue<PagePart> by lazy {
        PriorityQueue(
            renderOptions.cacheSize,
            orderComparator
        )
    }
    private val thumbnails = arrayListOf<PagePart>()
    private val passiveActiveLock = Any()
    private val orderComparator: Comparator<PagePart> = Comparator { part1, part2 ->
        part1?.cacheOrder?.compareTo(part2.cacheOrder) ?: 0
    }

    fun cachePart(part: PagePart) {
        synchronized(passiveActiveLock) {

            // Remove and recycle bitmaps if the cache is full
            makeAFreeSpace()

            // Add the provided PagePart to the active cache
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
     * Make free space in the cache by removing and recycling bitmaps.
     */
    private fun makeAFreeSpace() {
        // Remove and recycle bitmaps from both passive and active cache until the limit is reached
        synchronized(passiveActiveLock) {
            while (activeCache.size + passiveCache.size >= renderOptions.cacheSize) {
                if (!passiveCache.isEmpty()) passiveCache.poll()?.renderedBitmap?.recycle()
                if (!activeCache.isEmpty()) activeCache.poll()?.renderedBitmap?.recycle()
            }
        }
    }

    fun cacheThumbnail(part: PagePart) {
        synchronized(thumbnails) {
            try {
                // If cache too big, remove and recycle
                while (thumbnails.size >= renderOptions.thumbnailsCacheSize)
                    thumbnails.removeAt(0).renderedBitmap?.recycle()

                // Add part if it doesn't exist, recycle bitmap otherwise
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
                null -> activeCache.firstOrNull { it == fakePart } != null
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
     * Return true if already contains the described PagePart
     */
    fun containsThumbnail(page: Int, pageRelativeBounds: RectF): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds, true, 0)
        synchronized(thumbnails) { return thumbnails.any { it == fakePart } }
    }

    fun getPageParts(): List<PagePart> = synchronized(passiveActiveLock) {
        return buildList {
            addAll(passiveCache)
            addAll(activeCache)
        }
    }

    fun getThumbnails(): List<PagePart> = synchronized(thumbnails) { return thumbnails }

    fun recycle() {
        // Recycle bitmaps and clear both caches
        synchronized(passiveActiveLock) {
            passiveCache.forEach { (_, renderedBitmap) -> renderedBitmap?.recycle() }
            activeCache.forEach { (_, renderedBitmap) -> renderedBitmap?.recycle() }
            passiveCache.clear()
            activeCache.clear()
        }

        // Recycle bitmaps and clear the thumbnail cache
        synchronized(thumbnails) {
            thumbnails.forEach { (_, renderedBitmap) -> renderedBitmap?.recycle() }
            thumbnails.clear()
        }
    }
}