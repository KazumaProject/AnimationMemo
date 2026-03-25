package com.kazumaproject.animationswipememo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle
import com.kazumaproject.animationswipememo.domain.model.GifQuality
import com.kazumaproject.animationswipememo.domain.model.PaperStyle
import com.kazumaproject.animationswipememo.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Settings")
                        Text(
                            text = "Theme and export defaults",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Default animation",
                style = MaterialTheme.typography.titleLarge
            )
            SelectionDropdownRow(
                label = "Animation",
                selectedLabel = uiState.defaultAnimation.displayName,
                options = AnimationStyle.entries.map { it.displayName },
                onSelectLabel = { label ->
                    AnimationStyle.entries.firstOrNull { it.displayName == label }
                        ?.let(viewModel::updateDefaultAnimation)
                }
            )

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge
            )
            SelectionDropdownRow(
                label = "Theme mode",
                selectedLabel = uiState.themeMode.displayName,
                options = ThemeMode.entries.map { it.displayName },
                onSelectLabel = { label ->
                    ThemeMode.entries.firstOrNull { it.displayName == label }
                        ?.let(viewModel::updateThemeMode)
                }
            )
            SelectionDropdownRow(
                label = "Default memo background",
                selectedLabel = uiState.defaultPaperStyle.displayName,
                options = PaperStyle.entries.map { it.displayName },
                onSelectLabel = { label ->
                    PaperStyle.entries.firstOrNull { it.displayName == label }
                        ?.let(viewModel::updateDefaultPaperStyle)
                }
            )
            Text(
                text = "New memos start with this paper design. Existing memos keep their saved background.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "GIF quality",
                style = MaterialTheme.typography.titleLarge
            )
            SelectionDropdownRow(
                label = "Quality",
                selectedLabel = uiState.gifQuality.displayName,
                options = GifQuality.entries.map { it.displayName },
                onSelectLabel = { label ->
                    GifQuality.entries.firstOrNull { it.displayName == label }
                        ?.let(viewModel::updateGifQuality)
                }
            )

            Text(
                text = "Editor sheet",
                style = MaterialTheme.typography.titleLarge
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Opacity: ${(uiState.editorSheetOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Lower values make the text editor sheet more see-through so the memo stays visible while typing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = uiState.editorSheetOpacity,
                    onValueChange = viewModel::updateEditorSheetOpacity,
                    valueRange = 0.45f..1f
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDropdownRow(
    label: String,
    selectedLabel: String,
    options: List<String>,
    onSelectLabel: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                modifier = Modifier
                    .menuAnchor(
                        type = MenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    )
                    .fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelectLabel(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
