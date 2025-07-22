package com.harissk.androidpdfpreview.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harissk.androidpdfpreview.presentation.model.ViewerSettings

@Composable
internal fun FileSelectionCard(
    viewerSettings: ViewerSettings,
    onSettingsChange: (ViewerSettings) -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PDF Preview",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Configure settings and select a PDF file to preview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Viewer Settings Section
            ViewerSettingsSection(
                settings = viewerSettings,
                onSettingsChange = onSettingsChange
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Pick File Button
            Button(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select PDF File")
            }
        }
    }
}

@Composable
private fun ViewerSettingsSection(
    settings: ViewerSettings,
    onSettingsChange: (ViewerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Viewer Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Default Page Setting
        TextField(
            value = settings.defaultPage.toString(),
            onValueChange = { value ->
                val page = value.toIntOrNull()?.coerceAtLeast(0) ?: 0
                onSettingsChange(settings.copy(defaultPage = page))
            },
            label = { Text("Default Page") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        // Swipe Direction Setting
        SettingRow(
            title = "Horizontal Swipe",
            subtitle = "Enable horizontal page navigation",
            checked = settings.swipeHorizontal,
            onCheckedChange = { onSettingsChange(settings.copy(swipeHorizontal = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Annotation Rendering Setting
        SettingRow(
            title = "Render Annotations",
            subtitle = "Display PDF annotations and comments",
            checked = settings.enableAnnotationRendering,
            onCheckedChange = { onSettingsChange(settings.copy(enableAnnotationRendering = it)) }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
