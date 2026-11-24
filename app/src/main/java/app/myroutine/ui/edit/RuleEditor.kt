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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myroutine.domain.model.Period
import app.myroutine.domain.model.RecordType
import app.myroutine.domain.model.RepeatRule
import app.myroutine.domain.schedule.RuleFormatter
import app.myroutine.ui.components.AppSwitch
import app.myroutine.ui.components.PillChip
import app.myroutine.ui.components.RowItem
import app.myroutine.ui.home.TimePickerDialog
import app.myroutine.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.MonthDay

private val WEEK_ORDER = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

val PRE_NOTIFY_OPTIONS = listOf(0 to "없음", 5 to "5분 전", 10 to "10분 전", 15 to "15분 전", 30 to "30분 전", 60 to "1시간 전")

/** 펼친 반복 주기 하나의 편집 영역. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuleEditorBody(rule: RepeatRule, goalEnabled: Boolean, recordType: RecordType, onChange: ((RepeatRule) -> RepeatRule) -> Unit) {
    val c = AppTheme.colors
    var pickTime by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTarget by remember { mutableStateOf(false) }
    var pickNotify by remember { mutableStateOf(false) }
    var pickPre by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        // 주기
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("주기", color = c.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val isDailyLike = rule.period == Period.DAILY
                PillChip("매일", isDailyLike) { onChange { it.copy(period = Period.DAILY, weeklyAnytime = false) } }
                PillChip("매주", rule.period == Period.WEEKLY) {
                    onChange { it.copy(period = Period.WEEKLY, weekdays = it.weekdays.ifEmpty { RepeatRule.WEEKDAYS }) }
                }
                PillChip("매월", rule.period == Period.MONTHLY) { onChange { it.copy(period = Period.MONTHLY, weeklyAnytime = false) } }
                PillChip("매년", rule.period == Period.YEARLY) { onChange { it.copy(period = Period.YEARLY, weeklyAnytime = false) } }
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(if (rule.period == Period.CUSTOM) c.accent else Color.Transparent)
                        .border(1.dp, if (rule.period == Period.CUSTOM) c.accent else c.outline, CircleShape)
                        .clickable { onChange { it.copy(period = Period.CUSTOM, weeklyAnytime = false) } },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.EditCalendar, "지정일", tint = if (rule.period == Period.CUSTOM) c.onAccent else c.textSecondary, modifier = Modifier.size(20.dp)) }
            }
        }

        when (rule.period) {
            Period.DAILY -> Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                PillChip("평일만", false) { onChange { it.copy(period = Period.WEEKLY, weekdays = RepeatRule.WEEKDAYS) } }
                Spacer(Modifier.width(8.dp))
                PillChip("주말만", false) { onChange { it.copy(period = Period.WEEKLY, weekdays = RepeatRule.WEEKEND) } }
            }
            Period.WEEKLY -> Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↳ 주중", color = c.textSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    PillChip("주중 아무때나", rule.weeklyAnytime) { onChange { it.copy(weeklyAnytime = true) } }
                    Spacer(Modifier.width(6.dp))
                    PillChip(
                        if (rule.weeklyAnytime || rule.weekdays.isEmpty()) "요일 지정" else RuleFormatter.periodLabel(rule.copy(weeklyAnytime = false)),
                        !rule.weeklyAnytime,
                    ) { onChange { it.copy(weeklyAnytime = false) } }
                }
                if (!rule.weeklyAnytime) {
                    Spacer(Modifier.size(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WEEK_ORDER.forEach { d ->
                            val on = d in rule.weekdays
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (on) c.accent else c.surfaceHigh)
                                    .clickable { onChange { r -> r.copy(weekdays = if (on) r.weekdays - d else r.weekdays + d) } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(RuleFormatter.dayName(d), color = if (on) c.onAccent else c.textSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                        listOf("평일" to RepeatRule.WEEKDAYS, "주말" to RepeatRule.WEEKEND, "매일" to WEEK_ORDER.toSet()).forEach { (label, set) ->
                            Text(
                                label, color = c.accent, fontSize = 13.sp,
                                modifier = Modifier.clip(CircleShape).clickable { onChange { it.copy(weekdays = set) } }.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
            Period.MONTHLY -> FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                maxItemsInEachRow = 7,
            ) {
                (1..31).forEach { d ->
                    val on = d in rule.monthDays
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(if (on) c.accent else c.surfaceHigh)
                                .clickable { onChange { r -> r.copy(monthDays = if (on) r.monthDays - d else r.monthDays + d) } },
                            contentAlignment = Alignment.Center,
                        ) { Text("$d", color = if (on) c.onAccent else c.textSecondary, fontSize = 13.sp) }
                    }
                }
                repeat(4) { Spacer(Modifier.weight(1f)) }
            }
            Period.YEARLY, Period.CUSTOM -> FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (rule.period == Period.YEARLY) {
                    rule.yearDates.sorted().forEach { md ->
                        RemovableChip("${md.monthValue}월 ${md.dayOfMonth}일") { onChange { it.copy(yearDates = it.yearDates - md) } }
                    }
                } else {
                    rule.customDates.sorted().forEach { d ->
                        RemovableChip(d.toString()) { onChange { it.copy(customDates = it.customDates - d) } }
                    }
                }
                Text("+ 날짜 추가", color = c.accent, fontSize = 15.sp, modifier = Modifier.clip(CircleShape).clickable { pickDate = true }.padding(8.dp))
            }
        }

        // 시간 지정
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("시간 지정", color = c.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            AppSwitch(rule.timeEnabled) { on -> onChange { it.copy(timeEnabled = on) }; if (on && rule.times.isEmpty()) pickTime = true }
        }
        if (rule.timeEnabled) {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rule.times.sorted().forEach { t ->
                    RemovableChip("%02d:%02d".format(t.hour, t.minute)) { onChange { it.copy(times = it.times - t) } }
                }
                Text("+ 추가", color = c.accent, fontSize = 15.sp, modifier = Modifier.clip(CircleShape).clickable { pickTime = true }.padding(8.dp))
            }
        }

        RowItem(
            "알림 설정",
            value = if (rule.notify) "푸시 알림" else "없음",
            valueColor = if (rule.notify) c.accent else c.textSecondary,
            onClick = { pickNotify = true },
        )
        if (rule.notify) {
            if (!rule.timeEnabled) {
                Text("시간을 지정해야 그 시각에 알림이 와요", color = c.textTertiary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
            }
            RowItem("미리 알림", value = PRE_NOTIFY_OPTIONS.firstOrNull { it.first == rule.preNotifyMinutes }?.second ?: "없음", onClick = { pickPre = true })
        }
        if (goalEnabled && recordType == RecordType.COUNT) {
            val anytime = rule.period == Period.WEEKLY && rule.weeklyAnytime
            RowItem(
                if (anytime) "주간 목표" else "목표치",
                value = if (anytime) "주 ${rule.target}회" else "${rule.target}회",
                valueColor = c.accent,
                onClick = { pickTarget = true },
            )
        }
    }

    if (pickTime) TimePickerDialog(null, onDismiss = { pickTime = false }) { t ->
        onChange { it.copy(timeEnabled = true, times = (it.times + t).distinct().sorted()) }
        pickTime = false
    }
    if (pickDate) AppDatePickerDialog(null, onDismiss = { pickDate = false }) { d ->
        onChange { if (it.period == Period.YEARLY) it.copy(yearDates = it.yearDates + MonthDay.from(d)) else it.copy(customDates = it.customDates + d) }
        pickDate = false
    }
    if (pickTarget) StepperDialog("목표치", rule.target, "회", onDismiss = { pickTarget = false }) { v -> onChange { it.copy(target = v) }; pickTarget = false }
    if (pickNotify) ChoiceDialog("알림 설정", listOf(false to "없음", true to "푸시 알림"), rule.notify, onDismiss = { pickNotify = false }) { v ->
        onChange { it.copy(notify = v) }; pickNotify = false
    }
    if (pickPre) ChoiceDialog("미리 알림", PRE_NOTIFY_OPTIONS, rule.preNotifyMinutes, onDismiss = { pickPre = false }) { v ->
        onChange { it.copy(preNotifyMinutes = v) }; pickPre = false
    }
}

@Composable
fun RemovableChip(text: String, onRemove: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier.clip(CircleShape).border(1.dp, c.outline, CircleShape).padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = c.textPrimary, fontSize = 15.sp)
        Icon(Icons.Rounded.Close, "삭제", tint = c.textTertiary, modifier = Modifier.padding(start = 2.dp).size(18.dp).clip(CircleShape).clickable(onClick = onRemove))
    }
}
