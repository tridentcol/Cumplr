package com.cumplr.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cumplr.core.domain.enums.TaskPriority
import com.cumplr.core.domain.model.Task
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrStatusDoneFg
import com.cumplr.core.ui.theme.CumplrStatusOverdueFg
import com.cumplr.core.ui.theme.CumplrStatusProgressFg
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.Radius
import com.cumplr.core.ui.theme.Spacing
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) CumplrSurface2 else CumplrSurface,
        label       = "taskCardBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(Radius.lg))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        // Priority indicator strip
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(task.priority.indicatorColor()),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(Spacing.lg),
        ) {
            // Title + status chip
            Row(
                modifier             = Modifier.fillMaxWidth(),
                verticalAlignment    = Alignment.Top,
            ) {
                Text(
                    text     = task.title,
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color    = CumplrFg,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.sm))
                CumplrChip(status = task.status)
            }

            Spacer(Modifier.height(Spacing.sm))

            // Deadline + location + assigner
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val deadline = task.deadline
                val location = task.location.orEmpty()

                if (deadline != null) {
                    val dlColor = deadlineColor(deadline)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint               = dlColor,
                            modifier           = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text  = formatDeadlineText(deadline),
                            style = MaterialTheme.typography.bodySmall,
                            color = dlColor,
                        )
                    }
                    if (location.isNotBlank()) Spacer(Modifier.width(Spacing.md))
                }

                if (location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint               = CumplrFgMuted,
                            modifier           = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text     = location,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = CumplrFgMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                if (assignerName != null) {
                    Text(
                        text  = assignerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = CumplrFgMuted,
                    )
                }
            }
        }
    }
}

private fun TaskPriority.indicatorColor(): Color = when (this) {
    TaskPriority.HIGH   -> CumplrStatusOverdueFg
    TaskPriority.MEDIUM -> CumplrStatusProgressFg
    TaskPriority.LOW    -> CumplrFgMuted
}

private fun deadlineColor(deadline: String): Color = try {
    val hours = Duration.between(Instant.now(), Instant.parse(deadline)).toHours()
    when {
        hours < 0  -> CumplrStatusOverdueFg
        hours < 24 -> CumplrStatusProgressFg
        else       -> CumplrStatusDoneFg
    }
} catch (_: Exception) { CumplrFgMuted }

private fun formatDeadlineText(deadline: String): String = try {
    val zdt      = Instant.parse(deadline).atZone(ZoneId.systemDefault())
    val today    = LocalDate.now(ZoneId.systemDefault())
    val date     = zdt.toLocalDate()
    val timeFmt  = DateTimeFormatter.ofPattern("HH:mm")
    val time     = zdt.format(timeFmt)
    when (date) {
        today             -> "Hoy $time"
        today.plusDays(1) -> "Mañana $time"
        else              -> zdt.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))
    }
} catch (_: Exception) { deadline }
