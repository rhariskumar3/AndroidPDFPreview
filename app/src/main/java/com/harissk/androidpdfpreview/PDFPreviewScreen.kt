package com.harissk.androidpdfpreview

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.harissk.pdfpreview.thumbnail.AspectRatio
import com.harissk.pdfpreview.thumbnail.PDFThumbnailGenerator
import com.harissk.pdfpreview.thumbnail.ThumbnailConfig
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

sealed class PDFPreviewState {
    object NoPdfSelected : PDFPreviewState()
    data class PdfSelected(val document: PDFDocument) : PDFPreviewState()
    data class PdfPreviewing(val document: PDFDocument) : PDFPreviewState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PDFPreviewScreen(
    onFullScreen: (current: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewState by remember { mutableStateOf<PDFPreviewState>(PDFPreviewState.NoPdfSelected) }
    var viewerSettings by remember { mutableStateOf(PDFViewerSettings()) }
    var pdfPreviewError by remember { mutableStateOf<PDFPreviewError>(PDFPreviewError.NoError) }
    var fullScreen by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = fullScreen) {
        onFullScreen.invoke(fullScreen)
    }

    val selectPDFIntent =
        remember { Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/pdf" } }
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val pdfUri = result.data?.data ?: return@rememberLauncherForActivityResult
                val file = pdfUri.uriToFile(context) ?: return@rememberLauncherForActivityResult
                previewState = PDFPreviewState.PdfSelected(
                    PDFDocument(
                        file = file,
                        name = pdfUri.getFileName(context).orEmpty()
                    )
                )
            }
        }

    fun pickPDF() = launcher.launch(selectPDFIntent)

    fun closePDF() {
        when (val state = previewState) {
            is PDFPreviewState.PdfSelected -> {
                state.document.file.delete()
                previewState = PDFPreviewState.NoPdfSelected
            }
            is PDFPreviewState.PdfPreviewing -> {
                state.document.file.delete()
                previewState = PDFPreviewState.NoPdfSelected
            }
            else -> {}
        }
    }

    fun backToDetails() {
        when (val state = previewState) {
            is PDFPreviewState.PdfPreviewing -> {
                previewState = PDFPreviewState.PdfSelected(state.document)
            }
            else -> {}
        }
    }

    fun startPreview() {
        when (val state = previewState) {
            is PDFPreviewState.PdfSelected -> {
                previewState = PDFPreviewState.PdfPreviewing(state.document)
            }
            else -> {}
        }
    }

    lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) closePDF()
    })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text("PDF Preview")
                        when (val state = previewState) {
                            is PDFPreviewState.PdfSelected, is PDFPreviewState.PdfPreviewing -> {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${state.document.name})",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                )
                            }
                            else -> {}
                        }
                    }
                },
                navigationIcon = {
                    when (previewState) {
                        is PDFPreviewState.PdfPreviewing -> {
                            IconButton(onClick = ::backToDetails) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                        else -> {}
                    }
                },
                actions = {
                    when (previewState) {
                        is PDFPreviewState.PdfPreviewing -> {
                            Button(onClick = { fullScreen = !fullScreen }) {
                                Icon(
                                    if (fullScreen) Icons.Filled.Fullscreen else Icons.Filled.FullscreenExit,
                                    contentDescription = "Full Screen"
                                )
                            }
                        }
                        else -> {}
                    }
                    
                    when (previewState) {
                        is PDFPreviewState.PdfSelected, is PDFPreviewState.PdfPreviewing -> {
                            Button(onClick = ::closePDF) {
                                Text("Close")
                            }
                        }
                        else -> {}
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            when (val state = previewState) {
                is PDFPreviewState.NoPdfSelected -> {
                    PDFConfigurationCard(
                        viewerSettings = viewerSettings,
                        onSettingsChange = { viewerSettings = it },
                        onPickPDF = ::pickPDF
                    )
                }
                
                is PDFPreviewState.PdfSelected -> {
                    PDFDetailsCard(
                        pdfDocument = state.document,
                        viewerSettings = viewerSettings,
                        onSettingsChange = { viewerSettings = it },
                        onStartPreview = ::startPreview,
                        onPickNewPDF = ::pickPDF
                    )
                }
                
                is PDFPreviewState.PdfPreviewing -> {
                    PDFViewer(
                        modifier = Modifier.fillMaxSize(),
                        pdfDocument = state.document,
                        viewerSettings = viewerSettings,
                        onError = { pdfPreviewError = PDFPreviewError.FileError(it) }
                    )
                }
            }

            when (val error = pdfPreviewError) {
                is PDFPreviewError.FileError -> Text(
                    text = "Error: ${error.message}",
                    color = Color.Red
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun PDFConfigurationCard(
    viewerSettings: PDFViewerSettings,
    onSettingsChange: (PDFViewerSettings) -> Unit,
    onPickPDF: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .padding(16.dp)
            .wrapContentWidth()
            .padding(16.dp),
        onClick = onPickPDF,
    ) {
        Text(
            text = "PDF Configurations",
            fontSize = 20.sp,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .padding(16.dp),
        )
        
        PDFSettingsForm(
            viewerSettings = viewerSettings,
            onSettingsChange = onSettingsChange
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            modifier = Modifier
                .padding(8.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.End),
            onClick = onPickPDF,
        ) {
            Icon(Icons.Filled.FileOpen, contentDescription = "Pick PDF")
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.label_pick_pdf))
        }
    }
}

@Composable
private fun PDFDetailsCard(
    pdfDocument: PDFDocument,
    viewerSettings: PDFViewerSettings,
    onSettingsChange: (PDFViewerSettings) -> Unit,
    onStartPreview: () -> Unit,
    onPickNewPDF: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingThumbnail by remember { mutableStateOf(false) }
    var pageCount by remember { mutableIntStateOf(0) }
    var fileSize by remember { mutableStateOf("") }
    
    LaunchedEffect(pdfDocument) {
        isLoadingThumbnail = true
        
        coroutineScope.launch {
            // Get page count
            pageCount = PDFThumbnailGenerator.getPageCount(context, pdfDocument.file)
            
            // Calculate file size
            val fileSizeBytes = pdfDocument.file.length()
            fileSize = formatFileSize(fileSizeBytes)
            
            // Generate thumbnail
            thumbnail = PDFThumbnailGenerator.generateThumbnail(
                context = context,
                source = pdfDocument.file,
                pageIndex = 0,
                config = ThumbnailConfig(
                    width = 200,
                    height = 280,
                    aspectRatio = AspectRatio.PRESERVE
                )
            )
            isLoadingThumbnail = false
        }
    }
    
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PDF Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Thumbnail section
                Box(
                    modifier = Modifier
                        .size(200.dp, 280.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingThumbnail -> {
                            CircularProgressIndicator()
                        }
                        thumbnail != null -> {
                            Image(
                                bitmap = thumbnail!!.asImageBitmap(),
                                contentDescription = "PDF Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        else -> {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No Preview\nAvailable",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // File information
                Column {
                    InfoRow(label = "File Name", value = pdfDocument.name)
                    InfoRow(label = "File Size", value = fileSize)
                    InfoRow(label = "Pages", value = if (pageCount > 0) pageCount.toString() else "Loading...")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Settings section
                Text(
                    text = "Viewer Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                PDFSettingsForm(
                    viewerSettings = viewerSettings,
                    onSettingsChange = onSettingsChange
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onPickNewPDF,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileOpen, contentDescription = "Pick Another PDF")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Another")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = onStartPreview,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Preview, contentDescription = "Start Preview")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview")
                    }
                }
            }
        }
    }
}

@Composable
private fun PDFSettingsForm(
    viewerSettings: PDFViewerSettings,
    onSettingsChange: (PDFViewerSettings) -> Unit
) {
    TextField(
        value = viewerSettings.defaultPage.toString(),
        onValueChange = {
            onSettingsChange(
                viewerSettings.copy(defaultPage = it.toIntOrNull() ?: 0)
            )
        },
        label = { Text("Default Page") },
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
    
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Swipe Horizontal")
        Switch(
            checked = viewerSettings.swipeHorizontal,
            onCheckedChange = {
                onSettingsChange(viewerSettings.copy(swipeHorizontal = it))
            }
        )
    }
    
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Enable Annotation Rendering")
        Switch(
            checked = viewerSettings.enableAnnotationRendering,
            onCheckedChange = {
                onSettingsChange(viewerSettings.copy(enableAnnotationRendering = it))
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    
    return DecimalFormat("#,##0.#").format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}

private val PDFPreviewState.document: PDFDocument
    get() = when (this) {
        is PDFPreviewState.PdfSelected -> this.document
        is PDFPreviewState.PdfPreviewing -> this.document
        else -> throw IllegalStateException("No document available")
    }