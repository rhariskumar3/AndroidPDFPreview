package com.harissk.pdfium.exception

/**
 * A base class for exceptions that can occur when using the Pdfium library.
 */
sealed class PdfiumException : Exception()

/**
 * Thrown when a PDF file is not found.
 */
class FileNotFoundException : PdfiumException() {
    override val message: String
        get() = "PDF file not found."
}

/**
 * Thrown when the PDF file is not in a valid format.
 */
class InvalidFormatException : PdfiumException() {
    override val message: String
        get() = "Invalid PDF file format."
}

/**
 * Thrown when the provided password is incorrect.
 */
class IncorrectPasswordException : PdfiumException() {
    override val message: String
        get() = "Incorrect password."
}

/**
 * Thrown when the PDF file uses an unsupported security method.
 */
class UnsupportedSecurityException : PdfiumException() {
    override val message: String
        get() = "Unsupported security method."
}

/**
 * Thrown when a requested page is not found in the PDF document.
 */
class PageNotFoundException : PdfiumException() {
    override val message: String
        get() = "Page not found."
}

/**
 * Thrown when an unknown error occurs during PDF processing.
 *
 * @param error The error message.
 */
class UnknownException(private val error: String) : PdfiumException() {
    override val message: String
        get() = "Unknown error: $error"
}