package app.myroutine.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import app.myroutine.core.DeepLinks
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.unit.dp
import app.myroutine.ui.home.HomeScreen
import app.myroutine.ui.templates.TemplatesScreen
import app.myroutine.ui.profile.ProfileScreen
import app.myroutine.ui.profile.SettingsSection
import app.myroutine.ui.profile.SettingsSectionScreen
import app.myroutine.ui.profile.TagManageScreen
import app.myroutine.ui.timeline.TimelineScreen
import app.myroutine.ui.detail.RoutineDetailScreen
import app.myroutine.ui.edit.RoutineEditScreen
import app.myroutine.ui.theme.AppTheme

object Routes {
    const val EDIT = "routine/edit?id={id}&template={template}"
    const val TAGS = "tags"
    const val DETAIL = "routine/detail/{id}"
    fun detail(id: String) = "routine/detail/$id"
    fun edit(id: String? = null, template: String? = null): String =
        "routine/edit?id=${id.orEmpty()}&template=${template.orEmpty()}"
}

enum class TopTab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "홈", Icons.Rounded.Home),
    TEMPLATES("templates", "추천루틴", Icons.Rounded.AutoAwesome),
    TIMELINE("timeline", "타임라인", Icons.Rounded.ViewTimeline),
    PROFILE("profile", "내정보", Icons.Rounded.Person),
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination
    val showBar = TopTab.entries.any { tab -> current?.hierarchy?.any { it.route == tab.route } == true }
    val c = AppTheme.colors
    val pendingLink by DeepLinks.pending.collectAsState()
    LaunchedEffect(pendingLink) {
        val route = pendingLink ?: return@LaunchedEffect
        DeepLinks.consume()
        val tab = TopTab.entries.firstOrNull { it.route == route }
        if (tab != null) {
            nav.navigate(tab.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
        } else {
            nav.navigate(route)
        }
    }

    Scaffold(
        containerColor = c.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = c.surface) {
                    TopTab.entries.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = c.accent, selectedTextColor = c.accent,
                                unselectedIconColor = c.textTertiary, unselectedTextColor = c.textTertiary,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = TopTab.HOME.route, modifier = Modifier.padding(bottom = if (showBar) padding.calculateBottomPadding() else 0.dp)) {
            composable(TopTab.HOME.route) {
                HomeScreen(
                    onAddRoutine = { nav.navigate(Routes.edit()) },
                    onEditRoutine = { id -> nav.navigate(Routes.edit(id = id)) },
                )
            }
            composable(TopTab.TEMPLATES.route) {
                TemplatesScreen(onPick = { nav.navigate(Routes.edit(template = it)) }, onCustom = { nav.navigate(Routes.edit()) })
            }
            composable(TopTab.TIMELINE.route) {
                TimelineScreen(onOpenRoutine = { nav.navigate(Routes.detail(it)) }, onManageTags = { nav.navigate(Routes.TAGS) })
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                RoutineDetailScreen(onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.edit(id = it)) })
            }
            composable(TopTab.PROFILE.route) {
                ProfileScreen(onOpen = { nav.navigate("settings/${it.route}") })
            }
            composable("settings/{section}", arguments = listOf(navArgument("section") { type = NavType.StringType })) { entry ->
                val section = SettingsSection.entries.firstOrNull { it.route == entry.arguments?.getString("section") } ?: SettingsSection.NOTIFICATIONS
                SettingsSectionScreen(section, onBack = { nav.popBackStack() }, onManageTags = { nav.navigate(Routes.TAGS) })
            }
            composable(
                Routes.EDIT,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("template") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                RoutineEditScreen(onBack = { nav.popBackStack() }, onManageTags = { nav.navigate(Routes.TAGS) })
            }
            composable(Routes.TAGS) { TagManageScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name (구현 예정)", color = AppTheme.colors.textSecondary)
    }
}
