package app.myroutine.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myroutine.ui.theme.AppTheme
import app.myroutine.ui.theme.RoutinePalettes

/** "색상 테마 선택"(4개씩 페이지) + "루틴 색상 선택"(5색) 바텀시트. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(currentKey: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val c = AppTheme.colors
    val (initTheme, initIdx) = RoutinePalettes.parse(currentKey)
    val themes = RoutinePalettes.themes
    var themeId by remember { mutableStateOf(initTheme.id) }
    var index by remember { mutableIntStateOf(initIdx) }
    val pages = themes.chunked(4)
    val pager = rememberPagerState(initialPage = themes.indexOfFirst { it.id == initTheme.id }.coerceAtLeast(0) / 4) { pages.size }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val theme = themes.first { it.id == themeId }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.surface) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("색상 테마 선택", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            HorizontalPager(pager) { page ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pages[page].forEach { t ->
                        Column(
                            Modifier.weight(1f).clickable { themeId = t.id },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val sel = t.id == themeId
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.55f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(if (sel) 3.dp else 1.dp, if (sel) c.textPrimary else c.outline, RoundedCornerShape(14.dp))
                                    .padding(if (sel) 5.dp else 3.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val h = size.height / t.colors.size
                                    t.colors.forEachIndexed { i, col ->
                                        drawRect(Color(col), Offset(0f, h * i), Size(size.width, h + 1))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(t.name, color = if (sel) c.textPrimary else c.textSecondary, fontSize = 13.sp)
                        }
                    }
                    repeat(4 - pages[page].size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier.padding(4.dp).size(8.dp).clip(CircleShape)
                            .background(if (pager.currentPage == i) c.textPrimary else c.outline),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("루틴 색상 선택", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                theme.colors.forEachIndexed { i, col ->
                    ColorDot(Color(col), 56.dp, Modifier.clickable { index = i }, selected = i == index)
                }
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { onConfirm(RoutinePalettes.key(themeId, index.coerceIn(0, theme.colors.lastIndex))) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = c.textPrimary, contentColor = c.background),
            ) { Text("확인", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

