@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.taskmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taskmanager.app.data.api.LabelDto
import com.taskmanager.app.data.api.ProjectDto
import com.taskmanager.app.ui.theme.Border
import com.taskmanager.app.ui.theme.Danger
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.Surface
import com.taskmanager.app.ui.theme.Surface2
import com.taskmanager.app.ui.theme.Text
import com.taskmanager.app.ui.theme.soft

/** Swatch palette for projects and labels (hex strings). */
val TagColorPalette = listOf(
    "#8B7CFF", "#67E8F9", "#34D399", "#FBBF24", "#F87171",
    "#F472B6", "#FB923C", "#A3E635", "#38BDF8", "#C084FC"
)

/** Small pill showing a label name with its color dot. */
@Composable
fun LabelPill(label: LabelDto, modifier: Modifier = Modifier) {
    val dot = remember(label.color) {
        label.color?.let { parseTagColor(it) } ?: Iris
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(dot.soft(0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label.name,
            style = MaterialTheme.typography.labelSmall,
            color = dot
        )
    }
}

/** Repeat badge shown on recurring tasks. */
@Composable
fun RecurrenceBadge(recurrence: String, modifier: Modifier = Modifier) {
    if (recurrence.isBlank() || recurrence == "none") return
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Iris.soft(0.15f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Repeat,
            contentDescription = null,
            tint = Iris,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = recurrence.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = Iris
        )
    }
}

private fun parseTagColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Iris
}

/** Horizontal scrollable row of selectable chips (All + items). */
@Composable
fun FilterChipRow(
    allLabel: String,
    items: List<Pair<String, String?>>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                label = allLabel,
                selected = selectedId == null,
                onClick = { onSelect(null) },
                leadingIcon = leadingIcon
            )
        }
        items(items, key = { it.first }) { (id, name) ->
            FilterChip(
                label = name.orEmpty(),
                selected = selectedId == id,
                onClick = { onSelect(id) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Iris else Surface2)
            .border(1.dp, if (selected) Iris else Border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()
        if (leadingIcon != null) Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else Dim
        )
    }
}

/** Multi-select label chips for editors. */
@Composable
fun LabelPickerChips(
    labels: List<LabelDto>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) return
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            val selected = label.id in selectedIds
            val dot = remember(label.color) {
                label.color?.let { parseTagColor(it) } ?: Iris
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) dot.soft(0.25f) else Surface2)
                    .border(
                        1.dp,
                        if (selected) dot else Border,
                        RoundedCornerShape(50)
                    )
                    .clickable { onToggle(label.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = dot,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    imageVector = Icons.Filled.Label,
                    contentDescription = null,
                    tint = if (selected) dot else Dim,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Text else Dim
                )
            }
        }
    }
}

/** Project picker dropdown for the task editor. */
@Composable
fun ProjectDropdown(
    projects: List<ProjectDto>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = projects.firstOrNull { it.id == selectedId }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface2.copy(alpha = 0.55f))
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected?.color != null) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(parseTagColor(selected.color))
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = selected?.name ?: "No project",
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected != null) Text else Dim,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Dim)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Surface)
        ) {
            DropdownMenuItem(
                text = { Text("No project", color = Text) },
                onClick = { expanded = false; onSelect(null) }
            )
            projects.forEach { project ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            project.color?.let { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(parseTagColor(hex))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(project.name, color = Text)
                        }
                    },
                    onClick = { expanded = false; onSelect(project.id) }
                )
            }
        }
    }
}

/** Swatch picker row used by project/label dialogs. */
@Composable
fun ColorSwatchRow(
    colors: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { hex ->
            val isSelected = selected.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(parseTagColor(hex))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color.White else Border,
                        shape = CircleShape
                    )
                    .clickable { onSelect(if (isSelected) null else hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Shared create/edit dialog for projects and labels: name field, color swatches,
 * optional delete button when editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditDialog(
    title: String,
    initialName: String,
    initialColor: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text(title, color = Text, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                PremiumTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name"
                )
                Spacer(modifier = Modifier.height(16.dp))
                SectionLabel("Color")
                Spacer(modifier = Modifier.height(10.dp))
                ColorSwatchRow(
                    colors = TagColorPalette,
                    selected = color,
                    onSelect = { color = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), color)
                        onDismiss()
                    }
                }
            ) {
                Text("Save", color = Iris, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { onDelete(); onDismiss() }) {
                        Text("Delete", color = Danger, style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Dim, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    )
}
