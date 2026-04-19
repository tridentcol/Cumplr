package com.cumplr.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrAccentInk
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.Radius

enum class CumplrButtonVariant { Primary, Secondary, Destructive }

@Composable
fun CumplrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CumplrButtonVariant = CumplrButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(Radius.md)
    val height = Modifier.height(48.dp)

    when (variant) {
        CumplrButtonVariant.Primary -> Button(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CumplrAccent,
                contentColor = CumplrAccentInk,
                disabledContainerColor = CumplrAccent.copy(alpha = 0.4f),
                disabledContentColor = CumplrAccentInk.copy(alpha = 0.4f),
            ),
            modifier = modifier.then(height),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }

        CumplrButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            border = BorderStroke(
                1.dp,
                if (enabled) CumplrAccent else CumplrAccent.copy(alpha = 0.4f),
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CumplrAccent,
                disabledContentColor = CumplrAccent.copy(alpha = 0.4f),
            ),
            modifier = modifier.then(height),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }

        CumplrButtonVariant.Destructive -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            border = BorderStroke(
                1.dp,
                if (enabled) CumplrStatusOverdueFg else CumplrStatusOverdueFg.copy(alpha = 0.4f),
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CumplrStatusOverdueFg,
                disabledContentColor = CumplrStatusOverdueFg.copy(alpha = 0.4f),
            ),
            modifier = modifier.then(height),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
