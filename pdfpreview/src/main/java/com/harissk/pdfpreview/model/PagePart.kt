package com.harissk.pdfpreview.model

import android.graphics.Bitmap
import android.graphics.RectF

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
 * Represents a part of a PDF page that is rendered in a PDF previewer.
 *
 * @param page                The page number.
 * @param renderedBitmap     The rendered {@link Bitmap} for this page part.
 * @param pageRelativeBounds The bounds of the page part relative to the page itself.
 * @param isThumbnail        Whether this page part represents a thumbnail.
 * @param cacheOrder          The order in which the page part was added to the cache, used for
 * caching strategies.
 */
data class PagePart(
    val page: Int,
    val renderedBitmap: Bitmap?,
    val pageRelativeBounds: RectF,
    val isThumbnail: Boolean,
    var cacheOrder: Int,
)