/*
 * Copyright (C) 2025 [Haris Kumar R](https://github.com/rhariskumar3)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.harissk.pdfpreview.thumbnail

import android.graphics.Bitmap
import androidx.collection.LruCache

/**
 * LRU cache for PDF thumbnails to improve performance by avoiding redundant generation.
 * Cache size is based on memory usage (in KB) rather than number of items.
 *
 * @param maxSizeInKB Maximum cache size in kilobytes. Default is 10MB (10,240 KB).
 */
internal class ThumbnailCache(maxSizeInKB: Int = 10_240) : LruCache<String, Bitmap>(maxSizeInKB) {

    override fun sizeOf(key: String, bitmap: Bitmap): Int {
        return bitmap.byteCount / 1024
    }

    override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
        if (evicted && !oldValue.isRecycled) oldValue.recycle()
    }

    /**
     * Generates a cache key based on source identifier, page index, and thumbnail configuration.
     */
    companion object {
        fun generateKey(
            sourceIdentifier: String,
            pageIndex: Int,
            config: ThumbnailConfig,
        ): String =
            "${sourceIdentifier}_${pageIndex}_${config.width}x${config.height}_${config.quality}_${config.aspectRatio}"
    }
}
