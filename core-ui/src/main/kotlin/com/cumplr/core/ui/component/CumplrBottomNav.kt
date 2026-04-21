package com.cumplr.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrSurface

data class CumplrNavItem(
    val key: String,
    val icon: ImageVector,
    val label: String,
)

@Composable
fun CumplrBottomNav(
    items: List<CumplrNavItem>,
    selectedKey: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(CumplrSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                NavItemCell(
                    item     = item,
                    selected = item.key == selectedKey,
                    onSelect = { onItemSelected(item.key) },
                )
            }
        }
    }
}

@Composable
private fun NavItemCell(item: CumplrNavItem, selected: Boolean, onSelect: () -> Unit) {
    val tint = if (selected) CumplrAccent else CumplrFgMuted
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onSelect,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector        = item.icon,
            contentDescription = item.label,
            tint               = tint,
            modifier           = Modifier.size(22.dp),
        )
        Text(
            text  = item.label,
            color = tint,
            style = MaterialTheme.typography.labelSmall,
        )
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) CumplrAccent else CumplrSurface),
        )
    }
}
