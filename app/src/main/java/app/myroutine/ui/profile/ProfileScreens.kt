package app.myroutine.ui.profile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.myroutine.BuildConfig
import app.myroutine.data.settings.ThemeMode
import app.myroutine.data.settings.ViewMode
import app.myroutine.notification.Notifications
import app.myroutine.ui.components.AppSwitch
import app.myroutine.ui.components.RowItem
import app.myroutine.ui.edit.ChoiceDialog
import app.myroutine.ui.home.ConfirmDialog
import app.myroutine.ui.theme.AppTheme
import app.myroutine.ui.theme.RoutinePalettes
import java.time.DayOfWeek
import java.time.LocalDate

enum class SettingsSection(val route: String, val title: String) {
    NOTIFICATIONS("notifications", "알림 설정"),
    DISPLAY("display", "화면 설정"),
    TIME("time", "시간 및 요일 설정"),
    ROUTINE("routine", "루틴 및 할 일 설정"),
    BACKUP("backup", "데이터 백업/복원"),
    WIDGETS("widgets", "위젯 가이드"),
    ARCHIVED("archived", "보관한 루틴"),
}

@Composable
fun ProfileScreen(onOpen: (SettingsSection) -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val c = AppTheme.colors
    val user by vm.user.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState())) {
        Row(Modifier.statusBarsPadding().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(c.surfaceHigh), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, tint = c.textSecondary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(user?.displayName ?: "로컬 사용자", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("로컬 사용자 · 로그인(준비 중)", color = c.textTertiary, fontSize = 13.sp)
            }
        }
        Group {
            listOf(SettingsSection.NOTIFICATIONS, SettingsSection.DISPLAY, SettingsSection.TIME, SettingsSection.ROUTINE)
                .forEach { RowItem(it.title, onClick = { onOpen(it) }) }
        }
        Group {
            RowItem("데이터 백업/복원", value = "기기 변경 대비", onClick = { onOpen(SettingsSection.BACKUP) })
        }
        Group {
            RowItem("위젯 가이드", onClick = { onOpen(SettingsSection.WIDGETS) })
            RowItem("앱 버전", value = BuildConfig.VERSION_NAME)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AppTheme.colors.surface), content = content)
}

@Composable
fun SettingsScaffold(title: String, onBack: () -> Unit, snackbar: SnackbarHostState? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = AppTheme.colors
    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", tint = c.textPrimary) }
                Text(title, color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding(), content = content)
        }
        if (snackbar != null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp)) {
            Snackbar(it, containerColor = c.surfaceHigh, contentColor = c.textPrimary)
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = AppTheme.colors.textTertiary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
}

