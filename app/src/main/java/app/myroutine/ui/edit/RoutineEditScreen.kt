package app.myroutine.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.ui.components.AppSwitch
import app.myroutine.ui.components.ColorDot
import app.myroutine.ui.components.ColorPickerSheet
import app.myroutine.ui.components.EmojiPickerSheet
import app.myroutine.ui.components.PillChip
import app.myroutine.ui.components.SwipeAction
import app.myroutine.ui.components.SwipeActionBox
import app.myroutine.ui.theme.AppTheme
import app.myroutine.ui.theme.RoutinePalettes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineEditScreen(
    onBack: () -> Unit,
    onManageTags: () -> Unit,
    vm: RoutineEditViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = AppTheme.colors
    val routineColor = RoutinePalettes.color(s.color)
    var emojiSheet by remember { mutableStateOf(false) }
    var colorSheet by remember { mutableStateOf(false) }
    var tagDialog by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.eventFlow.collect { if (it is EditEvent.Saved) onBack() } }

    Column(Modifier.fillMaxSize().background(c.surface).imePadding()) {
        // 상단 바
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", tint = c.textPrimary) }
            Text(
                if (s.isNew) "루틴 추가" else "",
                color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.clip(CircleShape).background(c.textPrimary).clickable(onClick = vm::save).padding(horizontal = 20.dp, vertical = 10.dp),
            ) { Text(if (s.isNew) "저장" else "루틴 수정", color = c.surface, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // 요약 문구
            if (s.name.isNotBlank()) {
                Text(
                    "${RuleFormatter.summary(s.rules.map { it.rule }, s.goalEnabled && s.recordType == RecordType.COUNT)} ${s.emoji}${s.name}",
                    color = routineColor, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            // 이름 / 이모지 / 색
            Text(buildLabel("루틴", required = true), color = c.textPrimary, fontSize = 16.sp, modifier = Modifier.padding(start = 20.dp, top = 16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c.surfaceHigh).clickable { emojiSheet = true },
                    contentAlignment = Alignment.Center,
                ) { Text(s.emoji.ifBlank { "🙂" }, fontSize = 24.sp, color = if (s.emoji.isBlank()) c.textTertiary else c.textPrimary) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (s.name.isEmpty()) Text("책 읽기, 5km 러닝 등", color = c.textTertiary, fontSize = 20.sp)
                            BasicTextField(
                                value = s.name, onValueChange = { v -> vm.update { it.copy(name = v.take(40)) } },
                                singleLine = true, textStyle = TextStyle(color = c.textPrimary, fontSize = 20.sp),
                                cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (s.name.isNotEmpty()) Icon(Icons.Rounded.Close, "지우기", tint = c.textTertiary, modifier = Modifier.size(22.dp).clickable { vm.update { it.copy(name = "") } })
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = c.outline)
                }
                Spacer(Modifier.width(16.dp))
                ColorDot(routineColor, 56.dp, Modifier.clip(CircleShape).clickable { colorSheet = true })
            }
            ToggleRow("목표치 설정", s.goalEnabled, onHelp = { help = "목표치 설정" to "켜면 반복 주기마다 하루 목표 횟수(예: 3회)를 정할 수 있어요. 끄면 하루 1번 하면 완료예요." }) { v -> vm.update { it.copy(goalEnabled = v) } }

            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("기록 방식", color = c.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                PillChip("횟수", s.recordType == RecordType.COUNT) { vm.update { it.copy(recordType = RecordType.COUNT) } }
                Spacer(Modifier.width(6.dp))
                PillChip("수치 (몸무게 등)", s.recordType == RecordType.NUMERIC) { vm.update { it.copy(recordType = RecordType.NUMERIC) } }
            }
            if (s.recordType == RecordType.NUMERIC) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = s.unit, onValueChange = { v -> vm.update { it.copy(unit = v.take(8)) } },
                        label = { Text("단위") }, placeholder = { Text("kg") }, singleLine = true, modifier = Modifier.width(120.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Text("소수점", color = c.textSecondary, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    listOf(0, 1, 2).forEach { d ->
                        PillChip("$d", s.decimals == d, Modifier.padding(start = 4.dp)) { vm.update { it.copy(decimals = d) } }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Gap()

            // 반복 주기
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("반복 주기", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (s.rules.size > 1) Text("반복 주기를 삭제하려면 스와이프하세요", color = c.textTertiary, fontSize = 12.sp)
            }
            s.rules.forEachIndexed { i, draft ->
                SwipeActionBox(
                    leftAction = if (s.rules.size > 1) SwipeAction(Icons.Rounded.Delete, "삭제", c.danger, androidx.compose.ui.graphics.Color.White) { vm.removeRule(i) } else null,
                    rightAction = null,
                    shape = RoundedCornerShape(0.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().background(c.surface).clickable { vm.toggleRuleExpanded(i) }.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            RuleFormatter.ruleText(draft.rule, s.goalEnabled && s.recordType == RecordType.COUNT),
                            color = c.accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        )
                        if (draft.rule.notify) Icon(Icons.Rounded.NotificationsNone, "알림", tint = c.accent, modifier = Modifier.padding(start = 4.dp).size(20.dp))
                        Spacer(Modifier.weight(1f))
                        Icon(if (draft.expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = c.textPrimary)
                    }
                }
                if (draft.expanded) {
                    RuleEditorBody(draft.rule, s.goalEnabled, s.recordType) { block -> vm.updateRule(i, block) }
                }
                HorizontalDivider(color = c.outline, modifier = Modifier.padding(horizontal = 20.dp))
            }
            Text(
                "+ 반복 주기 추가", color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clickable(onClick = vm::addRule).padding(vertical = 18.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Gap()

            ToggleRow("빠른 기록", s.quickRecord, onHelp = { help = "빠른 기록" to "켜면 탭 한 번에 그날 목표치를 모두 채워 완료 처리해요." }) { v -> vm.update { it.copy(quickRecord = v) } }
            Gap()

            // 태그
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("태그", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("태그 관리", color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onManageTags))
            }
            FlowRow(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillChip("태그 없음", s.tagIds.isEmpty()) { vm.toggleTag(null) }
                s.allTags.forEach { t -> PillChip("${t.emoji} ${t.name}".trim(), t.id in s.tagIds) { vm.toggleTag(t.id) } }
                Text("+ 추가", color = c.accent, fontSize = 15.sp, modifier = Modifier.clip(CircleShape).clickable { tagDialog = true }.padding(8.dp))
            }
            Spacer(Modifier.height(24.dp))

            // 기간
            Text("기간 설정", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                DateBox(s.startDate.toString(), placeholder = false, Modifier.weight(1f)) { pickStart = true }
                Text("  –  ", color = c.textSecondary)
                DateBox(s.endDate?.toString() ?: "종료일 (선택)", placeholder = s.endDate == null, Modifier.weight(1f), onClear = if (s.endDate != null) ({ vm.update { it.copy(endDate = null) } }) else null) { pickEnd = true }
            }
            Spacer(Modifier.height(40.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
        s.error?.let {
            Text(
                it, color = c.onAccent, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().background(c.danger).navigationBarsPadding().padding(16.dp),
            )
        }
    }

    if (emojiSheet) EmojiPickerSheet(s.emoji, onDismiss = { emojiSheet = false }) { e -> vm.update { it.copy(emoji = e) }; emojiSheet = false }
    if (colorSheet) ColorPickerSheet(s.color, onDismiss = { colorSheet = false }) { k -> vm.update { it.copy(color = k) }; colorSheet = false }
    if (tagDialog) TagCreateDialog(onDismiss = { tagDialog = false }) { n, e -> vm.createTag(n, e); tagDialog = false }
    help?.let { (t, m) -> InfoDialog(t, m) { help = null } }
    if (pickStart) AppDatePickerDialog(s.startDate, onDismiss = { pickStart = false }) { d -> vm.update { it.copy(startDate = d) }; pickStart = false }
    if (pickEnd) AppDatePickerDialog(s.endDate ?: s.startDate.plusMonths(1), onDismiss = { pickEnd = false }) { d -> vm.update { it.copy(endDate = d) }; pickEnd = false }
}

private fun buildLabel(text: String, required: Boolean) = androidx.compose.ui.text.buildAnnotatedString {
    append(text)
    if (required) {
        pushStyle(androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color(0xFFFF6B6B)))
        append(" *")
        pop()
    }
}

@Composable
private fun Gap() {
    Spacer(Modifier.fillMaxWidth().height(10.dp).background(AppTheme.colors.background))
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onHelp: (() -> Unit)? = null, onChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = c.textPrimary, fontSize = 16.sp)
        if (onHelp != null) Icon(Icons.Outlined.HelpOutline, "도움말", tint = c.textTertiary, modifier = Modifier.padding(start = 6.dp).size(20.dp).clickable(onClick = onHelp))
        Spacer(Modifier.weight(1f))
        AppSwitch(checked, onChange)
    }
}

@Composable
private fun DateBox(text: String, placeholder: Boolean, modifier: Modifier, onClear: (() -> Unit)? = null, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, c.outline, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = if (placeholder) c.textTertiary else c.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (onClear != null) Icon(Icons.Rounded.Close, "지우기", tint = c.textTertiary, modifier = Modifier.size(18.dp).clickable(onClick = onClear))
        else Icon(Icons.Rounded.CalendarMonth, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
    }
}

