package app.myroutine.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myroutine.domain.model.RecordType
import app.myroutine.ui.components.CardShape
import app.myroutine.ui.components.PieCheck
import app.myroutine.ui.components.ProgressRing
import app.myroutine.ui.theme.AppTheme
import app.myroutine.ui.theme.RoutinePalettes

/** ⋮ 메뉴에서 고를 수 있는 동작. */
enum class ItemAction(val label: String) { EDIT("수정"), MINUS("-1"), RESET("기록 초기화"), ARCHIVE("보관"), DELETE("삭제") }

fun HomeItem.color(): Color = RoutinePalettes.color(routine.color)

/** "📰뉴스 읽기 1/3회" — 진행 숫자만 루틴 색. */
@Composable
fun itemTitle(item: HomeItem, showCount: Boolean = true) = buildAnnotatedString {
    val c = AppTheme.colors
    append(item.routine.emoji)
    append(item.routine.name)
    if (!showCount) return@buildAnnotatedString
    append(" ")
    if (item.routine.recordType == RecordType.NUMERIC) {
        val v = item.log?.value
        withStyle(SpanStyle(color = item.color(), fontWeight = FontWeight.Bold)) {
            append(if (v == null) "-" else formatValue(v, item.routine.decimals))
        }
        item.routine.unit?.let { withStyle(SpanStyle(color = c.textSecondary)) { append(it) } }
    } else {
        withStyle(SpanStyle(color = item.color(), fontWeight = FontWeight.Bold)) { append("${item.count}") }
        append("/${item.target}회")
    }
}

fun formatValue(v: Double, decimals: Int): String = "%.${decimals.coerceIn(0, 3)}f".format(v)

@Composable
fun ItemMenuButton(item: HomeItem, onAction: (ItemAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val c = AppTheme.colors
    Box {
        Icon(
            Icons.Rounded.MoreVert, "메뉴", tint = c.textTertiary,
            modifier = Modifier.size(32.dp).clip(CircleShape).combinedClickable(onClick = { open = true }).padding(4.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surfaceHigh) {
            ItemAction.entries.forEach { a ->
                if (a == ItemAction.MINUS && (item.count == 0 || item.routine.recordType == RecordType.NUMERIC)) return@forEach
                DropdownMenuItem(
                    text = { Text(a.label, color = if (a == ItemAction.DELETE) c.danger else c.textPrimary) },
                    onClick = { open = false; onAction(a) },
                )
            }
        }
    }
}

@Composable
fun TimeChip(text: String) {
    val c = AppTheme.colors
    Text(
        text, color = c.textSecondary, fontSize = 12.sp,
        modifier = Modifier.clip(CircleShape).background(c.surfaceHigh).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** 진행 중(부분 달성)이면 오른쪽 위 접힌 모서리. */
@Composable
private fun CornerFold(color: Color, modifier: Modifier) {
    Canvas(modifier.size(26.dp)) {
        val p = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); close() }
        drawPath(p, color)
    }
}

@Composable
private fun ColorBar(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.width(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
}

// ---------------------------------------------------------------- 1. 카드형

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineCard(item: HomeItem, modifier: Modifier, onTap: () -> Unit, onLongPress: (() -> Unit)?, onAction: (ItemAction) -> Unit) {
    val c = AppTheme.colors
    val color = item.color()
    Box(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.surface)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        Row(Modifier.padding(start = 0.dp, top = 16.dp, bottom = 14.dp, end = 8.dp)) {
            ColorBar(color, Modifier.padding(top = 2.dp).height(40.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.periodLabel, color = c.textSecondary, fontSize = 13.sp)
                    item.timeLabel?.let { TimeChip(it) }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    itemTitle(item), color = if (item.complete) c.textSecondary else c.textPrimary, fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.complete) TextDecoration.LineThrough else null,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    item.history.forEach { HistoryIcon(it, color) }
                    if (item.history.isNotEmpty()) Text("최근", color = c.textTertiary, fontSize = 11.sp)
                }
            }
            ItemMenuButton(item, onAction)
        }
        when {
            item.complete -> Box(
                Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 44.dp).size(22.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = Color.Black.copy(alpha = 0.7f), modifier = Modifier.size(16.dp)) }
            item.progress > 0f -> CornerFold(color, Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
fun HistoryIcon(mark: HistoryMark, color: Color) {
    val c = AppTheme.colors
    when (mark) {
        HistoryMark.Missed -> Icon(Icons.Rounded.Close, "미달성", tint = c.textTertiary, modifier = Modifier.size(20.dp))
        is HistoryMark.Partial -> ProgressRing(mark.progress, color, 20.dp, stroke = 2.5.dp)
        HistoryMark.Done -> Box(Modifier.size(20.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, "완료", tint = Color.Black.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        }
    }
}

// ---------------------------------------------------------------- 2. 2열 그리드

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineGridCell(item: HomeItem, modifier: Modifier, onTap: () -> Unit, onLongPress: (() -> Unit)?, onAction: (ItemAction) -> Unit) {
    val c = AppTheme.colors
    val color = item.color()
    Box(modifier.clip(CardShape).background(c.surface).combinedClickable(onClick = onTap, onLongClick = onLongPress)) {
        Row(Modifier.padding(top = 14.dp, bottom = 14.dp, end = 4.dp)) {
            ColorBar(color, Modifier.height(36.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.periodLabel, color = c.textSecondary, fontSize = 12.sp, maxLines = 1)
                    item.timeLabel?.let { TimeChip(it) }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        append(item.routine.emoji); append(item.routine.name)
                        if (item.routine.recordType == RecordType.COUNT && item.target > 1) append(" ${item.target}회")
                    },
                    color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { item.progress }, color = color, trackColor = c.surfaceHigh,
                        strokeCap = StrokeCap.Round, drawStopIndicator = {}, gapSize = 0.dp,
                        modifier = Modifier.weight(1f).height(8.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = color)) {
                                append(if (item.routine.recordType == RecordType.NUMERIC) item.log?.value?.let { formatValue(it, item.routine.decimals) } ?: "-" else "${item.count}")
                            }
                            append(if (item.routine.recordType == RecordType.NUMERIC) item.routine.unit.orEmpty() else "/${item.target}")
                        },
                        color = c.textTertiary, fontSize = 13.sp,
                    )
                }
            }
            ItemMenuButton(item, onAction)
        }
        if (item.progress > 0f && !item.complete) CornerFold(color, Modifier.align(Alignment.TopEnd).size(20.dp))
    }
}