@Composable
fun SettingsSectionScreen(section: SettingsSection, onBack: () -> Unit, onManageTags: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val c = AppTheme.colors
    val st by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    var choice by remember { mutableStateOf<String?>(null) }

    when (section) {
        SettingsSection.NOTIFICATIONS -> {
            var tick by remember { mutableIntStateOf(0) }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ } // 설정에서 돌아오면 권한 상태 새로고침
            val canPost = remember(tick) { Notifications.canPost(context) }
            val canExact = remember(tick) { Notifications.canExact(context) }
            val battery = remember(tick) { Notifications.ignoringBatteryOptimizations(context) }
            val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
            SettingsScaffold(section.title, onBack) {
                Group {
                    RowItem("전체 알림", trailing = { AppSwitch(st.notificationsEnabled, vm::setNotifications) })
                }
                Group {
                    RowItem("알림 권한", value = if (canPost) "허용됨" else "꺼짐 · 허용하기", valueColor = if (canPost) c.accent else c.danger, onClick = {
                        if (!canPost && android.os.Build.VERSION.SDK_INT >= 33) permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        else context.startActivity(Notifications.appNotificationSettings(context))
                    })
                    RowItem("정확한 시각 알림 (알람 및 리마인더)", value = if (canExact) "허용됨" else "꺼짐", valueColor = if (canExact) c.accent else c.danger, onClick = {
                        runCatching { context.startActivity(Notifications.exactAlarmSettings(context)) }
                    })
                    RowItem("배터리 최적화 예외", value = if (battery) "적용됨" else "미적용", valueColor = if (battery) c.accent else c.danger, onClick = {
                        runCatching { context.startActivity(Notifications.batteryOptimizationRequest(context)) }
                    })
                }
                Hint(
                    "삼성 갤럭시(One UI)에서 알림·위젯이 누락된다면:\n" +
                        "• 설정 > 배터리 > 백그라운드 사용 한도 > '사용하지 않는 앱을 절전 상태로 전환' 끄기\n" +
                        "• 같은 화면의 '절전 예외 앱'에 마이루틴 추가\n" +
                        "• 설정 > 애플리케이션 > 마이루틴 > 배터리 > '제한 없음' 선택",
                )
            }
        }
        SettingsSection.DISPLAY -> SettingsScaffold(section.title, onBack) {
            Group {
                RowItem("기본 보기 모드", value = viewModeLabel(st.viewMode), onClick = { choice = "view" })
                RowItem("테마", value = when (st.themeMode) { ThemeMode.DARK -> "다크"; ThemeMode.LIGHT -> "라이트"; ThemeMode.SYSTEM -> "시스템 설정" }, onClick = { choice = "theme" })
            }
            if (choice == "view") ChoiceDialog("기본 보기 모드", ViewMode.entries.map { it to viewModeLabel(it) }, st.viewMode, { choice = null }) { vm.setViewMode(it); choice = null }
            if (choice == "theme") ChoiceDialog("테마", listOf(ThemeMode.DARK to "다크", ThemeMode.LIGHT to "라이트", ThemeMode.SYSTEM to "시스템 설정"), st.themeMode, { choice = null }) { vm.setTheme(it); choice = null }
        }
        SettingsSection.TIME -> SettingsScaffold(section.title, onBack) {
            Group {
                RowItem("한 주 시작 요일", value = if (st.weekStart == DayOfWeek.SUNDAY) "일요일" else "월요일", onClick = { choice = "week" })
                RowItem("하루 시작 시각", value = if (st.dayStartHour == 0) "자정 (0시)" else "새벽 ${st.dayStartHour}시", onClick = { choice = "day" })
            }
            Hint("하루 시작 시각을 새벽 4시로 하면 새벽 3시 59분까지의 기록은 전날로 들어가요. 늦게 자는 분께 좋아요.")
            if (choice == "week") ChoiceDialog("한 주 시작 요일", listOf(DayOfWeek.SUNDAY to "일요일", DayOfWeek.MONDAY to "월요일"), st.weekStart, { choice = null }) { vm.setWeekStart(it); choice = null }
            if (choice == "day") ChoiceDialog("하루 시작 시각", (0..6).map { it to if (it == 0) "자정 (0시)" else "새벽 ${it}시" }, st.dayStartHour, { choice = null }) { vm.setDayStart(it); choice = null }
        }
        SettingsSection.ROUTINE -> SettingsScaffold(section.title, onBack) {
            Group {
                RowItem("완료한 루틴 아래로 내리기", trailing = { AppSwitch(st.completedToBottom, vm::setCompletedToBottom) })
                RowItem("태그 관리", onClick = onManageTags)
            }
            ArchivedList(vm)
        }
        SettingsSection.ARCHIVED -> SettingsScaffold(section.title, onBack) { ArchivedList(vm) }
        SettingsSection.BACKUP -> {
            val pending by vm.pendingRestore.collectAsStateWithLifecycle()
            val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::export) }
            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::pickRestore) }
            SettingsScaffold(section.title, onBack, snackbar) {
                Hint("로그인 없이 기기에만 저장되므로, 기기를 바꾸기 전에 백업 파일을 만들어 구글 드라이브·카카오톡 나에게 보내기 등으로 옮겨 두세요.")
                Group {
                    RowItem("백업 파일 만들기", value = "JSON 내보내기", onClick = { exportLauncher.launch("myroutine-backup-${LocalDate.now()}.json") })
                    RowItem("백업에서 복원", value = "가져오기", onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
                }
                Hint("복원하면 현재 기기의 루틴·기록·태그·할 일이 백업 내용으로 모두 바뀌어요. 설정값(보기 모드 등)은 바뀌지 않아요.")
            }
            pending?.let { f ->
                val sm = vm.summaryOf(f)
                ConfirmDialog(
                    title = "백업에서 복원",
                    message = "${java.time.Instant.ofEpochMilli(f.exportedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()} 백업\n루틴 ${sm.routines}개 · 기록 ${sm.logs}일 · 태그 ${sm.tags}개 · 할 일 ${sm.todos}개\n\n현재 데이터를 모두 이 백업으로 바꿀까요?",
                    confirm = "복원",
                    onDismiss = { vm.pendingRestore.value = null },
                    onConfirm = { vm.confirmRestore() },
                )
            }
        }
        SettingsSection.WIDGETS -> WidgetGuideScreen(onBack)
    }
}

@Composable
private fun ArchivedList(vm: ProfileViewModel) {
    val c = AppTheme.colors
    val archived by vm.archived.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<String?>(null) }
    Text("보관한 루틴 ${archived.size}", color = c.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp))
    if (archived.isEmpty()) Hint("보관한 루틴이 없어요. 홈에서 루틴의 ⋮ 메뉴 > 보관을 누르면 기록은 남기고 목록에서 숨겨요.")
    else Group {
        archived.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(RoutinePalettes.color(r.color)))
                Text("${r.emoji}${r.name}", color = c.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                TextButton(onClick = { vm.unarchive(r.id) }) { Text("복원", color = c.accent) }
                TextButton(onClick = { deleting = r.id }) { Text("삭제", color = c.danger) }
            }
        }
    }
    deleting?.let { id ->
        ConfirmDialog("루틴 삭제", "이 루틴과 기록을 삭제할까요?", "삭제", onDismiss = { deleting = null }) { vm.deleteRoutine(id); deleting = null }
    }
}

fun viewModeLabel(v: ViewMode) = when (v) {
    ViewMode.CARD -> "카드형"; ViewMode.GRID -> "2열 그리드"; ViewMode.COMPACT -> "컴팩트 리스트"; ViewMode.CHECK -> "체크 리스트"
}

