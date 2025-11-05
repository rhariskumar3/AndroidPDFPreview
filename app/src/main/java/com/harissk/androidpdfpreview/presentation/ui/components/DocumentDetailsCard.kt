package com.harissk.androidpdfpreview.presentation.ui.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harissk.pdfpreview.validation.DocumentValidationResult

@Composable
internal fun DocumentDetailsCard(
    fileName: String,
    fileSize: String,
    documentStatus: String,
    pageCount: Int,
    thumbnail: Bitmap?,
    validationResult: DocumentValidationResult?,
    isLoading: Boolean,
    canPreview: Boolean,
    onStartPreview: () -> Unit,
    onPickNewFile: () -> Unit,
    onLaunchXmlActivity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Header Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Document Details",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = onStartPreview,
                            enabled = canPreview,
                            modifier = Modifier.padding(start = 16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Preview", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Document Thumbnail
                DocumentThumbnailSection(
                    thumbnail = thumbnail,
                    isLoading = isLoading,
                    documentStatus = documentStatus
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Document Information
                DocumentInfoSection(
                    fileName = fileName,
                    fileSize = fileSize,
                    documentStatus = documentStatus,
                    pageCount = pageCount,
                    validationResult = validationResult
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                ActionButtonsSection(
                    onPickNewFile = onPickNewFile,
                    onLaunchXmlActivity = onLaunchXmlActivity,
                )
            }
        }
    }
}

@Composable
private fun DocumentThumbnailSection(
    thumbnail: Bitmap?,
    isLoading: Boolean,
    documentStatus: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(200.dp, 280.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = documentStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            thumbnail != null -> {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
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
}

@Composable
private fun DocumentInfoSection(
    fileName: String,
    fileSize: String,
    documentStatus: String,
    pageCount: Int,
    validationResult: DocumentValidationResult?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        InfoRow(label = "File Name", value = fileName)
        InfoRow(label = "File Size", value = fileSize)
        InfoRow(label = "Status", value = documentStatus)
        InfoRow(
            label = "Pages",
            value = if (pageCount > 0) pageCount.toString() else "Unknown"
        )

        // Additional validation details
        validationResult?.let { result ->
            when (result) {
                is DocumentValidationResult.Valid -> {
                    if (result.hasMetadata) {
                        InfoRow(label = "Metadata", value = "Available")
                    }
                    if (result.hasBookmarks) {
                        InfoRow(label = "Bookmarks", value = "Available")
                    }
                }

                is DocumentValidationResult.PasswordProtected -> {
                    InfoRow(
                        label = "Security",
                        value = result.securityLevel.name.replace("_", " ")
                    )
                }

                is DocumentValidationResult.Corrupted -> {
                    if (result.errorCode != -1) {
                        InfoRow(label = "Error Code", value = result.errorCode.toString())
                    }
                }

                else -> { /* No additional info */
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onPickNewFile: () -> Unit,
    onLaunchXmlActivity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onPickNewFile,
            modifier = Modifier.weight(1f)
        ) {
            Text("Select Another")
        }
        // launch XmlActivity
        Button(
            onClick = onLaunchXmlActivity,
            modifier = Modifier.weight(1f),
        ) {
            Text("Open in Xml")
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}