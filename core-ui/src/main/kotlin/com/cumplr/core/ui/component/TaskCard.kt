package com.cumplr.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
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
    CumplrCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        // Title + status chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = CumplrFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.sm))
            CumplrChip(status = task.status)
        }

        // Description
        val description = task.description
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = CumplrFgMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        // Deadline + assigner row
        val deadline = task.deadline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (deadline != null) {
                Text(
                    text = formatDeadline(deadline),
                    style = MaterialTheme.typography.bodySmall,
                    color = deadlineColor(deadline),
                )
            }
            if (assignerName != null) {
                Text(
                    text = assignerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = CumplrFgMuted,
                )
            }
        }
    }
}

private fun deadlineColor(deadline: String): Color {
    return try {
        val deadlineInstant = Instant.parse(deadline)
        val now = Instant.now()
        val hours = java.time.Duration.between(now, deadlineInstant).toHours()
        when {
            hours < 0  -> CumplrStatusOverdueFg
            hours < 24 -> CumplrStatusProgressFg
            else       -> CumplrStatusDoneFg
        }
    } catch (_: Exception) {
        CumplrFgMuted
    }
}

private fun formatDeadline(deadline: String): String {
    return try {
        val instant = Instant.parse(deadline)
        val zdt = instant.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale.getDefault())
        "Vence: ${zdt.format(formatter)}"
    } catch (_: Exception) {
        deadline
    }
}
