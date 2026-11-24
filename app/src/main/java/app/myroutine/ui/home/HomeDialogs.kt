package app.myroutine.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.myroutine.domain.model.Todo
import app.myroutine.ui.theme.AppTheme
import java.time.LocalTime

@Composable
fun NumericInputDialog(item: HomeItem, onDismiss: () -> Unit, onSave: (Double?) -> Unit) {
    val c = AppTheme.colors
    val r = item.routine
    var text by remember { mutableStateOf(item.log?.value?.let { formatValue(it, r.decimals) } ?: "") }
    val parsed = text.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("${r.emoji}${r.name} 기록", color = c.textPrimary) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(12) }, singleLine = true,
                suffix = { r.unit?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = { Text("값 입력") },
            )
        },
        confirmButton = { TextButton(enabled = parsed != null, onClick = { onSave(parsed) }) { Text("저장", color = c.accent) } },
        dismissButton = {
            Row {
                if (item.log?.value != null) TextButton(onClick = { onSave(null) }) { Text("기록 삭제", color = c.danger) }
                TextButton(onClick = onDismiss) { Text("취소", color = c.textSecondary) }
            }
        },
    )
}

@Composable
fun ConfirmDialog(title: String, message: String, confirm: String, danger: Boolean = true, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val c = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = c.textPrimary) },
        text = { Text(message, color = c.textSecondary) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (danger) c.danger else c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = c.textSecondary) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: LocalTime?, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val c = AppTheme.colors
    val t = initial ?: LocalTime.of(9, 0)
    val state = rememberTimePickerState(t.hour, t.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        text = { TimePicker(state) },
        confirmButton = { TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) { Text("확인", color = c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = c.textSecondary) } },
    )
}

/** 할 일 추가/수정. existing=null 이면 추가. */
@Composable
fun TodoEditDialog(existing: Todo?, onDismiss: () -> Unit, onSave: (String, LocalTime?) -> Unit, onDelete: (() -> Unit)? = null) {
    val c = AppTheme.colors
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var time by remember { mutableStateOf(existing?.time) }
    var picking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(if (existing == null) "할 일 추가" else "할 일 수정", color = c.textPrimary) },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it.take(60) }, singleLine = true, placeholder = { Text("예: 공과금 납부") })
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { picking = true }) {
                        Text(time?.let { "시간 %02d:%02d".format(it.hour, it.minute) } ?: "+ 시간 지정", color = c.accent)
                    }
                    if (time != null) TextButton(onClick = { time = null }) { Text("시간 없음", color = c.textSecondary) }
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(title.trim(), time) }) { Text("저장", color = c.accent) } },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("삭제", color = c.danger) }
                TextButton(onClick = onDismiss) { Text("취소", color = c.textSecondary) }
            }
        },
    )
    if (picking) TimePickerDialog(time, onDismiss = { picking = false }) { time = it; picking = false }
}
