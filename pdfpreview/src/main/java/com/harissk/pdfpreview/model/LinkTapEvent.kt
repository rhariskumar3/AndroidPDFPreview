package com.harissk.pdfpreview.model

import android.graphics.RectF
import com.harissk.pdfium.Link

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
 * Represents a link tap event in a PDF previewer.
 *
 * @param originalX The original X coordinate of the touch event in screen coordinates.
 * @param originalY The original Y coordinate of the touch event in screen coordinates.
 * @param documentX The X coordinate of the touch event in document coordinates.
 * @param documentY The Y coordinate of the touch event in document coordinates.
 * @param mappedLinkRect The bounding rectangle of the link, mapped to screen coordinates.
 * @param link       The {@link Link} object representing the tapped link.
 */
data class LinkTapEvent(
    val originalX: Float,
    val originalY: Float,
    val documentX: Float,
    val documentY: Float,
    val mappedLinkRect: RectF,
    val link: Link,
)