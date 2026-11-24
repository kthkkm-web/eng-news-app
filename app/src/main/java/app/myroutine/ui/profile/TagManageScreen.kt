package app.myroutine.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myroutine.domain.model.Tag
import app.myroutine.ui.home.ConfirmDialog
import app.myroutine.ui.theme.AppTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun TagManageScreen(onBack: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val c = AppTheme.colors
    val tags by vm.tagList.collectAsStateWithLifecycle()
    var local by remember { mutableStateOf(tags) }
    LaunchedEffect(tags) { local = tags }
    var editing by remember { mutableStateOf<Tag?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Tag?>(null) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        local = local.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", tint = c.textPrimary) }
            Text("태그 관리", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { adding = true }) { Text("+ 추가", color = c.accent, fontSize = 16.sp) }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentPadding = PaddingValues(16.dp)) {
            items(local, key = { it.id }) { t ->
                ReorderableItem(reorder, key = t.id) { dragging ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .then(if (dragging) Modifier.shadow(8.dp, RoundedCornerShape(16.dp)) else Modifier)
                            .clip(RoundedCornerShape(16.dp)).background(c.surface)
                            .clickable { editing = t }.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(t.emoji.ifBlank { "🏷️" }, fontSize = 20.sp)
                        Text(t.name, color = c.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        IconButton(onClick = { deleting = t }) { Icon(Icons.Rounded.Delete, "삭제", tint = c.textTertiary) }
                        Icon(
                            Icons.Rounded.DragHandle, "순서 변경", tint = c.textTertiary,
                            modifier = Modifier.draggableHandle(onDragStopped = { vm.reorderTags(local.map { it.id }) }).padding(12.dp).size(22.dp),
                        )
                    }
                }
            }
        }
    }

    if (adding) TagDialog(null, onDismiss = { adding = false }) { n, e -> vm.addTag(n, e); adding = false }
    editing?.let { t -> TagDialog(t, onDismiss = { editing = null }) { n, e -> vm.saveTag(t.copy(name = n, emoji = e)); editing = null } }
    deleting?.let { t ->
        ConfirmDialog("태그 삭제", "'${t.name}' 태그를 삭제할까요?\n루틴은 삭제되지 않고 태그만 빠져요.", "삭제", onDismiss = { deleting = null }) { vm.deleteTag(t.id); deleting = null }
    }
}

@Composable
private fun TagDialog(existing: Tag?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val c = AppTheme.colors
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.surface,
        title = { Text(if (existing == null) "태그 추가" else "태그 수정", color = c.textPrimary) },
        text = {
            androidx.compose.foundation.layout.Column {
                OutlinedTextField(emoji, { emoji = it.take(8) }, singleLine = true, label = { Text("이모지 (선택)") })
                OutlinedTextField(name, { name = it.take(20) }, singleLine = true, label = { Text("태그 이름") }, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), emoji.trim()) }) { Text("저장", color = c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = c.textSecondary) } },
    )
}
