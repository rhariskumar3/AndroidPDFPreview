package com.harissk.androidpdfpreview.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.harissk.androidpdfpreview.presentation.model.PDFPreviewIntent
import com.harissk.androidpdfpreview.presentation.model.PDFPreviewState
import com.harissk.androidpdfpreview.presentation.model.Screen
import com.harissk.androidpdfpreview.presentation.model.ViewerSettings
import com.harissk.pdfpreview.thumbnail.AspectRatio
import com.harissk.pdfpreview.thumbnail.PDFThumbnailGenerator
import com.harissk.pdfpreview.thumbnail.ThumbnailConfig
import com.harissk.pdfpreview.validation.DocumentValidationResult
import com.harissk.pdfpreview.validation.PDFDocumentValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

internal class PDFPreviewViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(PDFPreviewState())
    val state: StateFlow<PDFPreviewState> = _state.asStateFlow()

    fun handleIntent(intent: PDFPreviewIntent) {
        when (intent) {
            is PDFPreviewIntent.FileSelected -> handleFileSelected(intent.file, intent.fileName)
            is PDFPreviewIntent.PickNewFile -> handlePickNewFile()
            is PDFPreviewIntent.ClearSelection -> handleClearSelection()
            is PDFPreviewIntent.NavigateToDetails -> handleNavigateToDetails()
            is PDFPreviewIntent.NavigateToViewer -> handleNavigateToViewer()
            is PDFPreviewIntent.NavigateBack -> handleNavigateBack()
            is PDFPreviewIntent.UpdateViewerSettings -> handleUpdateViewerSettings(intent.settings)
            is PDFPreviewIntent.SetFullScreen -> handleSetFullScreen(intent.isFullScreen)
            is PDFPreviewIntent.ShowError -> handleShowError(intent.message)
            is PDFPreviewIntent.ClearError -> handleClearError()
            is PDFPreviewIntent.ValidationCompleted -> handleValidationCompleted(intent.result)
            is PDFPreviewIntent.ThumbnailGenerated -> handleThumbnailGenerated(intent.bitmap)
            is PDFPreviewIntent.LoadingStateChanged -> handleLoadingStateChanged(
                intent.isLoading,
                intent.message
            )
        }
    }

    private fun handleFileSelected(file: File, fileName: String) {
        _state.value = _state.value.copy(
            selectedFile = file,
            fileName = fileName,
            fileSize = formatFileSize(file.length()),
            currentScreen = Screen.DocumentDetails,
            errorMessage = null,
            thumbnail = null,
            validationResult = null,
            pageCount = 0
        )

        // Start validation and thumbnail generation
        validateDocument(file)
    }

    private fun handlePickNewFile() {
        // Clean up current file if exists
        _state.value.selectedFile?.delete()

        _state.value = _state.value.copy(
            currentScreen = Screen.FileSelection,
            selectedFile = null,
            fileName = "",
            fileSize = "",
            thumbnail = null,
            validationResult = null,
            pageCount = 0,
            isLoading = false,
            loadingMessage = "",
            errorMessage = null
        )
    }

    private fun handleClearSelection() {
        _state.value.selectedFile?.delete()
        _state.value = PDFPreviewState()
    }

    private fun handleNavigateToDetails() {
        if (_state.value.selectedFile != null) {
            _state.value = _state.value.copy(currentScreen = Screen.DocumentDetails)
        }
    }

    private fun handleNavigateToViewer() {
        if (_state.value.canPreview) {
            _state.value = _state.value.copy(currentScreen = Screen.PdfViewer)
        }
    }

    private fun handleNavigateBack() {
        when (_state.value.currentScreen) {
            Screen.PdfViewer -> _state.value = _state.value.copy(
                currentScreen = Screen.DocumentDetails,
                isFullScreen = false
            )

            Screen.DocumentDetails -> handlePickNewFile()
            Screen.FileSelection -> { /* Already at root */
            }
        }
    }

    private fun handleUpdateViewerSettings(settings: ViewerSettings) {
        _state.value = _state.value.copy(viewerSettings = settings)
    }

    private fun handleSetFullScreen(isFullScreen: Boolean) {
        _state.value = _state.value.copy(isFullScreen = isFullScreen)
    }

    private fun handleShowError(message: String) {
        _state.value = _state.value.copy(errorMessage = message)
    }

    private fun handleClearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun handleValidationCompleted(result: DocumentValidationResult) {
        _state.value = _state.value.copy(
            validationResult = result,
            pageCount = if (result is DocumentValidationResult.Valid) result.pageCount else 0
        )

        // Generate thumbnail if document is valid
        if (result is DocumentValidationResult.Valid) {
            generateThumbnail()
        } else {
            handleIntent(PDFPreviewIntent.LoadingStateChanged(false))
        }
    }

    private fun handleThumbnailGenerated(bitmap: Bitmap?) {
        _state.value = _state.value.copy(
            thumbnail = bitmap,
            isLoading = false,
            loadingMessage = ""
        )
    }

    private fun handleLoadingStateChanged(isLoading: Boolean, message: String) {
        _state.value = _state.value.copy(
            isLoading = isLoading,
            loadingMessage = message
        )
    }

    private fun validateDocument(file: File) {
        handleIntent(PDFPreviewIntent.LoadingStateChanged(true, "Validating document..."))

        viewModelScope.launch {
            try {
                val result = PDFDocumentValidator.validateDocument(context, file)
                handleIntent(PDFPreviewIntent.ValidationCompleted(result))
            } catch (e: Exception) {
                handleIntent(PDFPreviewIntent.ShowError("Failed to validate document: ${e.message}"))
                handleIntent(PDFPreviewIntent.LoadingStateChanged(false))
            }
        }
    }

    private fun generateThumbnail() {
        val file = _state.value.selectedFile ?: return

        handleIntent(PDFPreviewIntent.LoadingStateChanged(true, "Generating thumbnail..."))

        viewModelScope.launch {
            try {
                val thumbnail = PDFThumbnailGenerator.generateThumbnail(
                    context = context,
                    source = file,
                    pageIndex = 0,
                    config = ThumbnailConfig(
                        width = 200,
                        height = 280,
                        aspectRatio = AspectRatio.PRESERVE
                    )
                )
                handleIntent(PDFPreviewIntent.ThumbnailGenerated(thumbnail))
            } catch (e: Exception) {
                handleIntent(PDFPreviewIntent.ShowError("Failed to generate thumbnail: ${e.message}"))
                handleIntent(PDFPreviewIntent.LoadingStateChanged(false))
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()

        return DecimalFormat("#,##0.#").format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up file when ViewModel is destroyed
        _state.value.selectedFile?.delete()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PDFPreviewViewModel::class.java)) {
                return PDFPreviewViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
