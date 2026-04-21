package com.cumplr.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cumplr.core.domain.model.Task
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    assignerName: String? = null,
) {
    CumplrCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text       = task.title,
                style      = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color      = CumplrFg,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f),
            )
            CumplrChip(status = task.status, modifier = Modifier)
        }

        Spacer(Modifier.height(Spacing.sm))

        // Time + location / assigner row
        val deadline  = task.deadline
        val location  = task.location.orEmpty()
        val assigner  = assignerName

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (deadline != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint     = CumplrFgMuted,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text  = formatDeadlineTime(deadline),
                        style = MaterialTheme.typography.bodySmall,
                        color = deadlineColor(deadline),
                    )
                }
            }
            if (location.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint     = CumplrFgMuted,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text     = location,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = CumplrFgMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (assigner != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    text  = assigner,
                    style = MaterialTheme.typography.bodySmall,
                    color = CumplrFgMuted,
                )
            }
        }
    }
}

private fun deadlineColor(deadline: String) = try {
    val hours = java.time.Duration.between(Instant.now(), Instant.parse(deadline)).toHours()
    when {
        hours < 0  -> CumplrStatusOverdueFg
        hours < 24 -> CumplrStatusProgressFg
        else       -> CumplrStatusDoneFg
    }
} catch (_: Exception) { CumplrFgMuted }

private fun formatDeadlineTime(deadline: String): String = try {
    val zdt = Instant.parse(deadline).atZone(ZoneId.systemDefault())
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(zdt)
} catch (_: Exception) { deadline }
