package com.kazumaproject.animationswipememo.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kazumaproject.animationswipememo.domain.model.AnimationStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimationStyleChips(
    selected: AnimationStyle,
    onSelect: (AnimationStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimationStyle.entries.forEach { style ->
            FilterChip(
                selected = style == selected,
                onClick = { onSelect(style) },
                label = {
                    Text(
                        text = style.displayName,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                modifier = Modifier.padding(end = 2.dp)
            )
        }
    }
}
