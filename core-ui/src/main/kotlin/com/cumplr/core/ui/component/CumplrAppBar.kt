package com.cumplr.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cumplr.core.ui.theme.CumplrBackground
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.Spacing

@Composable
fun CumplrAppBar(
    title: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(CumplrBackground)
            .padding(horizontal = Spacing.lg),
    ) {
        if (leadingIcon != null) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                leadingIcon()
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = CumplrFg,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = if (leadingIcon != null) 40.dp else 0.dp),
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            content = actions,
        )
    }
}
