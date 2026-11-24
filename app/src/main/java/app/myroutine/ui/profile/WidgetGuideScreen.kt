package app.myroutine.ui.profile

import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import app.myroutine.ui.theme.AppTheme
import app.myroutine.widget.DataWidget
import app.myroutine.widget.WidgetUpdater

private data class GuideItem(val title: String, val size: String, val desc: String)

private val GUIDE = listOf(
    GuideItem("오늘의 루틴", "2×2", "오늘 할 루틴 목록. 행을 누르면 앱을 열지 않고 +1 기록돼요. 길게 눌러 '설정'에서 태그를 고를 수 있어요."),
    GuideItem("오늘의 할 일", "2×2", "오늘 할 일 목록. 행을 누르면 완료 체크, + 를 누르면 바로 추가해요."),
    GuideItem("타임라인 (월간)", "2×2", "이번 달 전체 루틴 달성률을 색 농도로 보여줘요. 오늘 칸은 테두리로 강조돼요."),
    GuideItem("루틴 그래프 (전체)", "4×2", "수치 기록 루틴(몸무게 등)의 전체 기간 라인 그래프. 최고·최저 값을 표시해요."),
    GuideItem("루틴 그래프 (최근)", "4×2", "수치 기록 루틴의 최근 8회 기록을 막대 그래프로 보여줘요."),
    GuideItem("루틴 달성 흐름", "4×2", "태그나 루틴 하나를 골라 이번 주 달성한 날을 보여줘요. 연속으로 달성한 날은 이어서 표시돼요."),
    GuideItem("일일 달성량", "4×2", "이번 주 날짜별 전체 달성률을 원형 링으로 보여줘요."),
    GuideItem("주간 타임라인", "4×2", "루틴별로 이번 주 요일마다 달성 여부를 한눈에 봐요."),
)

@Composable
fun WidgetGuideScreen(onBack: () -> Unit) {
    val c = AppTheme.colors
    SettingsScaffold("위젯 가이드", onBack) {
        Text(
            "홈 화면 빈 곳을 길게 누르기 → 위젯 → 마이루틴에서 원하는 위젯을 끌어다 놓으세요.\n아래 미리보기는 지금 내 기록으로 그린 모습이에요.",
            color = c.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        GUIDE.forEachIndexed { i, g ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(16.dp)) {
                Text("${g.title}  ·  ${g.size}", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(g.desc, color = c.textSecondary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                val small = i < 3
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WidgetPreview(i, if (small) DpSize(170.dp, 170.dp) else DpSize(320.dp, 160.dp))
                }
            }
        }
    }
}

/** 실제 위젯 Content 를 RemoteViews 로 만들어 그대로 보여준다. */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun WidgetPreview(index: Int, size: DpSize) {
    val context = LocalContext.current
    var rv by remember { mutableStateOf<RemoteViews?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        runCatching {
            @Suppress("UNCHECKED_CAST")
            val w = WidgetUpdater.all[index] as DataWidget<Any?>
            val data = w.load(context, null)
            GlanceRemoteViews().compose(context, size, state = emptyPreferences()) { w.Content(context, data) }.remoteViews
        }.onSuccess { rv = it }.onFailure { failed = true }
    }
    val c = AppTheme.colors
    Box(Modifier.width(size.width).height(size.height)) {
        val views = rv
        when {
            views != null -> AndroidView(
                factory = { ctx -> FrameLayout(ctx) },
                update = { frame -> frame.removeAllViews(); runCatching { frame.addView(views.apply(frame.context, frame)) } },
                modifier = Modifier.width(size.width).height(size.height),
            )
            failed -> Text("미리보기를 그릴 수 없어요", color = c.textTertiary, modifier = Modifier.align(Alignment.Center))
        }
    }
}
