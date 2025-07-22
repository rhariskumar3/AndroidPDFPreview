package com.harissk.androidpdfpreview.domain.model

sealed class PDFPreviewError {
    data object NoError : PDFPreviewError()
    data class FileError(val message: String) : PDFPreviewError()
}
