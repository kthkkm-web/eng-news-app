package app.myroutine.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import app.myroutine.domain.model.RecordType
import app.myroutine.ui.theme.AppTheme
import app.myroutine.ui.theme.MyRoutineTheme
import kotlinx.coroutines.launch

/**
 * 위젯 설정: 그래프(4·5) → 수치 루틴, 달성 흐름(6) → 태그 또는 루틴, 오늘의 루틴(1)·주간 타임라인(8) → 태그 필터.
 */
class WidgetConfigActivity : ComponentActivity() {
    private data class Option(val id: String?, val label: String, val section: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className.orEmpty()

        lifecycleScope.launch {
            val w = WidgetDataSource.context(this@WidgetConfigActivity)
            val tagOptions = listOf(Option(null, "전체 루틴", "태그")) + w.tags.map { Option(it.id, "${it.emoji} ${it.name}".trim(), "태그") }
            val (title, options) = when {
                provider.endsWith("GraphLineReceiver") || provider.endsWith("GraphBarReceiver") ->
                    "그래프로 볼 수치 루틴" to w.routines.filter { it.recordType == RecordType.NUMERIC }.map { Option(it.id, "${it.emoji}${it.name}", "수치 루틴") }
                provider.endsWith("StreakFlowReceiver") ->
                    "달성 흐름 대상" to (tagOptions + w.routines.map { Option(it.id, "${it.emoji}${it.name}", "루틴") })
                else -> "표시할 태그" to tagOptions
            }
            setContent {
                MyRoutineTheme {
                    val c = AppTheme.colors
                    var selected by remember { mutableStateOf(options.firstOrNull()?.id) }
                    Column(Modifier.fillMaxSize().background(c.background).safeDrawingPadding()) {
                        Text(title, color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
                        if (options.isEmpty()) {
                            Text("수치형 루틴이 없어요. 앱에서 기록 방식을 '수치'로 한 루틴을 만든 뒤 다시 설정해 주세요. (지금은 빈 그래프로 추가돼요)", color = c.textSecondary, modifier = Modifier.padding(20.dp))
                        }
                        LazyColumn(Modifier.weight(1f)) {
                            var lastSection = ""
                            options.forEach { o ->
                                if (o.section != lastSection) {
                                    lastSection = o.section
                                    item(key = "s-${o.section}") { Text(o.section, color = c.textTertiary, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, top = 12.dp)) }
                                }
                                item(key = "o-${o.section}-${o.id}") {
                                    Row(Modifier.fillMaxWidth().clickable { selected = o.id }.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = selected == o.id, onClick = { selected = o.id }, colors = RadioButtonDefaults.colors(selectedColor = c.accent))
                                        Text(o.label, color = c.textPrimary, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = c.outline)
                        Text(
                            "완료", color = c.onAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(16.dp).background(c.accent, androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                                .clickable { save(widgetId, selected) }.padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    private fun save(widgetId: Int, target: String?) = lifecycleScope.launch {
        val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(widgetId)
        updateAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                if (target == null) remove(WidgetKeys.TARGET) else this[WidgetKeys.TARGET] = target
                this[WidgetKeys.VERSION] = System.currentTimeMillis()
            }
        }
        val provider = AppWidgetManager.getInstance(this@WidgetConfigActivity).getAppWidgetInfo(widgetId)?.provider?.className
        WidgetUpdater.forReceiver(provider)?.update(this@WidgetConfigActivity, glanceId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
