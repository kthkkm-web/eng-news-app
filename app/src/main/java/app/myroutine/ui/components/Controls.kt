package app.myroutine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myroutine.ui.theme.AppTheme

/** 둥근 알약 칩. selected = 강조색 채움, 아니면 테두리만. */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    filledStyle: Boolean = false,
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    val bg = when {
        selected && filledStyle -> c.textPrimary
        selected -> c.accent
        filledStyle -> c.surfaceHigh
        else -> Color.Transparent
    }
    val fg = when {
        selected && filledStyle -> c.background
        selected -> c.onAccent
        else -> c.textSecondary
    }
    Box(
        modifier
            .clip(CircleShape)
            .background(bg)
            .then(if (!selected && !filledStyle) Modifier.border(1.dp, c.outline, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = c.accent, checkedThumbColor = c.onAccent,
            uncheckedTrackColor = c.surfaceHigh, uncheckedThumbColor = c.textTertiary, uncheckedBorderColor = c.outline,
        ),
    )
}

@Composable
fun SectionDivider() {
    Spacer(Modifier.fillMaxWidth().height(8.dp).background(AppTheme.colors.background))
}

@Composable
fun RowItem(
    title: String,
    value: String? = null,
    valueColor: Color = AppTheme.colors.textSecondary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = c.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = valueColor, fontSize = 15.sp)
        if (onClick != null && trailing == null) Text("  ›", color = c.textTertiary, fontSize = 20.sp)
        trailing?.invoke()
    }
}

val CardShape = RoundedCornerShape(20.dp)

fun Modifier.card(color: Color) = this.clip(CardShape).background(color)
