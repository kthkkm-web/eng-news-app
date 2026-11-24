package app.myroutine.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.myroutine.MainActivity

object WidgetKeys {
    /** 갱신 버전: 바뀌면 위젯이 데이터를 다시 읽는다. */
    val VERSION = longPreferencesKey("version")
    /** 위젯 설정: 태그 ID 또는 루틴 ID. */
    val TARGET = stringPreferencesKey("target")

    val ROUTINE_ID = ActionParameters.Key<String>("routineId")
    val TODO_ID = ActionParameters.Key<String>("todoId")
}

fun Preferences.target(): String? = this[WidgetKeys.TARGET]?.takeIf { it.isNotBlank() }

/** 다크/라이트 자동 대응 색. */
object WColors {
    val bg = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1B1E22))
    val cell = ColorProvider(day = Color(0xFFEBEDF0), night = Color(0xFF262A2F))
    val text = ColorProvider(day = Color(0xFF15181B), night = Color(0xFFF2F4F5))
    val sub = ColorProvider(day = Color(0xFF5C636B), night = Color(0xFFA3A9B0))
    val tertiary = ColorProvider(day = Color(0xFF9AA0A7), night = Color(0xFF6B7178))
    val accent = ColorProvider(day = Color(0xFF13B57A), night = Color(0xFF3DDC97))
    fun fixed(argb: Long) = ColorProvider(Color(argb))
    fun fixed(c: Color) = ColorProvider(c)

    fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun palette(context: Context): WidgetBitmaps.Palette = if (isNight(context)) {
        WidgetBitmaps.Palette(text = 0xFFF2F4F5.toInt(), sub = 0xFFA3A9B0.toInt(), track = 0xFF2E3339.toInt(), accent = 0xFF3DDC97.toInt(), todayRing = 0xFFF2F4F5.toInt())
    } else {
        WidgetBitmaps.Palette(text = 0xFF15181B.toInt(), sub = 0xFF5C636B.toInt(), track = 0xFFE3E6EA.toInt(), accent = 0xFF13B57A.toInt(), todayRing = 0xFF15181B.toInt())
    }

    /** 히트맵 칸 색(다크/라이트 고정값 계산용). */
    fun heatArgb(context: Context, ratio: Float?, color: Long): Long {
        val night = isNight(context)
        val empty = if (night) 0xFF262A2F else 0xFFE6E8EB
        if (ratio == null) return if (night) 0xFF1F2327 else 0xFFF0F1F3
        if (ratio <= 0f) return empty
        val t = 0.35f + 0.65f * ratio.coerceIn(0f, 1f)
        fun ch(a: Long, b: Long, s: Int) = (((a shr s) and 0xFF) + ((((b shr s) and 0xFF) - ((a shr s) and 0xFF)) * t)).toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (ch(empty, color, 16) shl 16) or (ch(empty, color, 8) shl 8) or ch(empty, color, 0)
    }
}

object DeepLink {
    const val EXTRA_ROUTE = "app.myroutine.ROUTE"
    fun intent(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

fun openApp(context: Context, route: String): Action = actionStartActivity(DeepLink.intent(context, route))

@Composable
fun WidgetFrame(onClick: Action?, padding: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    var m = GlanceModifier.fillMaxSize().appWidgetBackground().background(WColors.bg).cornerRadius(22.dp)
    if (onClick != null) m = m.clickable(onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun WText(text: String, size: TextUnit = 13.sp, color: ColorProvider = WColors.text, bold: Boolean = false, modifier: GlanceModifier = GlanceModifier, maxLines: Int = 1) {
    Text(text, modifier = modifier, maxLines = maxLines, style = TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal))
}

@Composable
fun ColorBar(argb: Long, h: Dp = 14.dp) {
    Box(GlanceModifier.width(3.dp).height(h).cornerRadius(2.dp).background(WColors.fixed(argb))) {}
}
