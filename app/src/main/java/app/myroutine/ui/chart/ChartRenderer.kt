package app.myroutine.ui.chart

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import java.time.LocalDate
import kotlin.math.max

/** 그래프 색 설정 (ARGB Int). */
data class ChartStyle(
    val line: Int,
    val text: Int,
    val subText: Int,
    val grid: Int,
    val background: Int = 0,
    val density: Float = 1f,
)

/**
 * android.graphics 로 그리는 그래프. 앱 화면(Image)과 Glance 위젯(Bitmap) 양쪽에서 쓴다.
 */
object ChartRenderer {

    private fun fmt(v: Double, decimals: Int) = "%.${decimals.coerceIn(0, 3)}f".format(v)
    private fun md(d: LocalDate) = "${d.monthValue}.${d.dayOfMonth}"

    /** 라인 그래프 + 아래 그라데이션 + 최고/최저 마커 + X축 날짜. */
    fun lineChart(widthPx: Int, heightPx: Int, points: List<Pair<LocalDate, Double>>, style: ChartStyle, decimals: Int = 1): Bitmap {
        val w = max(widthPx, 10); val h = max(heightPx, 10)
        val bmp = createBitmap(w, h)
        val c = Canvas(bmp)
        if (style.background != 0) c.drawColor(style.background)
        val dp = style.density
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.subText; textSize = 10 * dp }
        if (points.isEmpty()) {
            text.textAlign = Paint.Align.CENTER; text.textSize = 12 * dp
            c.drawText("기록이 없어요", w / 2f, h / 2f, text)
            return bmp
        }
        val left = 10 * dp; val right = w - 10 * dp
        val top = 24 * dp; val bottom = h - 18 * dp
        val values = points.map { it.second }
        val minV = values.min(); val maxV = values.max()
        val span = (maxV - minV).takeIf { it > 1e-9 } ?: 1.0
        fun x(i: Int) = if (points.size == 1) (left + right) / 2 else left + (right - left) * i / (points.size - 1)
        fun y(v: Double) = (bottom - (bottom - top) * ((v - minV) / span)).toFloat()

        // 바닥선
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.grid; strokeWidth = 1 * dp }
        c.drawLine(left, bottom, right, bottom, grid)

        val path = Path(); val fill = Path()
        points.forEachIndexed { i, (_, v) ->
            if (i == 0) { path.moveTo(x(i), y(v)); fill.moveTo(x(i), bottom); fill.lineTo(x(i), y(v)) }
            else { path.lineTo(x(i), y(v)); fill.lineTo(x(i), y(v)) }
        }
        fill.lineTo(x(points.lastIndex), bottom); fill.close()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, top, 0f, bottom, ColorUtils.setAlphaComponent(style.line, 110), ColorUtils.setAlphaComponent(style.line, 0), Shader.TileMode.CLAMP)
        }
        c.drawPath(fill, fillPaint)
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.line; strokeWidth = 2.2f * dp; style(Paint.Style.STROKE); strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
        })

        // 최고/최저 마커
        val iMax = values.indexOf(maxV); val iMin = values.lastIndexOf(minV)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.line }
        listOf(iMax to "최고", iMin to "최저").distinctBy { it.first }.forEach { (i, label) ->
            val px = x(i); val py = y(values[i])
            c.drawCircle(px, py, 3.5f * dp, dot)
            badge(c, px, py - 8 * dp, label, fmt(values[i], decimals), style, w.toFloat())
        }

        // X축: 처음, 중간, 끝 날짜
        text.color = style.subText
        val idx = listOf(0, points.size / 2, points.lastIndex).distinct()
        idx.forEach { i ->
            text.textAlign = when (i) { 0 -> Paint.Align.LEFT; points.lastIndex -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
            c.drawText(md(points[i].first), x(i), h - 4 * dp, text)
        }
        return bmp
    }

    private fun badge(c: Canvas, cx: Float, bottomY: Float, label: String, value: String, style: ChartStyle, maxW: Float) {
        val dp = style.density
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f * dp; typeface = Typeface.DEFAULT_BOLD }
        val s = "$label $value"
        val tw = p.measureText(s) + 10 * dp
        var l = cx - tw / 2
        l = l.coerceIn(2 * dp, maxW - tw - 2 * dp)
        val rect = RectF(l, bottomY - 15 * dp, l + tw, bottomY)
        c.drawRoundRect(rect, 7 * dp, 7 * dp, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.line })
        p.color = if (ColorUtils.calculateLuminance(style.line) > 0.5) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
        p.textAlign = Paint.Align.CENTER
        c.drawText(s, rect.centerX(), rect.bottom - 4.5f * dp, p)
    }

    /** 막대 그래프 + 막대 위 값 라벨 + X축 날짜. 마지막 막대(최근)는 진하게. */
    fun barChart(widthPx: Int, heightPx: Int, points: List<Pair<LocalDate, Double>>, style: ChartStyle, decimals: Int = 1): Bitmap {
        val w = max(widthPx, 10); val h = max(heightPx, 10)
        val bmp = createBitmap(w, h)
        val c = Canvas(bmp)
        if (style.background != 0) c.drawColor(style.background)
        val dp = style.density
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.subText; textSize = 9.5f * dp; textAlign = Paint.Align.CENTER }
        if (points.isEmpty()) {
            text.textSize = 12 * dp
            c.drawText("기록이 없어요", w / 2f, h / 2f, text)
            return bmp
        }
        val left = 6 * dp; val right = w - 6 * dp
        val top = 16 * dp; val bottom = h - 16 * dp
        val values = points.map { it.second }
        val maxV = values.max(); val minV = values.min()
        // 값 차이가 보이도록 바닥을 최소값보다 약간 아래로
        val floor = if (minV > 0) minV - (maxV - minV).coerceAtLeast(maxV * 0.05) * 1.2 else 0.0
        val span = (maxV - floor).takeIf { it > 1e-9 } ?: 1.0
        val slot = (right - left) / points.size
        val bw = slot * 0.62f
        val bar = Paint(Paint.ANTI_ALIAS_FLAG)
        points.forEachIndexed { i, (d, v) ->
            val cx = left + slot * i + slot / 2
            val by = (bottom - (bottom - top) * ((v - floor) / span)).toFloat()
            bar.color = if (i == points.lastIndex) style.line else ColorUtils.setAlphaComponent(style.line, 190)
            c.drawRoundRect(RectF(cx - bw / 2, by, cx + bw / 2, bottom), 3 * dp, 3 * dp, bar)
            text.color = style.text
            c.drawText(fmt(v, decimals), cx, by - 3 * dp, text)
            text.color = style.subText
            c.drawText(md(d), cx, h - 3 * dp, text)
        }
        return bmp
    }
}

private fun Paint.style(s: Paint.Style) { this.style = s }
