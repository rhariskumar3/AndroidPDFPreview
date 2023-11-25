package com.harissk.pdfpreview

import android.graphics.RectF
import com.harissk.pdfpreview.model.PagePart
import com.harissk.pdfpreview.utils.Constants.Cache.CACHE_SIZE
import com.harissk.pdfpreview.utils.Constants.Cache.THUMBNAILS_CACHE_SIZE
import java.util.PriorityQueue


/**
 * Created by Harishkumar on 25/11/23.
 */

class CacheManager {
    private val passiveCache: PriorityQueue<PagePart>
    private val activeCache: PriorityQueue<PagePart>
    private val thumbnails: MutableList<PagePart>
    private val passiveActiveLock = Any()
    private val orderComparator = PagePartComparator()

    init {
        activeCache = PriorityQueue<PagePart>(CACHE_SIZE, orderComparator)
        passiveCache = PriorityQueue<PagePart>(CACHE_SIZE, orderComparator)
        thumbnails = ArrayList()
    }

    fun cachePart(part: PagePart) {
        synchronized(passiveActiveLock) {

            // If cache too big, remove and recycle
            makeAFreeSpace()

            // Then add part
            activeCache.offer(part)
        }
    }

    fun makeANewSet() {
        synchronized(passiveActiveLock) {
            passiveCache.addAll(activeCache)
            activeCache.clear()
        }
    }

    private fun makeAFreeSpace() {
        synchronized(passiveActiveLock) {
            while (activeCache.size + passiveCache.size >= CACHE_SIZE &&
                !passiveCache.isEmpty()
            ) {
                val (_, renderedBitmap) = passiveCache.poll()!!
                renderedBitmap?.recycle()
            }
            while (activeCache.size + passiveCache.size >= CACHE_SIZE &&
                !activeCache.isEmpty()
            ) {
                activeCache.poll()?.renderedBitmap?.recycle()
            }
        }
    }

    fun cacheThumbnail(part: PagePart) {
        synchronized(thumbnails) {

            // If cache too big, remove and recycle
            while (thumbnails.size >= THUMBNAILS_CACHE_SIZE) {
                thumbnails.removeAt(0).renderedBitmap?.recycle()
            }

            // Then add thumbnail
            addWithoutDuplicates(thumbnails, part)
        }
    }

    fun upPartIfContained(page: Int, pageRelativeBounds: RectF?, toOrder: Int): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds!!, false, 0)
        var found: PagePart?
        synchronized(passiveActiveLock) {
            found = find(passiveCache, fakePart)
            if (found != null) {
                passiveCache.remove(found)
                found?.cacheOrder = toOrder
                activeCache.offer(found)
                return true
            }
            return find(activeCache, fakePart) != null
        }
    }

    /**
     * Return true if already contains the described PagePart
     */
    fun containsThumbnail(page: Int, pageRelativeBounds: RectF?): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds!!, true, 0)
        synchronized(thumbnails) {
            for (part in thumbnails) {
                if (part.equals(fakePart)) {
                    return true
                }
            }
            return false
        }
    }

    /**
     * Add part if it doesn't exist, recycle bitmap otherwise
     */
    private fun addWithoutDuplicates(collection: MutableCollection<PagePart>, newPart: PagePart) {
        for (part in collection) {
            if (part == newPart) {
                newPart.renderedBitmap?.recycle()
                return
            }
        }
        collection.add(newPart)
    }

    fun getPageParts(): List<PagePart> {
        synchronized(passiveActiveLock) {
            val parts: MutableList<PagePart> =
                ArrayList(passiveCache)
            parts.addAll(activeCache)
            return parts
        }
    }

    fun getThumbnails(): List<PagePart> {
        synchronized(thumbnails) { return thumbnails }
    }

    fun recycle() {
        synchronized(passiveActiveLock) {
            for ((_, renderedBitmap) in passiveCache) {
                renderedBitmap?.recycle()
            }
            passiveCache.clear()
            for ((_, renderedBitmap) in activeCache) {
                renderedBitmap?.recycle()
            }
            activeCache.clear()
        }
        synchronized(thumbnails) {
            for ((_, renderedBitmap) in thumbnails) {
                renderedBitmap?.recycle()
            }
            thumbnails.clear()
        }
    }

    internal inner class PagePartComparator : Comparator<PagePart?> {
        override fun compare(part1: PagePart?, part2: PagePart?): Int {
            if (part1?.cacheOrder == part2?.cacheOrder) {
                return 0
            }
            return if ((part1?.cacheOrder ?: 0) > (part2?.cacheOrder ?: 0)) 1 else -1
        }
    }

    companion object {
        private fun find(vector: PriorityQueue<PagePart>, fakePart: PagePart): PagePart? {
            for (part in vector) {
                if (part.equals(fakePart)) {
                    return part
                }
            }
            return null
        }
    }
}