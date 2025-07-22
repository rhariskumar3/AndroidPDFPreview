package com.harissk.androidpdfpreview.presentation.model

import android.graphics.Bitmap
import com.harissk.pdfpreview.validation.DocumentValidationResult
import java.io.File

/**
 * Represents the complete UI state for PDF preview functionality
 */
internal data class PDFPreviewState(
    val currentScreen: Screen = Screen.FileSelection,
    val selectedFile: File? = null,
    val fileName: String = "",
    val fileSize: String = "",
    val thumbnail: Bitmap? = null,
    val validationResult: DocumentValidationResult? = null,
    val pageCount: Int = 0,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val errorMessage: String? = null,
    val viewerSettings: ViewerSettings = ViewerSettings(),
    val isFullScreen: Boolean = false,
) {
    val canPreview: Boolean get() = validationResult is DocumentValidationResult.Valid
    val documentStatus: String
        get() = when (validationResult) {
            is DocumentValidationResult.Valid -> "Ready for preview"
            is DocumentValidationResult.PasswordProtected -> "Password protected"
            is DocumentValidationResult.Corrupted -> "Document corrupted"
            is DocumentValidationResult.Invalid -> "Invalid document"
            is DocumentValidationResult.Error -> "Validation error"
            null -> if (isLoading) loadingMessage else "No validation performed"
        }
}

