package com.harissk.pdfpreview.listener

import androidx.annotation.MainThread

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
 * Interface for listening to document load events.
 */
interface DocumentLoadListener {

    /**
     * Called when the PDF loading process starts.
     */
    @MainThread
    fun onDocumentLoadingStart()

    /**
     * Called when the PDF is loaded.
     *
     * @param totalPages The total number of pages in the PDF document.
     */
    @MainThread
    fun onDocumentLoaded(totalPages: Int)

    /**
     * Called if an error occurred while opening the PDF.
     *
     * @param error The exception that occurred.
     */
    @MainThread
    fun onDocumentLoadError(error: Throwable)
}