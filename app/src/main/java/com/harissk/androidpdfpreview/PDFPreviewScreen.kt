package com.harissk.androidpdfpreview

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PDFPreviewScreen(
    onFullScreen: (current: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var pdfDocument by remember { mutableStateOf<PDFDocument?>(null) }
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
                pdfDocument = PDFDocument(
                    file = file,
                    name = pdfUri.getFileName(context).orEmpty()
                )
            }
        }

    fun pickPDF() = launcher.launch(selectPDFIntent)

    fun closePDF() {
        pdfDocument?.file?.delete()
        pdfDocument = null
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
                        if (pdfDocument != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${pdfDocument?.name})",
                                color = Color.Gray,
                                fontSize = 14.sp,
                            )
                        }
                    }
                },
                actions = {
                    Button(onClick = { fullScreen = !fullScreen }) {
                        Icon(
                            if (fullScreen) Icons.Filled.Fullscreen else Icons.Filled.FullscreenExit,
                            contentDescription = "Full Screen"
                        )
                    }
                    if (pdfDocument != null)
                        Button(onClick = {
                            closePDF()
                        }) {
                            Text("Close")
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

            when (pdfDocument) {
                null -> OutlinedCard(
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentWidth()
                        .padding(16.dp),
                    onClick = ::pickPDF,
                ) {
                    Text(
                        text = "PDF Configurations",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                    TextField(
                        value = viewerSettings.defaultPage.toString(),
                        onValueChange = {
                            viewerSettings =
                                viewerSettings.copy(defaultPage = it.toIntOrNull() ?: 0)
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
                                viewerSettings = viewerSettings.copy(swipeHorizontal = it)
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
                                viewerSettings = viewerSettings.copy(enableAnnotationRendering = it)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        modifier = Modifier
                            .padding(8.dp)
                            .padding(horizontal = 16.dp)
                            .align(Alignment.End),
                        onClick = ::pickPDF,
                    ) {
                        Icon(Icons.Filled.FileOpen, contentDescription = "Pick PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.label_pick_pdf))
                    }
                }

                else -> PDFViewer(
                    modifier = Modifier.fillMaxSize(),
                    pdfDocument = pdfDocument!!,
                    viewerSettings = viewerSettings,
                    onError = { pdfPreviewError = PDFPreviewError.FileError(it) }
                )
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