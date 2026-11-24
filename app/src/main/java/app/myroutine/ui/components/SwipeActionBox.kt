package app.myroutine.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

data class SwipeAction(val icon: ImageVector, val label: String, val background: Color, val tint: Color, val onTrigger: () -> Unit)

/**
 * 좌우 스와이프 액션. 항목은 사라지지 않고, 임계값(폭의 28%)을 넘겨 놓으면 액션 실행 후 제자리로 돌아온다.
 * - 왼쪽으로 밀기 → [leftAction] (오른쪽 끝에 아이콘이 드러남)
 * - 오른쪽으로 밀기 → [rightAction]
 */
@Composable
fun SwipeActionBox(
    modifier: Modifier = Modifier,
    leftAction: SwipeAction?,
    rightAction: SwipeAction?,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var width by remember { mutableIntStateOf(1) }
    val threshold = width * 0.28f
    var armed by remember { mutableIntStateOf(0) } // -1 left, 1 right

    val dragState = rememberDraggableState { delta ->
        scope.launch {
            val min = if (leftAction != null) -width * 0.6f else 0f
            val max = if (rightAction != null) width * 0.6f else 0f
            val next = (offset.value + delta).coerceIn(min, max)
            offset.snapTo(next)
            val nowArmed = when { next <= -threshold -> -1; next >= threshold -> 1; else -> 0 }
            if (nowArmed != armed) {
                if (nowArmed != 0) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                armed = nowArmed
            }
        }
    }

    Box(modifier.onSizeChanged { width = it.width.coerceAtLeast(1) }) {
        val o = offset.value
        val action = if (o < 0) leftAction else if (o > 0) rightAction else null
        if (action != null && abs(o) > 1f) {
            val reached = abs(o) >= threshold
            Box(
                Modifier
                    .matchParentSize()
                    .background(if (reached) action.background else action.background.copy(alpha = 0.45f), shape)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (o < 0) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(action.icon, null, tint = action.tint)
                    Text(action.label, color = action.tint, fontSize = 14.sp)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offset.value }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    onDragStopped = {
                        val v = offset.value
                        when {
                            v <= -threshold -> leftAction?.onTrigger?.invoke()
                            v >= threshold -> rightAction?.onTrigger?.invoke()
                        }
                        armed = 0
                        offset.animateTo(0f, tween(220))
                    },
                ),
        ) { content() }
    }
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
