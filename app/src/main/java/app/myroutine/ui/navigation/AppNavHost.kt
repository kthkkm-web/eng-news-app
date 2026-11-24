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
import app.myroutine.ui.edit.RoutineEditScreen
import app.myroutine.ui.theme.AppTheme

object Routes {
    const val EDIT = "routine/edit?id={id}&template={template}"
    const val TAGS = "tags"
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
            composable(TopTab.TEMPLATES.route) { Placeholder(TopTab.TEMPLATES.label) }
            composable(TopTab.TIMELINE.route) { Placeholder(TopTab.TIMELINE.label) }
            composable(TopTab.PROFILE.route) { Placeholder(TopTab.PROFILE.label) }
            composable(
                Routes.EDIT,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("template") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                RoutineEditScreen(onBack = { nav.popBackStack() }, onManageTags = { nav.navigate(Routes.TAGS) })
            }
            composable(Routes.TAGS) { Placeholder("태그 관리") }
        }
    }
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name (구현 예정)", color = AppTheme.colors.textSecondary)
    }
}
