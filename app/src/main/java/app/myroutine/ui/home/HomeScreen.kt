package app.myroutine.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewHeadline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myroutine.data.settings.GroupMode
import app.myroutine.data.settings.ViewMode
import app.myroutine.domain.model.Todo
import app.myroutine.ui.components.PillChip
import app.myroutine.ui.components.SwipeAction
import app.myroutine.ui.components.SwipeActionBox
import app.myroutine.ui.theme.AppTheme
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

private fun itemKey(groupKey: String, id: String) = "r|$groupKey|$id"
private fun idFromKey(key: Any?): String? = (key as? String)?.takeIf { it.startsWith("r|") }?.substringAfterLast('|')

@Composable
fun HomeScreen(
    onAddRoutine: () -> Unit,
    onEditRoutine: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = AppTheme.colors
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var numericItem by remember { mutableStateOf<HomeItem?>(null) }
    var deleteItem by remember { mutableStateOf<HomeItem?>(null) }
    var showCalendar by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday() }

    LaunchedEffect(Unit) {
        vm.eventFlow.collect { e ->
            when (e) {
                is HomeEvent.AskNumeric -> numericItem = e.item
                is HomeEvent.Snackbar -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val r = snackbar.showSnackbar(e.message, actionLabel = if (e.undo != null) "되돌리기" else null, withDismissAction = false)
                    if (r == SnackbarResult.ActionPerformed) e.undo?.let { vm.runUndo(it) }
                }
            }
        }
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val f = idFromKey(from.key); val t = idFromKey(to.key)
        if (f != null && t != null && f != t) vm.movePending(f, t)
    }

    val onAction: (HomeItem, ItemAction) -> Unit = { item, a ->
        when (a) {
            ItemAction.EDIT -> onEditRoutine(item.id)
            ItemAction.MINUS -> vm.minusOne(item)
            ItemAction.RESET -> vm.resetLog(item)
            ItemAction.ARCHIVE -> vm.archive(item)
            ItemAction.DELETE -> deleteItem = item
        }
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item(key = "top") {
                HomeTopBar(
                    state = state,
                    onMonthClick = { showCalendar = true },
                    onCycleView = vm::cycleViewMode,
                    onGroupMode = vm::setGroupMode,
                )
            }
            item(key = "week") {
                WeekStrip(state.today, state.selectedDate, state.weekStart, state.dayRatios, vm::selectDate)
                Spacer(Modifier.height(16.dp))
            }
            item(key = "todos") {
                TodoSection(
                    todos = state.todos,
                    expanded = state.todosExpanded,
                    isToday = state.selectedDate == state.today,
                    onToggleExpand = { vm.setTodosExpanded(!state.todosExpanded) },
                    onToggle = vm::toggleTodo,
                    onAdd = vm::addTodo,
                    onUpdate = vm::updateTodo,
                    onDelete = vm::deleteTodo,
                )
                Spacer(Modifier.height(16.dp))
            }
            if (!state.loading && !state.hasAnyRoutine) {
                item(key = "empty") { EmptyHome(onAddRoutine) }
            } else {
                item(key = "filters") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.filterChips, key = { it.label }) { chip ->
                            PillChip("${chip.label} ${chip.count}", selected = state.filter == chip.filter, filledStyle = true) { vm.setFilter(chip.filter) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (state.groups.isEmpty() && !state.loading) {
                    item(key = "nothing") {
                        Text(
                            if (state.filter == HomeFilter.All) "이 날은 해당하는 루틴이 없어요" else "조건에 맞는 루틴이 없어요",
                            color = c.textTertiary, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        )
                    }
                }
                state.groups.forEach { group ->
                    routineGroup(group, state.viewMode, reorderState, vm, onAction, onDragStopped = vm::commitOrder)
                }
                item(key = "tip") {
                    Text(
                        "💡 루틴을 꾹 눌러 순서를 바꾸고, 왼쪽으로 밀어 완료 · 오른쪽으로 밀어 취소해요.",
                        color = c.textTertiary, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                    )
                }
            }
        }

        // FAB
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(64.dp)
                .shadow(8.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(c.textPrimary)
                .clickable(onClick = onAddRoutine),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "루틴 추가", tint = c.background, modifier = Modifier.size(34.dp)) }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp, start = 12.dp, end = 12.dp)) { data ->
            Snackbar(data, containerColor = c.surfaceHigh, contentColor = c.textPrimary, actionColor = c.accent)
        }
    }

    if (showCalendar) {
        MonthCalendarSheet(state.selectedDate, state.today, state.weekStart, onDismiss = { showCalendar = false }) {
            vm.selectDate(it); showCalendar = false
        }
    }
    numericItem?.let { item ->
        NumericInputDialog(item, onDismiss = { numericItem = null }) { v -> vm.setNumeric(item, v); numericItem = null }
    }
    deleteItem?.let { item ->
        ConfirmDialog(
            title = "루틴 삭제",
            message = "'${item.routine.name}' 루틴과 기록을 삭제할까요?\n기록을 남기고 싶다면 '보관'을 사용하세요.",
            confirm = "삭제",
            onDismiss = { deleteItem = null },
            onConfirm = { vm.delete(item); deleteItem = null },
        )
    }
}

