package com.cumplr.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cumplr.core.domain.model.Note
import com.cumplr.core.ui.theme.CumplrAccent
import com.cumplr.core.ui.theme.CumplrBorder
import com.cumplr.core.ui.theme.CumplrFg
import com.cumplr.core.ui.theme.CumplrFgMuted
import com.cumplr.core.ui.theme.CumplrSurface
import com.cumplr.core.ui.theme.CumplrSurface2
import com.cumplr.core.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotesSection(
    notes: List<Note>,
    onAddNote: (String) -> Unit,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
) {
    var noteText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CumplrSurface)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text  = "Notas",
            style = MaterialTheme.typography.labelSmall,
            color = CumplrAccent,
        )

        if (notes.isEmpty()) {
            Text(
                text  = "Sin notas aún.",
                style = MaterialTheme.typography.bodySmall,
                color = CumplrFgMuted,
            )
        } else {
            notes.forEach { note ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text  = note.authorName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CumplrFg,
                        )
                        Text(
                            text  = formatNoteTime(note.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = CumplrFgMuted,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = note.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = CumplrFg,
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = Spacing.xs), color = CumplrSurface2)
                }
            }
        }

        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            OutlinedTextField(
                value         = noteText,
                onValueChange = { if (it.length <= 500) noteText = it },
                placeholder   = { Text("Añadir nota…", style = MaterialTheme.typography.bodySmall) },
                singleLine    = false,
                maxLines      = 3,
                modifier      = Modifier.weight(1f),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = CumplrAccent,
                    unfocusedBorderColor    = CumplrBorder,
                    focusedTextColor        = CumplrFg,
                    unfocusedTextColor      = CumplrFg,
                    focusedContainerColor   = CumplrSurface,
                    unfocusedContainerColor = CumplrSurface,
                    cursorColor             = CumplrAccent,
                ),
                shape = RoundedCornerShape(8.dp),
            )
            IconButton(
                onClick  = {
                    if (noteText.isNotBlank() && !isSubmitting) {
                        onAddNote(noteText.trim())
                        noteText = ""
                    }
                },
                enabled = noteText.isNotBlank() && !isSubmitting,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CumplrAccent, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Send, contentDescription = "Enviar nota", tint = CumplrAccent)
                }
            }
        }
    }
}

private fun formatNoteTime(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.getDefault()))
} catch (_: Exception) { "" }
