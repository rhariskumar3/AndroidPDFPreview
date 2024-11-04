package com.harissk.pdfium.listener

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
 * A functional interface representing a logger.
 *
 * Implementations of this interface are responsible for handling log messages,
 * along with optional Throwable and tags.
 *
 * @since 04/11/24
 *
 * @see writeLog
 */
interface LogWriter {
    fun writeLog(message: String, tag: String = "PDFPreview")
    fun writeLog(throwable: Throwable, tag: String = "PDFPreview")
}