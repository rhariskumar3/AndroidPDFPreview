package com.harissk.androidpdfpreview.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
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
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "Viewer Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Default Page Setting
        OutlinedTextField(
            value = settings.defaultPage.toString(),
            onValueChange = { value ->
                val page = value.toIntOrNull()?.coerceAtLeast(0) ?: 0
                onSettingsChange(settings.copy(defaultPage = page))
            },
            label = { Text("Default Page") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true
        )

        // Display Settings
        SettingsCategory(
            title = "Display",
            icon = Icons.Default.Palette
        ) {
            SettingRow(
                title = "Night Mode",
                subtitle = "Enable dark theme for better readability in low light",
                checked = settings.enableNightMode,
                onCheckedChange = { onSettingsChange(settings.copy(enableNightMode = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingRow(
                title = "Anti-aliasing",
                subtitle = "Enable smooth rendering for better text and graphics quality",
                checked = settings.enableAntialiasing,
                onCheckedChange = { onSettingsChange(settings.copy(enableAntialiasing = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingRow(
                title = "Render Annotations",
                subtitle = "Display PDF annotations and comments",
                checked = settings.enableAnnotationRendering,
                onCheckedChange = { onSettingsChange(settings.copy(enableAnnotationRendering = it)) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interaction Settings
        SettingsCategory(
            title = "Interaction",
            icon = Icons.Default.TouchApp
        ) {
            SettingRow(
                title = "Horizontal Swipe",
                subtitle = "Enable horizontal page navigation",
                checked = settings.swipeHorizontal,
                onCheckedChange = { onSettingsChange(settings.copy(swipeHorizontal = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingRow(
                title = "Double Tap Zoom",
                subtitle = "Enable double-tap gestures for zooming in and out",
                checked = settings.enableDoubleTapZoom,
                onCheckedChange = { onSettingsChange(settings.copy(enableDoubleTapZoom = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingRow(
                title = "Single Page Mode",
                subtitle = "View one page at a time without adjacent page visibility",
                checked = settings.singlePageMode,
                onCheckedChange = { onSettingsChange(settings.copy(singlePageMode = it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingRow(
                title = "Auto Page Spacing",
                subtitle = "Automatically adjust spacing between pages based on screen size",
                checked = settings.automaticPageSpacing,
                onCheckedChange = { onSettingsChange(settings.copy(automaticPageSpacing = it)) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Performance Settings
        SettingsCategory(
            title = "Performance",
            icon = Icons.Default.Speed
        ) {
            SettingRow(
                title = "High Memory Mode",
                subtitle = "Use more memory to prevent empty pages during scrolling",
                checked = settings.highMemoryMode,
                onCheckedChange = { onSettingsChange(settings.copy(highMemoryMode = it)) }
            )
        }
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

@Composable
private fun SettingsCategory(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Category Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Category Content
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}