private fun LazyListScope.routineGroup(
    group: HomeGroup,
    mode: ViewMode,
    reorderState: ReorderableLazyListState,
    vm: HomeViewModel,
    onAction: (HomeItem, ItemAction) -> Unit,
    onDragStopped: () -> Unit,
) {
    val isCheck = mode == ViewMode.CHECK
    item(key = "h|${group.key}") { GroupHeader(group, inCard = isCheck) }

    if (mode == ViewMode.GRID) {
        // 2열 그리드: 드래그 정렬은 지원하지 않음 (CLAUDE.md 결정 사항)
        group.items.chunked(2).forEachIndexed { rowIdx, row ->
            item(key = "g|${group.key}|$rowIdx|${row.joinToString { it.id }}") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { it ->
                        SwipeActionBox(
                            modifier = Modifier.weight(1f),
                            leftAction = completeAction { vm.swipeComplete(it) },
                            rightAction = cancelAction { vm.swipeCancel(it) },
                        ) { RoutineGridCell(it, Modifier.fillMaxWidth(), onTap = { vm.tap(it) }, onLongPress = null, onAction = { a -> onAction(it, a) }) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        return
    }

    group.items.forEachIndexed { index, it ->
        val key = itemKey(group.key, it.id)
        item(key = key) {
            ReorderableItem(reorderState, key = key) { dragging ->
                val c = AppTheme.colors
                val last = index == group.items.lastIndex
                val outer = if (isCheck) {
                    Modifier.padding(horizontal = 16.dp)
                        .clip(if (last) RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) else RoundedCornerShape(0.dp))
                        .background(c.surface)
                        .padding(horizontal = 12.dp)
                        .padding(bottom = if (last) 8.dp else 0.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                }
                Column(outer.then(if (dragging) Modifier.shadow(12.dp, RoundedCornerShape(20.dp)) else Modifier)) {
                    SwipeActionBox(
                        leftAction = completeAction { vm.swipeComplete(it) },
                        rightAction = cancelAction { vm.swipeCancel(it) },
                        shape = if (isCheck) RoundedCornerShape(12.dp) else RoundedCornerShape(20.dp),
                    ) {
                        val handle = Modifier.longPressDraggableHandle(onDragStopped = { onDragStopped() })
                        when (mode) {
                            ViewMode.CARD -> RoutineCard(it, handle, onTap = { vm.tap(it) }, onLongPress = null, onAction = { a -> onAction(it, a) })
                            ViewMode.COMPACT -> RoutineCompactRow(it, handle, onTap = { vm.tap(it) }, onLongPress = null, onAction = { a -> onAction(it, a) })
                            else -> RoutineCheckRow(it, handle, onTap = { vm.tap(it) }, onLongPress = null, onAction = { a -> onAction(it, a) })
                        }
                    }
                    if (isCheck && !last) HorizontalDivider(color = c.outline.copy(alpha = 0.6f), modifier = Modifier.padding(start = 52.dp))
                }
            }
        }
    }
}

@Composable
private fun completeAction(onTrigger: () -> Unit): SwipeAction {
    val c = AppTheme.colors
    return SwipeAction(Icons.Rounded.CheckCircle, "완료", c.accent, c.onAccent, onTrigger)
}

@Composable
private fun cancelAction(onTrigger: () -> Unit): SwipeAction {
    val c = AppTheme.colors
    return SwipeAction(Icons.AutoMirrored.Rounded.Undo, "취소", c.danger, Color.White, onTrigger)
}

@Composable
private fun GroupHeader(group: HomeGroup, inCard: Boolean) {
    val c = AppTheme.colors
    val base = if (inCard) {
        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(c.surface)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
    } else {
        Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
    }
    Row(base.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(group.emoji, fontSize = 20.sp)
        Spacer(Modifier.width(6.dp))
        Text(group.title, color = c.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text("${group.done}/${group.items.size}", color = c.textTertiary, fontSize = 17.sp)
    }
}

@Composable
private fun HomeTopBar(
    state: HomeUiState,
    onMonthClick: () -> Unit,
    onCycleView: () -> Unit,
    onGroupMode: (GroupMode) -> Unit,
) {
    val c = AppTheme.colors
    var groupMenu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onMonthClick).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${state.selectedDate.year}년 ${state.selectedDate.monthValue}월", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.KeyboardArrowDown, "달력", tint = c.textPrimary)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onCycleView) {
            val icon = when (state.viewMode) {
                ViewMode.CARD -> Icons.Rounded.ViewAgenda
                ViewMode.GRID -> Icons.Rounded.GridView
                ViewMode.COMPACT -> Icons.Rounded.ViewHeadline
                ViewMode.CHECK -> Icons.Rounded.Checklist
            }
            Icon(icon, "보기 모드 전환", tint = c.textPrimary, modifier = Modifier.size(28.dp))
        }
        Box {
            IconButton(onClick = { groupMenu = true }) { Icon(Icons.Rounded.Category, "그룹 설정", tint = c.textPrimary, modifier = Modifier.size(28.dp)) }
            DropdownMenu(expanded = groupMenu, onDismissRequest = { groupMenu = false }, containerColor = c.surfaceHigh) {
                listOf(
                    Triple(GroupMode.DEFAULT, "그룹: 기본", "오늘의 루틴, 중장기 루틴별로 모아보세요."),
                    Triple(GroupMode.TAG, "그룹: 태그", "☀️아침, 💪운동 등 태그별로 루틴을 모아보세요."),
                    Triple(GroupMode.MERGED, "그룹: 통합", "모든 루틴을 분리 없이 한눈에 모아볼 수 있어요."),
                ).forEach { (mode, title, desc) ->
                    DropdownMenuItem(
                        leadingIcon = { if (state.groupMode == mode) Icon(Icons.Rounded.Check, null, tint = c.accent) else Spacer(Modifier.size(24.dp)) },
                        text = {
                            Column(Modifier.width(240.dp).padding(vertical = 6.dp)) {
                                Text(title, color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(desc, color = c.textSecondary, fontSize = 13.sp)
                            }
                        },
                        onClick = { onGroupMode(mode); groupMenu = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoSection(
    todos: List<Todo>,
    expanded: Boolean,
    isToday: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (Todo) -> Unit,
    onAdd: (String, java.time.LocalTime?) -> Unit,
    onUpdate: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
) {
    val c = AppTheme.colors
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Todo?>(null) }
    Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(c.surface)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggleExpand).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✅", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(if (isToday) "오늘의 할 일" else "할 일", color = c.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("${todos.count { it.done }}/${todos.size}", color = c.textTertiary, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = c.textPrimary)
        }
        if (expanded) {
            todos.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().clickable { editing = t }.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).clickable { onToggle(t) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (t.done) c.accent else c.surfaceHigh),
                            contentAlignment = Alignment.Center,
                        ) { if (t.done) Icon(Icons.Rounded.Check, null, tint = c.onAccent, modifier = Modifier.size(16.dp)) }
                    }
                    Text(
                        t.title, color = if (t.done) c.textTertiary else c.textPrimary, fontSize = 16.sp,
                        textDecoration = if (t.done) TextDecoration.LineThrough else null, modifier = Modifier.weight(1f).padding(start = 6.dp),
                    )
                    t.time?.let { TimeChip("%02d:%02d".format(it.hour, it.minute)) }
                    IconButton(onClick = { onDelete(t) }) { Icon(Icons.Rounded.Close, "삭제", tint = c.textTertiary, modifier = Modifier.size(18.dp)) }
                }
            }
            Text(
                "+ 할 일 추가", color = c.accent, fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth().clickable { adding = true }.padding(horizontal = 24.dp, vertical = 14.dp),
            )
        }
    }
    if (adding) TodoEditDialog(null, onDismiss = { adding = false }, onSave = { title, time -> onAdd(title, time); adding = false })
    editing?.let { t ->
        TodoEditDialog(
            t, onDismiss = { editing = null },
            onSave = { title, time -> onUpdate(t.copy(title = title, time = time)); editing = null },
            onDelete = { onDelete(t); editing = null },
        )
    }
}

@Composable
private fun EmptyHome(onAdd: () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🌱", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("아직 루틴이 없어요", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("작은 습관 하나부터 시작해 보세요.\n추천루틴 탭에서 골라도 좋아요.", color = c.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        PillChip("+ 루틴 만들기", selected = true, onClick = onAdd)
    }
}
