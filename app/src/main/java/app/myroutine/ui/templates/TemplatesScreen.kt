package app.myroutine.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.myroutine.domain.repository.RoutineRepository
import app.myroutine.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel @Inject constructor(templates: TemplateRepository, routines: RoutineRepository) : ViewModel() {
    val categories: List<TemplateCategory> = templates.categories
    /** 이미 추가한 템플릿 key (삭제되지 않은 루틴 기준). */
    val added: StateFlow<Set<String>> = routines.observeRoutines()
        .map { list -> list.mapNotNull { it.templateKey }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
}

@Composable
fun TemplatesScreen(onPick: (String) -> Unit, onCustom: () -> Unit, vm: TemplatesViewModel = hiltViewModel()) {
    val c = AppTheme.colors
    val added by vm.added.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item {
                Text(
                    "추천 루틴", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
                )
            }
            items(vm.categories, key = { it.key }) { cat ->
                Column(Modifier.padding(vertical = 10.dp)) {
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(cat.title, color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("  ${cat.items.size}", color = c.textTertiary, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    // 2줄 가로 스크롤
                    Column(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val (row1, row2) = cat.items.withIndex().partition { it.index % 2 == 0 }
                        listOf(row1, row2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (_, t) -> TemplateChip(t, t.key in added) { onPick(t.key) } }
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                .shadow(8.dp, CircleShape).clip(CircleShape).background(c.textPrimary)
                .clickable(onClick = onCustom).padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { Text("+ 나만의 루틴 만들기", color = c.background, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun TemplateChip(t: RoutineTemplate, added: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .border(1.dp, if (added) c.accent else c.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${t.emoji}${t.label}", color = c.textPrimary, fontSize = 14.sp)
        if (added) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.Check, "추가됨", tint = c.accent, modifier = Modifier.size(16.dp))
        }
    }
}
