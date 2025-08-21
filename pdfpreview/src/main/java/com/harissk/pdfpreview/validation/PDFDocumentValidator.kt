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

import android.content.Context
import com.harissk.pdfium.PdfiumCore
import com.harissk.pdfium.exception.IncorrectPasswordException
import com.harissk.pdfium.exception.InvalidFormatException
import com.harissk.pdfium.exception.UnsupportedSecurityException
import com.harissk.pdfpreview.source.DocumentSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Utility class for validating PDF documents from various sources.
 *
 * This class provides static methods to validate PDF documents and determine their state:
 * - Valid and readable
 * - Password protected
 * - Corrupted
 * - Invalid format
 * - File not found
 *
 * All operations are performed on background threads for optimal performance and can be
 * used to pre-validate documents before attempting to open them in viewers or generate thumbnails.
 *
 * Example usage:
 * ```kotlin
 * // Basic validation
 * val result = PDFDocumentValidator.validateDocument(context, file)
 * when (result) {
 *     is DocumentValidationResult.Valid -> {
 *         println("Document is valid with ${result.pageCount} pages")
 *     }
 *     is DocumentValidationResult.PasswordProtected -> {
 *         println("Document requires password")
 *     }
 *     is DocumentValidationResult.Corrupted -> {
 *         println("Document is corrupted: ${result.reason}")
 *     }
 *     is DocumentValidationResult.Invalid -> {
 *         println("Document is invalid: ${result.reason}")
 *     }
 *     is DocumentValidationResult.Error -> {
 *         println("Error validating document: ${result.errorMessage}")
 *     }
 * }
 *
 * // Validation with password attempt
 * val resultWithPassword = PDFDocumentValidator.validateDocument(
 *     context, file, password = "user_password"
 * )
 *
 * // Quick validation (faster, less detailed)
 * val isValid = PDFDocumentValidator.isDocumentValid(context, file)
 * ```
 */
object PDFDocumentValidator {

    /**
     * Validates a PDF document from any supported source and returns detailed validation results.
     *
     * @param context The application context.
     * @param source The PDF document source (File, Uri, ByteArray, InputStream, String asset path, or DocumentSource).
     * @param password Optional password to test if the document is password protected.
     * @param includeMetadata Whether to include metadata analysis in the validation (default: true).
     * @return A [DocumentValidationResult] indicating the document's state and properties.
     */
    suspend fun validateDocument(
        context: Context,
        source: Any,
        password: String? = null,
        includeMetadata: Boolean = true,
    ): DocumentValidationResult = withContext(Dispatchers.IO) {
        try {
            // Convert source to DocumentSource
            val documentSource = try {
                DocumentSource.toDocumentSource(source)
            } catch (_: IllegalArgumentException) {
                return@withContext DocumentValidationResult.Invalid(
                    reason = InvalidityReason.INVALID_FORMAT,
                    errorMessage = "Unsupported document source type: ${source::class.simpleName}"
                )
            }

            // Pre-validate source if it's a file
            if (source is File) {
                val fileValidation = validateFileSource(source)
                if (fileValidation != null) return@withContext fileValidation
            }

            // Attempt to open the document
            val pdfiumCore = PdfiumCore()
            try {
                documentSource.createDocument(context, pdfiumCore, password)

                // Document opened successfully, gather information
                val pageCount = pdfiumCore.pageCount
                val hasMetadata = if (includeMetadata) checkMetadata(pdfiumCore) else false
                val hasBookmarks = if (includeMetadata) checkBookmarks(pdfiumCore) else false

                DocumentValidationResult.Valid(
                    pageCount = pageCount,
                    hasMetadata = hasMetadata,
                    hasBookmarks = hasBookmarks
                )

            } catch (e: IOException) {
                // Analyze the IOException to determine the specific issue
                analyzeIOException(e, pdfiumCore)

            } finally {
                try {
                    pdfiumCore.close()
                } catch (_: Exception) {
                    // Ignore close errors
                }
            }

        } catch (e: Exception) {
            // Handle any unexpected exceptions
            DocumentValidationResult.Error(
                exception = e,
                errorMessage = "Unexpected error during validation: ${e.message}"
            )
        }
    }

