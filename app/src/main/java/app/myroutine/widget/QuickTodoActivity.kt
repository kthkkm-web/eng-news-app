package app.myroutine.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import app.myroutine.core.newId
import app.myroutine.di.AppEntryPoint
import app.myroutine.domain.model.Todo
import app.myroutine.domain.schedule.ScheduleResolver
import app.myroutine.ui.home.TodoEditDialog
import app.myroutine.ui.theme.MyRoutineTheme
import kotlinx.coroutines.launch

/** 위젯 "+" → 다이얼로그만 띄워 오늘 할 일 추가. */
class QuickTodoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyRoutineTheme {
                TodoEditDialog(
                    existing = null,
                    onDismiss = { finish() },
                    onSave = { title, time ->
                        lifecycleScope.launch {
                            val ep = AppEntryPoint.get(this@QuickTodoActivity)
                            val today = ScheduleResolver.logicalDate(ep.time().nowDateTime(), ep.settings().current().dayStartHour)
                            ep.todos().saveTodo(Todo(newId(), title, today, time, false, 999))
                            WidgetUpdater.updateAll(this@QuickTodoActivity)
                            finish()
                        }
                    },
                )
            }
        }
    }
}
