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

package com.harissk.androidpdfpreview.presentation.model

import android.graphics.Bitmap
import java.io.File

/**
 * User intents for PDF preview functionality
 */
internal sealed class PDFPreviewIntent {

    // File selection intents
    data class FileSelected(val file: File, val fileName: String) : PDFPreviewIntent()
    data object PickNewFile : PDFPreviewIntent()
    data object ClearSelection : PDFPreviewIntent()

    // Navigation intents
    data object NavigateToDetails : PDFPreviewIntent()
    data object NavigateToViewer : PDFPreviewIntent()
    data object NavigateBack : PDFPreviewIntent()

    // Settings intents
    data class UpdateViewerSettings(val settings: ViewerSettings) : PDFPreviewIntent()

    // UI state intents
    data class SetFullScreen(val isFullScreen: Boolean) : PDFPreviewIntent()
    data class ShowError(val message: String) : PDFPreviewIntent()
    data object ClearError : PDFPreviewIntent()

    // Internal processing intents (for ViewModel use)
    data class ValidationCompleted(val result: com.harissk.pdfpreview.validation.DocumentValidationResult) :
        PDFPreviewIntent()

    data class ThumbnailGenerated(val bitmap: Bitmap?) : PDFPreviewIntent()
    data class LoadingStateChanged(val isLoading: Boolean, val message: String = "") :
        PDFPreviewIntent()
}
