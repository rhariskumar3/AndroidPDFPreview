package com.harissk.pdfpreview.request

import com.harissk.pdfpreview.listener.DocumentLoadListener
import com.harissk.pdfpreview.source.DocumentSource

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
 * Configuration for loading a specific PDF document.
 * These settings are document-specific and may change at runtime
 * (e.g., password retry, jumping to different pages, loading different documents).
 *
 * @property source The source of the PDF document to load (e.g., file, asset, URI).
 * @property password The password to use if the PDF document is encrypted. Defaults to null.
 * @property pageNumbers An optional list of specific page numbers to load. If null, all pages are loaded.
 * @property defaultPage The initial page number to display when the document is loaded. Defaults to 0 (the first page).
 * @property documentLoadListener A listener to be notified when the document is loaded and ready for rendering. Defaults to null.
 */
data class PdfLoadRequest(
    val source: DocumentSource,
    val password: String? = null,
    val pageNumbers: List<Int>? = null,
    val defaultPage: Int = 0,
    val documentLoadListener: DocumentLoadListener? = null,
) {

    class Builder(private val source: DocumentSource) {
        private var password: String? = null
        private var pageNumbers: List<Int>? = null
        private var defaultPage: Int = 0
        private var documentLoadListener: DocumentLoadListener? = null

        fun password(password: String?): Builder {
            this.password = password
            return this
        }

        fun pages(vararg pageNumbers: Int): Builder {
            this.pageNumbers = pageNumbers.toList()
            return this
        }

        fun defaultPage(defaultPage: Int): Builder {
            this.defaultPage = defaultPage
            return this
        }

        fun documentLoadListener(documentLoadListener: DocumentLoadListener): Builder {
            this.documentLoadListener = documentLoadListener
            return this
        }

        fun build() = PdfLoadRequest(
            source = source,
            password = password,
            pageNumbers = pageNumbers,
            defaultPage = defaultPage,
            documentLoadListener = documentLoadListener,
        )
    }

    override fun toString(): String = source.toString()
}