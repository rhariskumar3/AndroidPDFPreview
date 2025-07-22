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
package com.harissk.pdfpreview.validation

/**
 * Represents the result of a document validation operation.
 *
 * This sealed class provides different states that a PDF document can be in
 * when validation is performed.
 */
sealed class DocumentValidationResult {

    /**
     * The document is valid and can be opened without issues.
     *
     * @param pageCount The total number of pages in the document
     * @param hasMetadata Whether the document contains metadata
     * @param hasBookmarks Whether the document contains bookmarks/table of contents
     */
    data class Valid(
        val pageCount: Int,
        val hasMetadata: Boolean = false,
        val hasBookmarks: Boolean = false,
    ) : DocumentValidationResult()

    /**
     * The document is password protected and requires authentication.
     *
     * @param securityLevel The level of security protection (if determinable)
     */
    data class PasswordProtected(
        val securityLevel: SecurityLevel = SecurityLevel.UNKNOWN,
    ) : DocumentValidationResult()

    /**
     * The document is corrupted and cannot be opened.
     *
     * @param reason The reason for corruption
     * @param errorCode The native error code from PdfiumCore (if available)
     * @param errorMessage The detailed error message from PdfiumCore
     */
    data class Corrupted(
        val reason: CorruptionReason,
        val errorCode: Int = -1,
        val errorMessage: String = "",
    ) : DocumentValidationResult()

    /**
     * The document source is invalid (file not found, invalid format, etc.).
     *
     * @param reason The reason for invalidity
     * @param errorMessage The detailed error message
     */
    data class Invalid(
        val reason: InvalidityReason,
        val errorMessage: String = "",
    ) : DocumentValidationResult()

    /**
     * An unknown error occurred during validation.
     *
     * @param exception The exception that occurred
     * @param errorMessage A user-friendly error message
     */
    data class Error(
        val exception: Throwable,
        val errorMessage: String = "Unknown error occurred during validation",
    ) : DocumentValidationResult()
}

/**
 * Enum representing different levels of document security.
 */
enum class SecurityLevel {
    UNKNOWN,
    PASSWORD_PROTECTED,
    ENCRYPTED,
    UNSUPPORTED_SECURITY
}

/**
 * Enum representing different reasons for document corruption.
 */
enum class CorruptionReason {
    INVALID_PDF_STRUCTURE,
    INCOMPLETE_FILE,
    DAMAGED_CONTENT,
    UNSUPPORTED_VERSION,
    UNKNOWN_CORRUPTION
}

/**
 * Enum representing different reasons for document invalidity.
 */
enum class InvalidityReason {
    FILE_NOT_FOUND,
    NOT_A_PDF,
    EMPTY_FILE,
    INVALID_FORMAT,
    ACCESS_DENIED,
    UNKNOWN_INVALIDITY
}
