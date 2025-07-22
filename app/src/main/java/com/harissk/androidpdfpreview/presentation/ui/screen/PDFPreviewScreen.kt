package com.harissk.androidpdfpreview.presentation.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harissk.androidpdfpreview.data.util.getFileName
import com.harissk.androidpdfpreview.data.util.uriToFile
import com.harissk.androidpdfpreview.domain.model.PDFDocument
import com.harissk.androidpdfpreview.presentation.model.PDFPreviewIntent
import com.harissk.androidpdfpreview.presentation.model.Screen
import com.harissk.androidpdfpreview.presentation.ui.components.DocumentDetailsCard
import com.harissk.androidpdfpreview.presentation.ui.components.FileSelectionCard
import com.harissk.androidpdfpreview.presentation.viewmodel.PDFPreviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFPreviewScreen(
    onFullScreen: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: PDFPreviewViewModel = viewModel(
        factory = PDFPreviewViewModel.Factory(context)
    )

    val state by viewModel.state.collectAsState()

    // Handle fullscreen changes
    LaunchedEffect(state.isFullScreen) {
        onFullScreen(state.isFullScreen)
    }

    // Handle error messages
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.handleIntent(PDFPreviewIntent.ClearError)
        }
    }

    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val file = uri.uriToFile(context)
                val fileName = uri.getFileName(context)
                if (file != null && fileName != null) {
                    viewModel.handleIntent(PDFPreviewIntent.FileSelected(file, fileName))
                } else {
                    viewModel.handleIntent(PDFPreviewIntent.ShowError("Failed to process selected file"))
                }
            }
        }
    }

    val pickFile = {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
        }
        filePickerLauncher.launch(intent)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (!state.isFullScreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (state.currentScreen) {
                                Screen.FileSelection -> "PDF Preview"
                                Screen.DocumentDetails -> state.fileName.takeIf { it.isNotEmpty() }
                                    ?: "Document Details"

                                Screen.PdfViewer -> state.fileName.takeIf { it.isNotEmpty() }
                                    ?: "PDF Viewer"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        when (state.currentScreen) {
                            Screen.DocumentDetails, Screen.PdfViewer -> {
                                IconButton(
                                    onClick = { viewModel.handleIntent(PDFPreviewIntent.NavigateBack) }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }

                            else -> {}
                        }
                    },
                    actions = {
                        when (state.currentScreen) {
                            Screen.PdfViewer -> {
                                IconButton(
                                    onClick = {
                                        viewModel.handleIntent(PDFPreviewIntent.SetFullScreen(true))
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Fullscreen,
                                        contentDescription = "Fullscreen"
                                    )
                                }
                            }

                            Screen.DocumentDetails -> {
                                IconButton(
                                    onClick = { viewModel.handleIntent(PDFPreviewIntent.ClearSelection) }
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close")
                                }
                            }

                            else -> {}
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            // Show exit fullscreen button when in fullscreen mode
            if (state.isFullScreen && state.currentScreen == Screen.PdfViewer) {
                FloatingActionButton(
                    onClick = {
                        viewModel.handleIntent(PDFPreviewIntent.SetFullScreen(false))
                    },
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                ) {
                    Icon(
                        Icons.Filled.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (state.currentScreen) {
                Screen.FileSelection -> {
                    FileSelectionCard(
                        viewerSettings = state.viewerSettings,
                        onSettingsChange = { settings ->
                            viewModel.handleIntent(PDFPreviewIntent.UpdateViewerSettings(settings))
                        },
                        onPickFile = pickFile
                    )
                }

                Screen.DocumentDetails -> {
                    DocumentDetailsCard(
                        fileName = state.fileName,
                        fileSize = state.fileSize,
                        documentStatus = state.documentStatus,
                        pageCount = state.pageCount,
                        thumbnail = state.thumbnail,
                        validationResult = state.validationResult,
                        isLoading = state.isLoading,
                        canPreview = state.canPreview,
                        viewerSettings = state.viewerSettings,
                        onSettingsChange = { settings ->
                            viewModel.handleIntent(PDFPreviewIntent.UpdateViewerSettings(settings))
                        },
                        onStartPreview = {
                            viewModel.handleIntent(PDFPreviewIntent.NavigateToViewer)
                        },
                        onPickNewFile = pickFile
                    )
                }

                Screen.PdfViewer -> {
                    state.selectedFile?.let { file ->
                        PDFViewer(
                            modifier = Modifier.fillMaxSize(),
                            pdfDocument = PDFDocument(file = file, name = state.fileName),
                            viewerSettings = state.viewerSettings,
                            onError = { error ->
                                viewModel.handleIntent(PDFPreviewIntent.ShowError(error))
                            },
                            onFullScreenToggle = {
                                viewModel.handleIntent(PDFPreviewIntent.SetFullScreen(!state.isFullScreen))
                            }
                        )
                    }
                }
            }
        }
    }
}