    /**
     * Performs a quick validation check to determine if a document is valid and readable.
     * This is a faster operation that returns only a boolean result.
     *
     * @param context The application context.
     * @param source The PDF document source.
     * @param password Optional password for encrypted documents.
     * @return True if the document is valid and readable, false otherwise.
     */
    suspend fun isDocumentValid(
        context: Context,
        source: Any,
        password: String? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = validateDocument(context, source, password, includeMetadata = false)
            result is DocumentValidationResult.Valid
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Validates if a document requires a password and tests if the provided password is correct.
     *
     * @param context The application context.
     * @param source The PDF document source.
     * @param password The password to test.
     * @return True if the password is correct, false if incorrect or if no password is needed.
     */
    suspend fun isPasswordCorrect(
        context: Context,
        source: Any,
        password: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = validateDocument(context, source, password, includeMetadata = false)
            result is DocumentValidationResult.Valid
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Determines if a document is password protected without attempting to open it.
     * This method will try to open the document without a password and check if it fails due to password protection.
     *
     * @param context The application context.
     * @param source The PDF document source.
     * @return True if the document requires a password, false otherwise.
     */
    suspend fun isPasswordProtected(
        context: Context,
        source: Any,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = validateDocument(context, source, password = null, includeMetadata = false)
            result is DocumentValidationResult.PasswordProtected
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Pre-validates a file source for basic file system issues.
     */
    private fun validateFileSource(file: File): DocumentValidationResult? {
        return when {
            !file.exists() -> DocumentValidationResult.Invalid(
                reason = InvalidityReason.FILE_NOT_FOUND,
                errorMessage = "File does not exist: ${file.absolutePath}"
            )

            !file.canRead() -> DocumentValidationResult.Invalid(
                reason = InvalidityReason.ACCESS_DENIED,
                errorMessage = "Cannot read file: ${file.absolutePath}"
            )

            file.length() == 0L -> DocumentValidationResult.Invalid(
                reason = InvalidityReason.EMPTY_FILE,
                errorMessage = "File is empty: ${file.absolutePath}"
            )

            file.length() < 10 -> DocumentValidationResult.Invalid(
                reason = InvalidityReason.INVALID_FORMAT,
                errorMessage = "File is too small to be a valid PDF"
            )

            else -> null // File appears valid for basic checks
        }
    }

    /**
     * Analyzes IOException to determine the specific validation result.
     */
    private fun analyzeIOException(
        e: IOException,
        pdfiumCore: PdfiumCore,
    ): DocumentValidationResult {
        val errorMessage = e.message?.lowercase() ?: ""

        return when {
            // Password related errors
            errorMessage.contains("password") ||
                    errorMessage.contains("incorrect password") ||
                    e.cause is IncorrectPasswordException -> {
                DocumentValidationResult.PasswordProtected(SecurityLevel.PASSWORD_PROTECTED)
            }

            // Security related errors
            errorMessage.contains("security") ||
                    errorMessage.contains("encrypted") ||
                    e.cause is UnsupportedSecurityException -> {
                DocumentValidationResult.PasswordProtected(SecurityLevel.UNSUPPORTED_SECURITY)
            }

            // Format related errors
            errorMessage.contains("invalid") ||
                    errorMessage.contains("format") ||
                    errorMessage.contains("not a pdf") ||
                    e.cause is InvalidFormatException -> {
                DocumentValidationResult.Invalid(
                    reason = InvalidityReason.NOT_A_PDF,
                    errorMessage = e.message ?: "Invalid PDF format"
                )
            }

            // File not found errors
            errorMessage.contains("file not found") ||
                    errorMessage.contains("no such file") ||
                    e is FileNotFoundException -> {
                DocumentValidationResult.Invalid(
                    reason = InvalidityReason.FILE_NOT_FOUND,
                    errorMessage = e.message ?: "File not found"
                )
            }

            // Corruption related errors
            errorMessage.contains("corrupt") ||
                    errorMessage.contains("damaged") ||
                    errorMessage.contains("incomplete") ||
                    errorMessage.contains("malformed") -> {
                val reason = when {
                    errorMessage.contains("incomplete") -> CorruptionReason.INCOMPLETE_FILE
                    errorMessage.contains("malformed") -> CorruptionReason.INVALID_PDF_STRUCTURE
                    else -> CorruptionReason.DAMAGED_CONTENT
                }

                DocumentValidationResult.Corrupted(
                    reason = reason,
                    errorCode = try {
                        pdfiumCore.getLastError(0)
                    } catch (_: Exception) {
                        -1
                    },
                    errorMessage = e.message ?: "Document is corrupted"
                )
            }

            else -> {
                // Unknown error, try to get more details from PdfiumCore
                val errorCode = try {
                    pdfiumCore.getLastError(0)
                } catch (_: Exception) {
                    -1
                }
                val nativeMessage = try {
                    if (errorCode != -1) pdfiumCore.getErrorMessage(errorCode) else ""
                } catch (_: Exception) {
                    ""
                }

                when (errorCode) {
                    1 -> DocumentValidationResult.PasswordProtected(SecurityLevel.PASSWORD_PROTECTED)
                    2 -> DocumentValidationResult.PasswordProtected(SecurityLevel.UNSUPPORTED_SECURITY)
                    3, 4, 5 -> DocumentValidationResult.Invalid(
                        reason = InvalidityReason.INVALID_FORMAT,
                        errorMessage = nativeMessage.ifEmpty { "Invalid PDF format" }
                    )

                    6, 7, 8 -> DocumentValidationResult.Corrupted(
                        reason = CorruptionReason.DAMAGED_CONTENT,
                        errorCode = errorCode,
                        errorMessage = nativeMessage.ifEmpty { "Document is corrupted" }
                    )

                    else -> DocumentValidationResult.Error(
                        exception = e,
                        errorMessage = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    /**
     * Checks if the document has metadata.
     */
    private suspend fun checkMetadata(pdfiumCore: PdfiumCore): Boolean {
        return try {
            val meta = pdfiumCore.getDocumentMeta()
            meta.title.isNotEmpty() || meta.author.isNotEmpty() || meta.subject.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the document has bookmarks/table of contents.
     */
    private suspend fun checkBookmarks(pdfiumCore: PdfiumCore): Boolean {
        return try {
            val bookmarks = pdfiumCore.getTableOfContents()
            bookmarks.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
