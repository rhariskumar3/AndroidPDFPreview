package com.harissk.pdfium.exception

/**
 * Created by Harishkumar on 12/12/23.
 */
sealed class PdfiumException : Exception()
class FileNotFoundException : PdfiumException()
class InvalidFormatException : PdfiumException()
class IncorrectPasswordException : PdfiumException()
class UnsupportedSecurityException : PdfiumException()

class PageNotFoundException : PdfiumException()
class UnknownException(private val error: String)
