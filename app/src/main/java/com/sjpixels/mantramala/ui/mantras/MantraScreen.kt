package com.sjpixels.mantramala.ui.mantras

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sjpixels.mantramala.R
import com.sjpixels.mantramala.data.CustomMantra
import com.sjpixels.mantramala.ui.MainViewModel
import com.sjpixels.mantramala.ui.MantraOption
import com.sjpixels.mantramala.ui.components.Diamond
import com.sjpixels.mantramala.ui.components.OrnamentDivider
import com.sjpixels.mantramala.ui.components.SectionHeader
import com.sjpixels.mantramala.ui.components.SharpCard
import com.sjpixels.mantramala.ui.theme.Devanagari

@Composable
fun mantraTitle(option: MantraOption): String =
    option.customName ?: option.builtInTitleRes?.let { stringResource(it) } ?: ""

@Composable
fun MantraScreen(vm: MainViewModel, onChosen: () -> Unit) {
    val options by vm.mantraOptions.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var toDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    val builtIns = options.filter { it.custom == null }
    val customs = options.filter { it.custom != null }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.mantras_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.mantras_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OrnamentDivider()
            }
        }
        item { SectionHeader(stringResource(R.string.section_sacred_mantras)) }
        items(builtIns, key = { it.key }) { option ->
            MantraCard(option, selected = option.key == settings.selectedMantraKey, onSelect = {
                vm.selectMantra(option.key); onChosen()
            })
        }
        item { SectionHeader(stringResource(R.string.section_my_mantras)) }
        if (customs.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.my_mantras_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        items(customs, key = { it.key }) { option ->
            MantraCard(
                option,
                selected = option.key == settings.selectedMantraKey,
                onSelect = { vm.selectMantra(option.key); onChosen() },
                onDelete = { toDelete = option.custom?.id },
            )
        }
        item {
            OutlinedButton(
                onClick = { adding = true },
                shape = CutCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Icon(Icons.Sharp.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_mantra))
            }
        }
    }

    if (adding) {
        AddMantraDialog(
            onDismiss = { adding = false },
            onSave = { name, text, meaning -> vm.addCustomMantra(name, text, meaning); adding = false; onChosen() },
        )
    }
    toDelete?.let { id ->
        val target: CustomMantra? = customs.firstOrNull { it.custom?.id == id }?.custom
        if (target == null) {
            toDelete = null
        } else {
            AlertDialog(
                onDismissRequest = { toDelete = null },
                title = { Text(stringResource(R.string.delete_mantra_title)) },
                text = { Text(stringResource(R.string.delete_mantra_message, target.name)) },
                confirmButton = {
                    TextButton(onClick = { vm.deleteCustomMantra(target); toDelete = null }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
}

@Composable
private fun MantraCard(option: MantraOption, selected: Boolean, onSelect: () -> Unit, onDelete: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    SharpCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect), highlighted = selected, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(mantraTitle(option), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(option.text, fontFamily = Devanagari, fontSize = 18.sp, lineHeight = 28.sp, color = scheme.primary)
                option.transliteration?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = scheme.onSurfaceVariant)
                }
                val meaning = option.builtInMeaningRes?.let { stringResource(it) } ?: option.customMeaning?.takeIf { it.isNotBlank() }
                meaning?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface.copy(alpha = 0.85f))
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    if (selected) {
                        Diamond(scheme.primary, size = 28.dp)
                        Icon(Icons.Sharp.Check, contentDescription = stringResource(R.string.mantra_selected), tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                    } else {
                        Diamond(scheme.outlineVariant, size = 14.dp)
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Sharp.DeleteOutline, contentDescription = stringResource(R.string.action_delete), tint = scheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (selected) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.mantra_selected).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AddMantraDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var meaning by rememberSaveable { mutableStateOf("") }
    val valid = name.isNotBlank() && text.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_mantra)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(60) },
                    label = { Text(stringResource(R.string.field_mantra_name)) },
                    singleLine = true, shape = MaterialTheme.shapes.small,
                )
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(400) },
                    label = { Text(stringResource(R.string.field_mantra_text)) },
                    minLines = 2, shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = Devanagari),
                )
                OutlinedTextField(
                    value = meaning, onValueChange = { meaning = it.take(300) },
                    label = { Text(stringResource(R.string.field_mantra_meaning)) },
                    minLines = 2, shape = MaterialTheme.shapes.small,
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(name, text, meaning) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