// ---------------------------------------------------------------- 3. 컴팩트 리스트

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineCompactRow(item: HomeItem, modifier: Modifier, onTap: () -> Unit, onLongPress: (() -> Unit)?, onAction: (ItemAction) -> Unit) {
    val c = AppTheme.colors
    val color = item.color()
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).combinedClickable(onClick = onTap, onLongClick = onLongPress)) {
        Row(Modifier.padding(vertical = 14.dp).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ColorBar(color, Modifier.height(28.dp))
            Text(
                itemTitle(item), color = if (item.complete) c.textSecondary else c.textPrimary, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (item.complete) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f).padding(start = 14.dp),
            )
            item.timeLabel?.let { TimeChip(it) }
            ItemMenuButton(item, onAction)
        }
        if (item.progress > 0f && !item.complete) CornerFold(color, Modifier.align(Alignment.TopEnd).size(20.dp))
    }
}

// ---------------------------------------------------------------- 4. 체크 리스트 (한 카드 안의 행)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineCheckRow(item: HomeItem, modifier: Modifier, onTap: () -> Unit, onLongPress: (() -> Unit)?, onAction: (ItemAction) -> Unit) {
    val c = AppTheme.colors
    val color = item.color()
    Row(
        modifier.fillMaxWidth().background(c.surface).combinedClickable(onClick = {}, onLongClick = onLongPress).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).combinedClickable(onClick = onTap), contentAlignment = Alignment.Center) {
            PieCheck(item.progress, color, 30.dp)
        }
        Text(
            itemTitle(item), color = if (item.complete) c.textSecondary else c.textPrimary, fontSize = 16.sp,
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textDecoration = if (item.complete) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        item.timeLabel?.let { TimeChip(it) }
        ItemMenuButton(item, onAction)
    }
}

