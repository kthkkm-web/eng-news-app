package app.myroutine.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.myroutine.ui.theme.AppTheme

/** 사선 줄무늬가 들어간 루틴 색상 원. selected 이면 바깥 흰 테두리. */
@Composable
fun ColorDot(color: Color, size: Dp, modifier: Modifier = Modifier, selected: Boolean = false) {
    val ring = AppTheme.colors.textPrimary
    val gap = AppTheme.colors.background
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = center
        if (selected) {
            drawCircle(ring, r)
            drawCircle(gap, r - 3.dp.toPx())
        } else {
            drawCircle(Color.Black.copy(alpha = 0.25f), r)
        }
        val inner = r - (if (selected) 6.dp else 3.dp).toPx()
        val light = lerp(color, Color.White, 0.45f)
        drawCircle(Brush.linearGradient(listOf(light, color), start = Offset(c.x - inner, c.y - inner), end = Offset(c.x + inner, c.y + inner)), inner, c)
        val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, inner)) }
        clipPath(clip) {
            val step = inner * 0.55f
            var x = -inner * 2
            while (x < inner * 2) {
                drawLine(
                    Color.White.copy(alpha = 0.22f),
                    start = Offset(c.x + x, c.y + inner * 1.2f),
                    end = Offset(c.x + x + inner * 2.4f, c.y - inner * 1.2f),
                    strokeWidth = step * 0.45f,
                )
                x += step
            }
        }
    }
}

/** 원형 진행 링. progress null = 해당 없음(트랙 없이 비움). */
@Composable
fun ProgressRing(
    progress: Float?,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    stroke: Dp = 3.dp,
    track: Color = AppTheme.colors.surfaceHigh,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val d = this.size.minDimension - sw
            val tl = Offset(sw / 2, sw / 2)
            if (progress != null) {
                drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
                if (progress > 0f) {
                    drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
        }
        content()
    }
}

/** 원 안을 진행률만큼 부채꼴로 채우는 체크 버튼 (체크 리스트 보기 모드). */
@Composable
fun PieCheck(progress: Float, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val sw = 2.dp.toPx()
        val d = this.size.minDimension - sw
        val tl = Offset(sw / 2, sw / 2)
        drawArc(color, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
        if (progress >= 1f) {
            drawCircle(color, d / 2)
            val p = Path().apply {
                moveTo(this@Canvas.size.width * 0.3f, this@Canvas.size.height * 0.52f)
                lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.66f)
                lineTo(this@Canvas.size.width * 0.72f, this@Canvas.size.height * 0.36f)
            }
            drawPath(p, Color.Black.copy(alpha = 0.75f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        } else if (progress > 0f) {
            val inset = 4.dp.toPx()
            drawArc(color, -90f, 360f * progress, true, Offset(inset, inset), Size(this.size.width - inset * 2, this.size.height - inset * 2))
        }
    }
}
