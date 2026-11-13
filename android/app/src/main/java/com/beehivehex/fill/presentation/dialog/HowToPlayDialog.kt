package com.beehivehex.fill.presentation.dialog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beehivehex.fill.R
import com.beehivehex.fill.core.theme.Honey
import com.beehivehex.fill.core.theme.TextSecondary

@Composable
fun HowToPlayDialog(onDismiss: () -> Unit) {
    val description = stringResource(R.string.axes_description)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.help_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Canvas(Modifier.fillMaxWidth().height(72.dp).semantics { contentDescription = description }) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.height * 0.44f
                    drawLine(Honey, center - Offset(radius, 0f), center + Offset(radius, 0f), 3.dp.toPx())
                    drawLine(Honey, center - Offset(radius / 2, radius * 0.86f), center + Offset(radius / 2, radius * 0.86f), 3.dp.toPx())
                    drawLine(Honey, center - Offset(radius / 2, -radius * 0.86f), center + Offset(radius / 2, -radius * 0.86f), 3.dp.toPx())
                    drawCircle(Honey, 6.dp.toPx(), center)
                }
                Text(stringResource(R.string.help_step_one))
                Text(stringResource(R.string.help_step_two))
                Text(stringResource(R.string.help_step_three))
                Text(stringResource(R.string.help_tip), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }, confirmButton = { TextButton(onDismiss, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.got_it)) } })
}
