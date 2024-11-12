package com.harissk.androidpdfpreview

sealed class PDFPreviewError {
    data object NoError : PDFPreviewError()
    data class FileError(val message: String) : PDFPreviewError()
}