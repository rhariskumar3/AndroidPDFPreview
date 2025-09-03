package com.harissk.pdfpreview.utils

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
 * Enum representing the different policies for fitting a PDF page into a view.
 */
enum class FitPolicy {
    /**
     * Fit the width of the page to the view's width.
     */
    WIDTH,

    /**
     * Fit the height of the page to the view's height.
     */
    HEIGHT,

    /**
     * Fit both the width and height of the page to the view's dimensions, while preserving aspect
     * ratio.
     */
    BOTH
}