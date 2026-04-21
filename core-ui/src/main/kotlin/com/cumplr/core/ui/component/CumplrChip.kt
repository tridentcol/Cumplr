package com.cumplr.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cumplr.core.domain.enums.TaskStatus
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusAssignedBg
import com.cumplr.core.ui.theme.CumplrStatusAssignedFg
import com.cumplr.core.ui.theme.CumplrStatusDoneBg
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueBg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressBg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrStatusSubmittedBg
import com.cumplr.core.ui.theme.CumplrStatusSubmittedFg
import com.cumplr.core.ui.theme.Radius
import com.cumplr.core.ui.theme.Spacing

private data class ChipStyle(val bg: Color, val fg: Color, val label: String)

private fun chipStyleFor(status: TaskStatus) = when (status) {
    TaskStatus.ASSIGNED     -> ChipStyle(CumplrStatusAssignedBg,  CumplrStatusAssignedFg,  "Asignada")
    TaskStatus.IN_PROGRESS  -> ChipStyle(CumplrStatusProgressBg,  CumplrStatusProgressFg,  "En progreso")
    TaskStatus.SUBMITTED    -> ChipStyle(CumplrStatusSubmittedBg, CumplrStatusSubmittedFg, "Enviada")
    TaskStatus.UNDER_REVIEW -> ChipStyle(CumplrStatusSubmittedBg, CumplrStatusSubmittedFg, "En revisión")
    TaskStatus.APPROVED     -> ChipStyle(CumplrStatusDoneBg,      CumplrStatusDoneFg,      "Aprobada")
    TaskStatus.REJECTED     -> ChipStyle(CumplrStatusOverdueBg,   CumplrStatusOverdueFg,   "Rechazada")
}

@Composable
fun CumplrChip(
    status: TaskStatus,
    modifier: Modifier = Modifier,
) {
    val style = chipStyleFor(status)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(style.bg)
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(style.fg),
        )
        Text(
            text  = style.label,
            color = style.fg,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
