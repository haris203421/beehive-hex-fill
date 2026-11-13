package com.beehivehex.fill.presentation.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.beehivehex.fill.R
import com.beehivehex.fill.core.theme.*
import com.beehivehex.fill.core.ui.PrimaryButton
import com.beehivehex.fill.core.ui.SecondaryButton
import com.beehivehex.fill.domain.model.Hex

@Composable
fun PlacementDialog(description: String, valid: Boolean, onMove: (Hex) -> Unit, onPlace: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.adjust_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.adjust_hint), color = TextSecondary)
                Text(description, color = if (valid) Honey else GentleWarning,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; stateDescription = description })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(R.string.up_left, { onMove(Hex(0, -1)) }, Modifier.weight(1f))
                    SecondaryButton(R.string.up_right, { onMove(Hex(1, -1)) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(R.string.left, { onMove(Hex(-1, 0)) }, Modifier.weight(1f))
                    SecondaryButton(R.string.right, { onMove(Hex(1, 0)) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(R.string.down_left, { onMove(Hex(-1, 1)) }, Modifier.weight(1f))
                    SecondaryButton(R.string.down_right, { onMove(Hex(0, 1)) }, Modifier.weight(1f))
                }
            }
        }, confirmButton = { PrimaryButton(R.string.place, onPlace, enabled = valid) },
        dismissButton = { TextButton(onDismiss, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) } })
}
