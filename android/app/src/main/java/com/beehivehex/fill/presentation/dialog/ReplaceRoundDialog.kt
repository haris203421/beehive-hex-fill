package com.beehivehex.fill.presentation.dialog

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.beehivehex.fill.R

@Composable
fun ReplaceRoundDialog(onReplace: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.replace_title)) },
        text = { Text(stringResource(R.string.replace_message)) },
        confirmButton = { TextButton(onReplace, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.new_round)) } },
        dismissButton = { TextButton(onDismiss, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) } })
}
