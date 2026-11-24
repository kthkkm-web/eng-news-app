package app.myroutine.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myroutine.ui.theme.AppTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val c = AppTheme.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text("확인", color = c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = c.textSecondary) } },
    ) { DatePicker(state, showModeToggle = false) }
}

@Composable
fun StepperDialog(title: String, initial: Int, suffix: String, range: IntRange = 1..99, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val c = AppTheme.colors
    var v by remember { mutableIntStateOf(initial.coerceIn(range)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.textPrimary) },
        text = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                FilledTonalIconButton(onClick = { if (v > range.first) v-- }) { Icon(Icons.Rounded.Remove, "감소") }
                Spacer(Modifier.width(24.dp))
                Text("$v$suffix", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(24.dp))
                FilledTonalIconButton(onClick = { if (v < range.last) v++ }) { Icon(Icons.Rounded.Add, "증가") }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(v) }) { Text("확인", color = c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = c.textSecondary) } },
    )
}

@Composable
fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    val c = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.textPrimary) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(value) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = { onPick(value) }, colors = RadioButtonDefaults.colors(selectedColor = c.accent))
                        Text(label, color = c.textPrimary, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기", color = c.textSecondary) } },
    )
}

@Composable
fun TagCreateDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    val c = AppTheme.colors
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("태그 추가", color = c.textPrimary) },
        text = {
            Column {
                OutlinedTextField(emoji, { emoji = it.take(8) }, singleLine = true, label = { Text("이모지 (선택)") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it.take(20) }, singleLine = true, label = { Text("태그 이름") })
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name, emoji) }) { Text("추가", color = c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = c.textSecondary) } },
    )
}

@Composable
fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.surface,
        title = { Text(title, color = c.textPrimary) }, text = { Text(message, color = c.textSecondary) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("확인", color = c.accent) } },
    )
}
