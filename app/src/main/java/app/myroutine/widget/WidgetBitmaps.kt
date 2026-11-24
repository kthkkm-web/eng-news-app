package app.myroutine.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import app.myroutine.domain.schedule.RuleFormatter
import java.time.LocalDate

/** Glance 가 Canvas 를 지원하지 않으므로 원형/알약 모양은 비트맵으로 그린다. */
object WidgetBitmaps {

    data class Palette(val text: Int, val sub: Int, val track: Int, val accent: Int, val todayRing: Int)

    /**
     * 달성 흐름: 일~토 날짜 원. 연속 달성일은 알약 모양으로 이어 붙인다. 오늘은 테두리 강조.
     */
    fun streakRow(w: Int, h: Int, weekStart: LocalDate, today: LocalDate, achieved: List<Boolean?>, color: Int, p: Palette, density: Float): Bitmap {
        val bmp = createBitmap(w.coerceAtLeast(10), h.coerceAtLeast(10))
        val c = Canvas(bmp)
        val slot = w / 7f
        val labelH = 14 * density
        val r = minOf(slot * 0.42f, (h - labelH) * 0.42f)
        val cy = labelH + (h - labelH) / 2f
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f * density; textAlign = Paint.Align.CENTER }
        val num = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12 * density; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        // 연속 구간 알약
        var i = 0
        while (i < 7) {
            if (achieved[i] == true) {
                var j = i
                while (j + 1 < 7 && achieved[j + 1] == true) j++
                fill.color = color
                c.drawRoundRect(RectF(slot * i + slot / 2 - r, cy - r, slot * j + slot / 2 + r, cy + r), r, r, fill)
                i = j + 1
            } else i++
        }
        for (k in 0 until 7) {
            val d = weekStart.plusDays(k.toLong())
            val cx = slot * k + slot / 2
            label.color = if (d == today) color else p.sub
            c.drawText(RuleFormatter.dayName(d.dayOfWeek), cx, labelH - 3 * density, label)
            val a = achieved[k]
            if (a != true) {
                fill.color = if (d.isAfter(today)) ColorUtils.setAlphaComponent(p.track, 120) else p.track
                c.drawCircle(cx, cy, r, fill)
            }
            if (d == today) {
                c.drawCircle(cx, cy, r - 1.2f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.4f * density; this.color = p.todayRing })
            }
            num.color = if (a == true) (if (ColorUtils.calculateLuminance(color) > 0.55) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()) else if (d.isAfter(today)) p.sub else p.text
            c.drawText("${d.dayOfMonth}", cx, cy + 4.3f * density, num)
        }
        return bmp
    }

    /** 일일 달성량: 요일 라벨 + 날짜 숫자 위 원형 링. 오늘 요일은 강조색. */
    fun ringRow(w: Int, h: Int, weekStart: LocalDate, today: LocalDate, ratios: List<Float?>, p: Palette, density: Float): Bitmap {
        val bmp = createBitmap(w.coerceAtLeast(10), h.coerceAtLeast(10))
        val c = Canvas(bmp)
        val slot = w / 7f
        val labelH = 15 * density
        val r = minOf(slot * 0.38f, (h - labelH) * 0.4f)
        val cy = labelH + (h - labelH) / 2f
        val sw = 3.2f * density
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11 * density; textAlign = Paint.Align.CENTER }
        val num = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11.5f * density; textAlign = Paint.Align.CENTER }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = sw; strokeCap = Paint.Cap.ROUND }
        for (k in 0 until 7) {
            val d = weekStart.plusDays(k.toLong())
            val cx = slot * k + slot / 2
            label.color = if (d == today) p.accent else p.sub
            label.typeface = if (d == today) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            c.drawText(RuleFormatter.dayName(d.dayOfWeek), cx, labelH - 3 * density, label)
            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            val v = ratios[k]
            if (v != null) {
                ring.color = p.track; c.drawArc(rect, 0f, 360f, false, ring)
                if (v > 0f) { ring.color = p.accent; c.drawArc(rect, -90f, 360f * v.coerceIn(0f, 1f), false, ring) }
            } else if (!d.isAfter(today)) {
                ring.color = ColorUtils.setAlphaComponent(p.track, 90); c.drawArc(rect, 0f, 360f, false, ring)
            }
            num.color = if (d.isAfter(today)) p.sub else p.text
            num.typeface = if (d == today) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            c.drawText("${d.dayOfMonth}", cx, cy + 4f * density, num)
        }
        return bmp
    }
}
