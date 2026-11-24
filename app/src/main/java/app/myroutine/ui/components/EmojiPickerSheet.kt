package app.myroutine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.sp
import app.myroutine.ui.theme.AppTheme

object EmojiSets {
    val categories: List<Pair<String, List<String>>> = listOf(
        "자주 쓰는" to listOf("📰", "🏋️", "🎧", "✏️", "📚", "🏃", "🧘", "💧", "💊", "🛏️", "⏰", "🥗"),
        "운동·건강" to listOf("💪", "🚴", "🏊", "⛰️", "🤸", "🚶", "🧗", "⚽", "🎾", "🏀", "🩺", "🦷", "🍎", "🥦", "🫁", "⚖️"),
        "공부·일" to listOf("📖", "📝", "💻", "🗂️", "📅", "🧠", "🌐", "🔤", "🎓", "📈", "💡", "✅"),
        "생활" to listOf("🧹", "🧺", "🍳", "🛒", "🪴", "🐶", "🐱", "🚿", "🧴", "👕", "🗑️", "💰", "🏠", "🚗", "📞", "✉️"),
        "마음·취미" to listOf("☀️", "🌙", "🙏", "😊", "🎨", "🎸", "🎹", "📷", "🎬", "🎮", "✍️", "🌿", "❤️", "⭐", "🔥", "🎯"),
        "금지" to listOf("🚫", "🚭", "🍺", "📵", "🍔", "🍬", "☕", "🎰"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val c = AppTheme.colors
    var custom by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("이모지 선택", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { onPick("") }) { Text("없음", color = c.textSecondary) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = custom, onValueChange = { custom = it.take(8) },
                    placeholder = { Text("키보드로 직접 입력") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { if (custom.isNotBlank()) onPick(custom.trim()) }) { Text("사용", color = c.accent) }
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(GridCells.Fixed(8), modifier = Modifier.fillMaxWidth().height(380.dp)) {
                EmojiSets.categories.forEach { (title, list) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(title, color = c.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    }
                    items(list) { e ->
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                .then(if (e == current) Modifier.background(c.surfaceHigh) else Modifier)
                                .clickable { onPick(e) },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 24.sp) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

